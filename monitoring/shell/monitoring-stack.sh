#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MONITORING_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMMON_LIB="${SCRIPT_DIR}/lib/common.sh"

# shellcheck disable=SC1091
source "${COMMON_LIB}"

SMTP_HOST="smtp.mailplug.co.kr"
SMTP_PORT="465"
COMPOSE_FILE="${MONITORING_DIR}/docker-compose.yml"
PROMETHEUS_FILE="${MONITORING_DIR}/prometheus/prometheus.yml"
TARGETS_FILE="${MONITORING_DIR}/prometheus/targets/node-exporter.yml"

usage() {
  cat <<USAGE
사용법:
  $(basename "$0") up
  $(basename "$0") up-ui
  $(basename "$0") down
  $(basename "$0") restart
  $(basename "$0") status
  $(basename "$0") logs [service]
  $(basename "$0") validate
  $(basename "$0") reload-prometheus
  $(basename "$0") check-smtp
  $(basename "$0") show-targets
USAGE
}

need_compose_context() {
  [[ -f "${COMPOSE_FILE}" ]] || {
    print_error "docker-compose.yml 파일이 없습니다: ${COMPOSE_FILE}"
    return 1
  }
  need_cmd docker
}

stack_up() {
  local with_ui="$1"
  need_compose_context

  if [[ "${with_ui}" == "Y" ]]; then
    (cd "${MONITORING_DIR}" && docker_compose_cmd --profile ui up -d)
  else
    (cd "${MONITORING_DIR}" && docker_compose_cmd up -d)
  fi

  print_ok "모니터링 스택을 기동했습니다."
}

stack_down() {
  need_compose_context
  (cd "${MONITORING_DIR}" && docker_compose_cmd down)
  print_ok "모니터링 스택을 종료했습니다."
}

stack_restart() {
  stack_down
  stack_up "N"
}

stack_status() {
  need_compose_context
  (cd "${MONITORING_DIR}" && docker_compose_cmd ps)
}

stack_logs() {
  need_compose_context
  local service="${1:-}"

  if [[ -n "${service}" ]]; then
    (cd "${MONITORING_DIR}" && docker_compose_cmd logs -f --tail 300 "${service}")
  else
    (cd "${MONITORING_DIR}" && docker_compose_cmd logs -f --tail 300)
  fi
}

validate_stack() {
  need_compose_context
  [[ -f "${PROMETHEUS_FILE}" ]] || {
    print_error "Prometheus 설정 파일이 없습니다: ${PROMETHEUS_FILE}"
    return 1
  }
  [[ -f "${TARGETS_FILE}" ]] || {
    print_error "targets 파일이 없습니다: ${TARGETS_FILE}"
    return 1
  }

  (cd "${MONITORING_DIR}" && docker_compose_cmd config -q)
  print_ok "docker compose 구성 검증이 완료되었습니다."
}

reload_prometheus() {
  need_cmd curl

  if ! curl -fsS -X POST http://127.0.0.1:9090/-/reload >/dev/null; then
    print_error "Prometheus reload 요청에 실패했습니다. Prometheus(9090) 상태를 확인해주세요."
    return 1
  fi

  print_ok "Prometheus 설정 reload 요청을 완료했습니다."
}

check_smtp() {
  print_info "SMTP 아웃바운드 연결을 확인합니다. (${SMTP_HOST}:${SMTP_PORT})"
  if timeout 5 bash -c ">/dev/tcp/${SMTP_HOST}/${SMTP_PORT}" 2>/dev/null; then
    print_ok "SMTP 아웃바운드 연결이 가능합니다."
    return 0
  fi

  print_warn "SMTP 아웃바운드 연결이 불가능합니다."
  return 1
}

show_targets() {
  [[ -f "${TARGETS_FILE}" ]] || {
    print_error "targets 파일이 없습니다: ${TARGETS_FILE}"
    return 1
  }

  print_info "현재 node_exporter targets 파일"
  nl -ba "${TARGETS_FILE}"
}

main() {
  local command="${1:-help}"
  shift || true

  case "${command}" in
    up)
      stack_up "N"
      ;;
    up-ui)
      stack_up "Y"
      ;;
    down)
      stack_down
      ;;
    restart)
      stack_restart
      ;;
    status)
      stack_status
      ;;
    logs)
      stack_logs "${1:-}"
      ;;
    validate)
      validate_stack
      ;;
    reload-prometheus)
      reload_prometheus
      ;;
    check-smtp)
      check_smtp
      ;;
    show-targets)
      show_targets
      ;;
    help|-h|--help)
      usage
      ;;
    *)
      print_error "지원하지 않는 명령입니다: ${command}"
      usage
      return 1
      ;;
  esac
}

main "$@"
