#!/usr/bin/env bash
set -euo pipefail

# shellcheck source=lib/logger.sh
source "$(dirname "${BASH_SOURCE[0]}")/logger.sh"

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "필수 명령을 찾을 수 없습니다: $1"
}

# 앱별 런타임 디렉토리(로그/실행 정보/릴리즈 보관소)를 보장합니다.
ensure_dirs_for_app() {
  local app="$1"
  mkdir -p "${APP_HOME[$app]}/run" "${APP_HOME[$app]}/logs" "${RELEASES_DIR[$app]}"
  touch "${APP_HOME[$app]}/logs/stdout.log"
}

run_dir() { echo "${APP_HOME[$1]}/run"; }
log_dir() { echo "${APP_HOME[$1]}/logs"; }

pid_file() { echo "$(run_dir "$1")/$1.pid"; }
pid_file_color() { echo "$(run_dir "$1")/$1.$2.pid"; }  # $2 = blue|green
active_color_file() { echo "$(run_dir "$1")/$1.active_color"; }

stdout_log() { echo "$(log_dir "$1")/stdout.log"; }
gc_log() { echo "$(log_dir "$1")/gc.log"; }

is_pid_running() {
  local pid="${1:-}"
  [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null
}

read_file_or_empty() {
  local f="$1"
  [[ -f "$f" ]] && cat "$f" || true
}

read_active_color() {
  local app="$1"
  if [[ -f "$(active_color_file "$app")" ]]; then
    cat "$(active_color_file "$app")"
  else
    echo "blue"
  fi
}

write_active_color() {
  local app="$1" color="$2"
  echo "$color" > "$(active_color_file "$app")"
}

inactive_color() {
  [[ "$1" == "blue" ]] && echo "green" || echo "blue"
}

port_by_color() {
  local app="$1" color="$2"
  if [[ "$color" == "blue" ]]; then
    echo "${PORT_BLUE[$app]}"
  else
    echo "${PORT_GREEN[$app]}"
  fi
}

health_url() {
  local app="$1" port="$2"
  echo "http://127.0.0.1:${port}${HEALTH_PATH[$app]}"
}

port_listening() {
  local port="$1"
  ss -ltn 2>/dev/null | grep -q ":${port} "
}

# PID -> 프로세스 이름(comm) 조회. 조회 실패 시 unknown 반환.
process_name_by_pid() {
  local pid="${1:-}"
  local name=""
  [[ -n "$pid" ]] || { echo "unknown"; return 0; }

  if command -v ps >/dev/null 2>&1; then
    name="$(ps -p "$pid" -o comm= 2>/dev/null | awk '{$1=$1;print}')"
  fi

  [[ -n "$name" ]] && echo "$name" || echo "unknown"
}

# 로그/메시지에 쓰기 좋은 PID 표기(예: 1234(java)).
pid_label() {
  local pid="${1:-}"
  local name
  name="$(process_name_by_pid "$pid")"
  echo "${pid}(${name})"
}

# 여러 PID를 한 줄로 직렬화(예: 1234(java), 5678(node)).
pid_labels() {
  local pids=("$@")
  local labels=()
  local pid

  for pid in "${pids[@]}"; do
    [[ -n "$pid" ]] || continue
    labels+=("$(pid_label "$pid")")
  done

  local IFS=", "
  echo "${labels[*]}"
}

# 포트 점유 PID 목록
# - ss 기반으로 먼저 찾고, lsof가 있으면 보강
pids_using_port() {
  local port="$1"
  local pids=()

  # ss 출력에서 pid 추출 (예: users:(("java",pid=1234,fd=...)))
  while IFS= read -r line; do
    # pid=숫자 패턴만 뽑기
    local pid
    pid="$(echo "$line" | sed -n 's/.*pid=\([0-9]\+\).*/\1/p' | head -n 1)"
    [[ -n "$pid" ]] && pids+=("$pid")
  done < <(ss -ltnp 2>/dev/null | grep ":${port} " || true)

  # lsof가 있으면 더 정확하게
  if command -v lsof >/dev/null 2>&1; then
    while IFS= read -r pid; do
      [[ -n "$pid" ]] && pids+=("$pid")
    done < <(lsof -t -iTCP:"$port" -sTCP:LISTEN 2>/dev/null | sort -u || true)
  fi

  # unique
  printf "%s\n" "${pids[@]}" | awk 'NF' | sort -u
}

kill_pids_gracefully() {
  local pids=("$@")
  [[ ${#pids[@]} -eq 0 ]] && return 0

  log_warn "포트 점유 프로세스를 종료합니다: $(pid_labels "${pids[@]}")"
  for pid in "${pids[@]}"; do
    kill -TERM "$pid" 2>/dev/null || true
  done

  local deadline=$((SECONDS + STOP_TIMEOUT_SEC))
  while [[ $SECONDS -lt $deadline ]]; do
    local alive=0
    for pid in "${pids[@]}"; do
      if is_pid_running "$pid"; then alive=1; fi
    done
    [[ $alive -eq 0 ]] && return 0
    sleep 1
  done

  log_warn "종료 대기 시간이 초과되어 강제 종료를 시도합니다."
  for pid in "${pids[@]}"; do
    kill -KILL "$pid" 2>/dev/null || true
  done
}

# 포트 사용 중이면 정책에 따라 처리합니다.
# interactive="true"이면 ask 모드에서 사용자에게 질문합니다.
ensure_port_free() {
  local port="$1"
  local interactive="${2:-false}"

  local pids
  mapfile -t pids < <(pids_using_port "$port")

  if [[ ${#pids[@]} -eq 0 ]]; then
    return 0
  fi

  case "$PORT_TAKEOVER_MODE" in
    never)
      die "포트 ${port}가 이미 사용 중입니다: $(pid_labels "${pids[@]}")"
      ;;
    auto)
      kill_pids_gracefully "${pids[@]}"
      ;;
    ask|*)
      if [[ "$interactive" == "true" ]]; then
        log_warn "포트 ${port}가 이미 사용 중입니다: $(pid_labels "${pids[@]}")"
        read -r -p "점유 프로세스를 종료하고 계속할까요? (y/N): " yn
        if [[ "${yn,,}" == "y" ]]; then
          kill_pids_gracefully "${pids[@]}"
        else
          die "사용자 선택으로 중단합니다."
        fi
      else
        die "포트 ${port}가 이미 사용 중입니다. ask 모드는 대화형 실행에서만 자동 처리할 수 있습니다: $(pid_labels "${pids[@]}")"
      fi
      ;;
  esac
}
