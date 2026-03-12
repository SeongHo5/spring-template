#!/usr/bin/env bash
set -euo pipefail

# shellcheck source=lib/logger.sh
source "$(dirname "${BASH_SOURCE[0]}")/logger.sh"
# shellcheck source=lib/util.sh
source "$(dirname "${BASH_SOURCE[0]}")/util.sh"

require_jar_exists() {
  local app="$1"
  [[ -f "${CURRENT_JAR[$app]}" ]] || die "${app}의 current.jar를 찾을 수 없습니다: ${CURRENT_JAR[$app]}"
}

current_pid() {
  local app="$1"
  local pid
  pid="$(read_file_or_empty "$(pid_file "$app")")"
  if is_pid_running "$pid"; then
    echo "$pid"
    return 0
  fi
  return 1
}

# 앱 health endpoint가 응답할 때까지 대기
wait_until_healthy() {
  local app="$1" port="$2"
  local url
  url="$(health_url "$app" "$port")"
  local deadline=$((SECONDS + START_TIMEOUT_SEC))

  while [[ $SECONDS -lt $deadline ]]; do
    if command -v curl >/dev/null 2>&1; then
      if curl -fsS "$url" >/dev/null; then
        return 0
      fi
    else
      # curl 없으면 LISTEN만 확인
      if port_listening "$port"; then
        return 0
      fi
    fi
    sleep 1
  done
  return 1
}

start_on_port() {
  local app="$1" port="$2"
  local interactive="${3:-false}"

  ensure_dirs_for_app "$app"
  require_jar_exists "$app"
  ensure_port_free "$port" "$interactive"

  local url
  url="$(health_url "$app" "$port")"
  log_info "${app} 시작을 진행합니다. 포트: ${port}"
  log_info "${app} 헬스 체크 URL: ${url}"

  local java_opts="${JAVA_OPTS_COMMON} ${JAVA_OPTS_APP[$app]} -Xlog:gc*:file=$(gc_log "$app"):time,level,tags:filecount=5,filesize=20M"
  local app_opts="--spring.profiles.active=${SPRING_PROFILE[$app]} --server.port=${port}"

  nohup "$JAVA_BIN" $java_opts -jar "${CURRENT_JAR[$app]}" $app_opts \
    >> "$(stdout_log "$app")" 2>&1 &

  local pid="$!"
  echo "$pid" > "$(pid_file "$app")"
  log_info "${app} 프로세스를 실행했습니다: $(pid_label "$pid")"

  if wait_until_healthy "$app" "$port"; then
    log_info "${app} 정상 기동 완료"
  else
    log_error "${app}이(가) ${START_TIMEOUT_SEC}초 내 정상 상태가 되지 않았습니다."
    tail -n 160 "$(stdout_log "$app")" || true
    return 1
  fi
}

start_app() {
  local app="$1"
  local interactive="${2:-false}"
  if current_pid "$app" >/dev/null 2>&1; then
    log_info "${app}은(는) 이미 실행 중입니다."
    return 0
  fi

  local color port
  color="$(read_active_color "$app")"
  port="$(port_by_color "$app" "$color")"
  start_on_port "$app" "$port" "$interactive"
}

stop_app() {
  local app="$1"

  ensure_dirs_for_app "$app"

  local pid
  if ! pid="$(current_pid "$app" 2>/dev/null)"; then
    log_info "${app}은(는) 실행 중이 아닙니다."
    rm -f "$(pid_file "$app")" || true
    return 0
  fi

  log_info "${app} 종료 요청을 보냅니다: $(pid_label "$pid")"
  kill -TERM "$pid" 2>/dev/null || true

  local deadline=$((SECONDS + STOP_TIMEOUT_SEC))
  while [[ $SECONDS -lt $deadline ]]; do
    if ! is_pid_running "$pid"; then
      log_info "${app} 정상 종료 완료"
      rm -f "$(pid_file "$app")" || true
      return 0
    fi
    sleep 1
  done

  log_warn "${app}이(가) ${STOP_TIMEOUT_SEC}초 내 종료되지 않아 강제 종료를 시도합니다."
  kill -KILL "$pid" 2>/dev/null || true
  sleep 1

  is_pid_running "$pid" && die "${app} 강제 종료에 실패했습니다: $(pid_label "$pid")"
  rm -f "$(pid_file "$app")" || true
  log_info "${app} 강제 종료 완료"
}

restart_app() {
  local app="$1"
  local interactive="${2:-false}"
  stop_app "$app"
  start_app "$app" "$interactive"
}

# 앱별 실행 여부와 blue/green 포트 상태를 출력합니다.
status_app() {
  local app="$1"
  ensure_dirs_for_app "$app"

  local color active_port standby_port
  color="$(read_active_color "$app")"
  active_port="$(port_by_color "$app" "$color")"
  standby_port="$(port_by_color "$app" "$(inactive_color "$color")")"

  if pid="$(current_pid "$app" 2>/dev/null)"; then
    log_info "${app} 상태: 실행 중 ($(pid_label "$pid"))"
  else
    log_info "${app} 상태: 중지"
  fi

  log_info "${app} 무중단 설정: active_color=${color}, active_port=${active_port}, standby_port=${standby_port}"
  port_listening "$active_port" && log_info "${app} active_port ${active_port}: LISTEN" || log_warn "${app} active_port ${active_port}: 비활성"
  port_listening "$standby_port" && log_info "${app} standby_port ${standby_port}: LISTEN" || log_info "${app} standby_port ${standby_port}: 비활성"
}

logs_app() {
  local app="$1"
  ensure_dirs_for_app "$app"
  log_info "${app} 로그를 출력합니다. 종료는 Ctrl+C"
  tail -n 200 -f "$(stdout_log "$app")"
}
