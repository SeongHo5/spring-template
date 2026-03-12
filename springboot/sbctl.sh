#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 공통 설정(앱 목록, 포트, 경로, 정책)
# shellcheck source=conf.sh
source "${BASE_DIR}/conf.sh"

# 기능 모듈(로깅, 유틸, 앱 제어, 배포, 인터랙티브 UI)
# shellcheck source=lib/logger.sh
source "${BASE_DIR}/lib/logger.sh"
# shellcheck source=lib/util.sh
source "${BASE_DIR}/lib/util.sh"
# shellcheck source=lib/app.sh
source "${BASE_DIR}/lib/app.sh"
# shellcheck source=lib/deploy.sh
source "${BASE_DIR}/lib/deploy.sh"
# shellcheck source=lib/interactive.sh
source "${BASE_DIR}/lib/interactive.sh"

usage() {
  cat <<EOF
사용법
  ${0}                           : 인터랙티브 모드
  ${0} status <app|all>          : 상태 확인
  ${0} start <app|all>           : 시작
  ${0} stop <app|all>            : 중지
  ${0} restart <app|all>         : 재기동
  ${0} zd-restart <app|all>      : 무중단 재기동
  ${0} logs <app>                : 로그 tail
  ${0} deploy <app> <jar> [zd|normal] : 배포 (기본: zd)
  ${0} rollback <app>            : 롤백(무중단)
  ${0} deploy-all <jar_dir>      : 전체 배포(디렉토리, DEPLOY_ORDER 순서)

예시
  ${0} status all
  ${0} zd-restart api
  ${0} deploy gateway /tmp/gateway.jar zd
  ${0} rollback api
  ${0} deploy-all /tmp/jars
EOF
}

for_each_app() {
  local fn="$1"
  local interactive="${2:-false}"
  for app in "${APPS[@]}"; do
    "$fn" "$app" "$interactive"
  done
}

# CLI 인자를 해석해서 각 기능 모듈로 전달합니다.
main() {
  if [[ $# -eq 0 ]]; then
    interactive_main
    return 0
  fi

  local cmd="${1:-}"
  local app="${2:-}"

  case "$cmd" in
    status)
      [[ "$app" == "all" ]] && for a in "${APPS[@]}"; do status_app "$a"; done || status_app "$app"
      ;;
    start)
      [[ "$app" == "all" ]] && for a in "${APPS[@]}"; do start_app "$a" "false"; done || start_app "$app" "false"
      ;;
    stop)
      [[ "$app" == "all" ]] && for a in "${APPS[@]}"; do stop_app "$a"; done || stop_app "$app"
      ;;
    restart)
      [[ "$app" == "all" ]] && for a in "${APPS[@]}"; do restart_app "$a" "false"; done || restart_app "$app" "false"
      ;;
    zd-restart)
      [[ "$app" == "all" ]] && for a in "${APPS[@]}"; do zd_restart_app "$a" "false"; done || zd_restart_app "$app" "false"
      ;;
    logs)
      logs_app "$app"
      ;;
    deploy)
      local jar="${3:-}"
      local mode="${4:-zd}"
      [[ -n "$jar" ]] || die "deploy 명령에는 jar 경로가 필요합니다."
      deploy_app "$app" "$jar" "$mode" "false"
      ;;
    rollback)
      rollback_app "$app" "false"
      ;;
    deploy-all)
      local jar_dir="${2:-}"
      [[ -n "$jar_dir" ]] || die "deploy-all 명령에는 jar_dir가 필요합니다."
      deploy_all_in_order "$jar_dir" "zd" "false"
      ;;
    help|-h|--help|"")
      usage
      ;;
    *)
      die "알 수 없는 명령입니다: ${cmd} (help를 확인해 주세요)"
      ;;
  esac
}

main "$@"
