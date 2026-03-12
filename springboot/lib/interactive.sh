#!/usr/bin/env bash
set -euo pipefail

# shellcheck source=lib/logger.sh
source "$(dirname "${BASH_SOURCE[0]}")/logger.sh"
# shellcheck source=lib/app.sh
source "$(dirname "${BASH_SOURCE[0]}")/app.sh"
# shellcheck source=lib/deploy.sh
source "$(dirname "${BASH_SOURCE[0]}")/deploy.sh"

pick() {
  local title="$1"; shift
  local -a items=("$@")

  while true; do
    echo
    log_info "$title"
    local i=1
    for it in "${items[@]}"; do
      echo "  ${i}) ${it}"
      i=$((i+1))
    done
    echo "  0) 뒤로가기"

    read -r -p "> " sel
    [[ "$sel" =~ ^[0-9]+$ ]] || { log_warn "숫자를 입력해 주세요."; continue; }

    if [[ "$sel" == "0" ]]; then
      return 1
    fi

    if (( sel >= 1 && sel <= ${#items[@]} )); then
      echo "${items[$((sel-1))]}"
      return 0
    fi

    log_warn "범위를 벗어난 입력입니다."
  done
}

interactive_main() {
  while true; do
    local target
    if ! target="$(pick "대상 앱을 선택해 주세요." "전체" "${APPS[@]}")"; then
      log_info "종료합니다."
      return 0
    fi

    local action
    if ! action="$(pick "작업을 선택해 주세요." \
      "상태 확인" "시작" "중지" "재기동" "무중단 재기동" "로그 보기" \
      "배포(무중단)" "배포(일반)" "롤백(무중단)" "전체 배포(디렉토리/무중단)" "돌아가기")"; then
      continue
    fi

    case "$action" in
      "상태 확인")
        if [[ "$target" == "전체" ]]; then
          for app in "${APPS[@]}"; do status_app "$app"; done
        else
          status_app "$target"
        fi
        ;;

      "시작")
        if [[ "$target" == "전체" ]]; then
          for app in "${APPS[@]}"; do start_app "$app" "true"; done
        else
          start_app "$target" "true"
        fi
        ;;

      "중지")
        if [[ "$target" == "전체" ]]; then
          for app in "${APPS[@]}"; do stop_app "$app"; done
        else
          stop_app "$target"
        fi
        ;;

      "재기동")
        if [[ "$target" == "전체" ]]; then
          for app in "${APPS[@]}"; do restart_app "$app" "true"; done
        else
          restart_app "$target" "true"
        fi
        ;;

      "무중단 재기동")
        if [[ "$target" == "전체" ]]; then
          for app in "${APPS[@]}"; do zd_restart_app "$app" "true"; done
        else
          zd_restart_app "$target" "true"
        fi
        ;;

      "로그 보기")
        if [[ "$target" == "전체" ]]; then
          log_warn "전체 로그 tail은 지원하지 않습니다. 앱 하나를 선택해 주세요."
        else
          logs_app "$target"
        fi
        ;;

      "배포(무중단)")
        if [[ "$target" == "전체" ]]; then
          log_warn "전체 배포는 '전체 배포' 메뉴를 사용해 주세요."
        else
          read -r -p "배포할 jar 경로를 입력해 주세요: " jar
          deploy_app "$target" "$jar" "zd" "true"
        fi
        ;;

      "배포(일반)")
        if [[ "$target" == "전체" ]]; then
          log_warn "전체 배포는 '전체 배포' 메뉴를 사용해 주세요."
        else
          read -r -p "배포할 jar 경로를 입력해 주세요: " jar
          deploy_app "$target" "$jar" "normal" "true"
        fi
        ;;

      "롤백(무중단)")
        if [[ "$target" == "전체" ]]; then
          log_warn "전체 롤백은 사고 위험이 커서 막아 두었습니다. 앱 하나를 선택해 주세요."
        else
          rollback_app "$target" "true"
        fi
        ;;

      "전체 배포(디렉토리/무중단)")
        if [[ "$target" != "전체" ]]; then
          log_warn "이 메뉴는 전체를 선택했을 때만 사용합니다."
        else
          read -r -p "jar 디렉토리를 입력해 주세요(예: /tmp/jars): " jar_dir
          deploy_all_in_order "$jar_dir" "zd" "true"
        fi
        ;;

      "돌아가기")
        continue
        ;;
    esac
  done
}
