#!/usr/bin/env bash
set -euo pipefail

# shellcheck source=lib/logger.sh
source "$(dirname "${BASH_SOURCE[0]}")/logger.sh"
# shellcheck source=lib/util.sh
source "$(dirname "${BASH_SOURCE[0]}")/util.sh"
# shellcheck source=lib/app.sh
source "$(dirname "${BASH_SOURCE[0]}")/app.sh"

nginx_upstream_file() {
  local app="$1"
  echo "${NGINX_UPSTREAM_DIR}/${app}.upstream.conf"
}

switch_nginx_upstream() {
  local app="$1" port="$2"
  if [[ ! -d "$NGINX_UPSTREAM_DIR" ]]; then
    log_warn "Nginx upstream 디렉토리를 찾을 수 없습니다. (${NGINX_UPSTREAM_DIR}) 전환 없이 진행합니다."
    return 0
  fi

  local upstream_name="${app}${UPSTREAM_NAME_SUFFIX}"
  local file
  file="$(nginx_upstream_file "$app")"
  mkdir -p "$NGINX_UPSTREAM_DIR"

  cat > "$file" <<EOF
upstream ${upstream_name} {
    server 127.0.0.1:${port};
    keepalive 64;
}
EOF

  log_info "nginx upstream 파일을 갱신합니다. (${file}) 포트: ${port}"

  if command -v systemctl >/dev/null 2>&1; then
    if $NGINX_RELOAD_CMD; then
      log_info "nginx reload 완료"
    else
      die "nginx reload가 실패했습니다. 설정을 확인해 주세요."
    fi
  else
    log_warn "systemctl이 없어서 nginx reload를 실행하지 않습니다."
  fi
}

# 무중단 재기동(blue/green)
# 1) standby 인스턴스 기동/검증 -> 2) 트래픽 전환 -> 3) 기존 active 종료
zd_restart_app() {
  local app="$1"
  local interactive="${2:-false}"

  ensure_dirs_for_app "$app"
  [[ -f "${CURRENT_JAR[$app]}" ]] || die "${app} current.jar를 찾을 수 없습니다."

  local active_color active_port standby_color standby_port
  active_color="$(read_active_color "$app")"
  active_port="$(port_by_color "$app" "$active_color")"
  standby_color="$(inactive_color "$active_color")"
  standby_port="$(port_by_color "$app" "$standby_color")"

  log_info "${app} 무중단 재기동 시작: active_color=${active_color}, active_port=${active_port}"
  log_info "standby 인스턴스 기동 대상: color=${standby_color}, port=${standby_port}"

  # 포트별 pid 파일
  local pid_active pid_standby
  local pidfile_active pidfile_standby
  pidfile_active="$(pid_file_color "$app" "$active_color")"
  pidfile_standby="$(pid_file_color "$app" "$standby_color")"

  # 현재 pid_file -> active pidfile로 보강
  if [[ -f "$(pid_file "$app")" ]] && [[ ! -f "$pidfile_active" ]]; then
    cp -f "$(pid_file "$app")" "$pidfile_active" || true
  fi

  # standby 포트가 점유 중이면 정책에 따라 정리
  ensure_port_free "$standby_port" "$interactive"

  # standby 기동 (pid는 standby pidfile에 기록)
  local url
  url="$(health_url "$app" "$standby_port")"
  log_info "${app} standby 인스턴스를 시작합니다. HealthCheck URL: ${url}"

  local java_opts="${JAVA_OPTS_COMMON} ${JAVA_OPTS_APP[$app]} -Xlog:gc*:file=$(gc_log "$app"):time,level,tags:filecount=5,filesize=20M"
  local app_opts="--spring.profiles.active=${SPRING_PROFILE[$app]} --server.port=${standby_port}"

  nohup "$JAVA_BIN" $java_opts -jar "${CURRENT_JAR[$app]}" $app_opts \
    >> "$(stdout_log "$app")" 2>&1 &

  pid_standby="$!"
  echo "$pid_standby" > "$pidfile_standby"
  log_info "${app} standby 실행 PID: $(pid_label "$pid_standby")"

  if ! wait_until_healthy "$app" "$standby_port"; then
    log_error "${app} standby가 정상 상태가 아니어서 전환을 중단합니다."
    tail -n 180 "$(stdout_log "$app")" || true
    die "무중단 재기동을 중단합니다."
  fi

  log_info "${app} standby 정상 확인 완료. 트래픽 전환을 진행합니다."
  switch_nginx_upstream "$app" "$standby_port"

  write_active_color "$app" "$standby_color"
  log_info "${app} active_color 변경: ${standby_color}"

  # 기존(active) 종료
  pid_active="$(read_file_or_empty "$pidfile_active")"
  if is_pid_running "$pid_active"; then
    log_info "${app} 기존 인스턴스를 종료합니다: $(pid_label "$pid_active")"
    kill -TERM "$pid_active" 2>/dev/null || true

    local deadline=$((SECONDS + STOP_TIMEOUT_SEC))
    while [[ $SECONDS -lt $deadline ]]; do
      if ! is_pid_running "$pid_active"; then
        log_info "${app} 기존 인스턴스 종료 완료"
        rm -f "$pidfile_active" || true
        break
      fi
      sleep 1
    done

    if is_pid_running "$pid_active"; then
      log_warn "기존 인스턴스 종료 대기 시간이 초과되어 강제 종료를 시도합니다."
      kill -KILL "$pid_active" 2>/dev/null || true
      sleep 1
      rm -f "$pidfile_active" || true
    fi
  else
    rm -f "$pidfile_active" || true
    log_info "기존 인스턴스 PID가 유효하지 않아 종료 단계를 생략합니다."
  fi

  # 편의: 대표 pid_file은 active 인스턴스로 맞춥니다.
  echo "$pid_standby" > "$(pid_file "$app")"

  log_info "${app} 무중단 재기동 완료"
}

# 배포: jar 복사 -> previous.jar 갱신 -> current.jar 갱신 -> (무중단/일반) 재기동
deploy_app() {
  local app="$1" jar="$2" mode="${3:-zd}" interactive="${4:-false}"
  [[ -f "$jar" ]] || die "배포할 jar를 찾을 수 없습니다: ${jar}"

  ensure_dirs_for_app "$app"
  mkdir -p "${RELEASES_DIR[$app]}"

  local ts release_target
  ts="$(date "+%Y%m%d_%H%M%S")"
  release_target="${RELEASES_DIR[$app]}/$(basename "$jar" .jar)-${ts}.jar"

  log_info "${app} jar 배포 파일 준비: ${jar} -> ${release_target}"
  cp -f "$jar" "$release_target"

  # current이 가리키던 실제 파일을 previous로 보관
  if [[ -L "${CURRENT_JAR[$app]}" ]]; then
    local current_real
    current_real="$(readlink -f "${CURRENT_JAR[$app]}")"
    if [[ -n "$current_real" && -f "$current_real" ]]; then
      ln -sfn "$current_real" "${PREVIOUS_JAR[$app]}"
      log_info "${app} previous.jar 갱신: ${current_real}"
    fi
  fi

  ln -sfn "$release_target" "${CURRENT_JAR[$app]}"
  log_info "${app} current.jar 갱신: ${release_target}"

  if [[ "$mode" == "zd" ]]; then
    zd_restart_app "$app" "$interactive"
  else
    restart_app "$app" "$interactive"
  fi
}

# 롤백: previous.jar -> current.jar 되돌림 -> 무중단 재기동
rollback_app() {
  local app="$1" interactive="${2:-false}"

  ensure_dirs_for_app "$app"
  [[ -L "${PREVIOUS_JAR[$app]}" ]] || die "${app} previous.jar가 없어 롤백할 수 없습니다."
  local prev_real
  prev_real="$(readlink -f "${PREVIOUS_JAR[$app]}")"
  [[ -f "$prev_real" ]] || die "${app} previous.jar 대상 파일이 없습니다. (${prev_real})"

  log_warn "${app} 롤백을 진행합니다. previous -> current 대상: ${prev_real}"
  ln -sfn "$prev_real" "${CURRENT_JAR[$app]}"

  zd_restart_app "$app" "$interactive"
  log_info "${app} 롤백 완료"
}

# 순서대로 여러 앱 무중단 배포
deploy_all_in_order() {
  local jar_dir="$1" mode="${2:-zd}" interactive="${3:-false}"
  [[ -d "$jar_dir" ]] || die "디렉토리를 찾을 수 없습니다: ${jar_dir}"

  log_info "전체 배포 시작. 순서: ${DEPLOY_ORDER[*]}"

  for app in "${DEPLOY_ORDER[@]}"; do
    local jar
    # 기본 규칙: <jar_dir>/<app>.jar 로 찾습니다. 필요하면 여기 규칙을 바꾸면 됩니다.
    jar="${jar_dir}/${app}.jar"
    [[ -f "$jar" ]] || die "배포 jar를 찾지 못했습니다. app=${app}, jar=${jar}"

    deploy_app "$app" "$jar" "$mode" "$interactive"
  done

  log_info "전체 배포 완료"
}
