#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMMON_LIB="${SCRIPT_DIR}/lib/common.sh"
STACK_SCRIPT="${SCRIPT_DIR}/monitoring-stack.sh"
DOCKER_BUNDLE_SCRIPT="${SCRIPT_DIR}/docker-bundle.sh"
NODE_INSTALL_SCRIPT="${SCRIPT_DIR}/node-exporter-install.sh"

# shellcheck disable=SC1091
source "${COMMON_LIB}"

DEFAULT_NODE_VERSION="v1.10.2"
DEFAULT_NODE_PORT="19100"
DEFAULT_EXTRA_ARGS="--collector.systemd --collector.processes"
DEFAULT_DOCKER_FAMILY="auto"
DEFAULT_INVENTORY="${SCRIPT_DIR}/inventory/node-exporter-hosts.csv"
SAMPLE_INVENTORY="${SCRIPT_DIR}/inventory/node-exporter-hosts.example.csv"

need_script() {
  local path="$1"
  [[ -x "$path" ]] || {
    print_error "필수 스크립트가 없거나 실행 권한이 없습니다: ${path}"
    return 1
  }
}

ask() {
  local prompt="$1"
  local default="${2:-}"
  local answer=""

  if [[ -n "${default}" ]]; then
    read -r -p "${prompt} (기본값: ${default}) : " answer
    echo "${answer:-$default}"
  else
    read -r -p "${prompt} : " answer
    echo "${answer}"
  fi
}

ask_yn() {
  local prompt="$1"
  local default="${2:-Y}"
  local answer=""
  local normalized=""

  while true; do
    read -r -p "${prompt} (Y/N, 기본값: ${default}) : " answer
    answer="${answer:-$default}"
    normalized="$(printf '%s' "${answer}" | tr '[:lower:]' '[:upper:]')"
    case "${normalized}" in
      Y|YES)
        echo "Y"
        return 0
        ;;
      N|NO)
        echo "N"
        return 0
        ;;
      *)
        print_warn "Y 또는 N으로 입력해주세요."
        ;;
    esac
  done
}

pause() {
  echo ""
  read -r -p "계속하려면 Enter를 누르세요..." _
  echo ""
}

run_cmd() {
  local cmd=("$@")
  echo ""
  print_info "실행: ${cmd[*]}"
  if "${cmd[@]}"; then
    print_ok "작업 완료"
  else
    local rc=$?
    print_error "실패 (exit code: ${rc})"
    return "${rc}"
  fi
}

copy_inventory_sample_if_needed() {
  if [[ -f "${DEFAULT_INVENTORY}" ]]; then
    return 0
  fi

  if [[ -f "${SAMPLE_INVENTORY}" ]]; then
    cp "${SAMPLE_INVENTORY}" "${DEFAULT_INVENTORY}"
    print_warn "기본 inventory가 없어 샘플을 복사했습니다: ${DEFAULT_INVENTORY}"
  fi
}

stack_menu() {
  while true; do
    echo ""
    echo "=== Monitoring Stack 관리 ==="
    echo "1) Prometheus + Alertmanager 기동"
    echo "2) Prometheus + Alertmanager + Grafana 기동"
    echo "3) 스택 종료"
    echo "4) 스택 재시작"
    echo "5) 상태 확인"
    echo "6) 로그 보기"
    echo "7) 설정 검증"
    echo "8) Prometheus reload"
    echo "9) SMTP 연결 체크"
    echo "10) 현재 node-exporter targets 보기"
    echo "11) 이전 메뉴"
    echo ""

    local choice
    choice="$(ask "번호를 선택해주세요" "1")"

    case "${choice}" in
      1)
        run_cmd "${STACK_SCRIPT}" up || true
        pause
        ;;
      2)
        run_cmd "${STACK_SCRIPT}" up-ui || true
        pause
        ;;
      3)
        run_cmd "${STACK_SCRIPT}" down || true
        pause
        ;;
      4)
        run_cmd "${STACK_SCRIPT}" restart || true
        pause
        ;;
      5)
        run_cmd "${STACK_SCRIPT}" status || true
        pause
        ;;
      6)
        local service
        service="$(ask "서비스명 입력(prometheus/alertmanager/grafana, 비우면 전체)" "")"
        echo ""
        print_info "로그 종료는 Ctrl+C"
        if [[ -n "${service}" ]]; then
          "${STACK_SCRIPT}" logs "${service}" || true
        else
          "${STACK_SCRIPT}" logs || true
        fi
        pause
        ;;
      7)
        run_cmd "${STACK_SCRIPT}" validate || true
        pause
        ;;
      8)
        run_cmd "${STACK_SCRIPT}" reload-prometheus || true
        pause
        ;;
      9)
        run_cmd "${STACK_SCRIPT}" check-smtp || true
        pause
        ;;
      10)
        run_cmd "${STACK_SCRIPT}" show-targets || true
        pause
        ;;
      11)
        return 0
        ;;
      *)
        print_warn "잘못된 선택입니다."
        ;;
    esac
  done
}

docker_bundle_menu() {
  while true; do
    echo ""
    echo "=== Docker/번들 관리 ==="
    echo "1) Docker 설치"
    echo "2) Docker 파일 다운로드 (폐쇄망에 넣을 파일을 온라인에서 미리 다운로드 받기)"
    echo "3) 모니터링 이미지 번들 준비"
    echo "4) 통합 번들 준비(Docker 패키지 + 모니터링 이미지)"
    echo "5) 번들 설치(폐쇄망)"
    echo "6) 이전 메뉴"
    echo ""

    local choice
    choice="$(ask "번호를 선택해주세요" "1")"

    case "${choice}" in
      1)
        local family
        family="$(ask "패키지 계열(auto/rpm/deb)" "${DEFAULT_DOCKER_FAMILY}")"
        run_cmd "${DOCKER_BUNDLE_SCRIPT}" install-docker-online --family "${family}" || true
        pause
        ;;
      2)
        local outdir family2
        outdir="$(ask "번들 저장 경로" "$(pwd)/bundle-$(date +%Y%m%d-%H%M%S)")"
        family2="$(ask "패키지 계열(auto/rpm/deb)" "${DEFAULT_DOCKER_FAMILY}")"
        run_cmd "${DOCKER_BUNDLE_SCRIPT}" prepare-pkgs --outdir "${outdir}" --family "${family2}" || true
        pause
        ;;
      3)
        local outdir3 include_grafana3 prom_tag3 am_tag3 graf_tag3
        outdir3="$(ask "이미지 번들 저장 경로" "$(pwd)/bundle-$(date +%Y%m%d-%H%M%S)")"
        include_grafana3="$(ask_yn "Grafana 이미지를 포함할까요?" "Y")"
        prom_tag3="$(ask "Prometheus 태그(버전)" "v3.10.0")"
        am_tag3="$(ask "Alertmanager 태그(버전)" "v0.31.0")"

        local cmd3=("${DOCKER_BUNDLE_SCRIPT}" prepare-images --outdir "${outdir3}" --prometheus-tag "${prom_tag3}" --alertmanager-tag "${am_tag3}")
        if [[ "${include_grafana3}" == "Y" ]]; then
          graf_tag3="$(ask "Grafana 태그(버전)" "12.3.4")"
          cmd3+=(--include-grafana --grafana-tag "${graf_tag3}")
        fi

        run_cmd "${cmd3[@]}" || true
        pause
        ;;
      4)
        local outdir4 family4 include_grafana4 include_pkgs4 include_images4 prom_tag4 am_tag4 graf_tag4
        outdir4="$(ask "통합 번들 저장 경로" "$(pwd)/bundle-$(date +%Y%m%d-%H%M%S)")"
        family4="$(ask "패키지 계열(auto/rpm/deb)" "${DEFAULT_DOCKER_FAMILY}")"
        include_grafana4="$(ask_yn "Grafana 이미지를 포함할까요?" "Y")"
        include_pkgs4="$(ask_yn "Docker 설치를 위한 패키지를 포함할까요?" "Y")"
        include_images4="$(ask_yn "모니터링 스택(Prometheus, Alertmanager) 이미지를 포함할까요?" "Y")"
        prom_tag4="$(ask "Prometheus 태그(버전)" "v3.10.0")"
        am_tag4="$(ask "Alertmanager 태그(버전)" "v0.31.0")"

        local cmd4=("${DOCKER_BUNDLE_SCRIPT}" prepare-bundle --outdir "${outdir4}" --family "${family4}" --prometheus-tag "${prom_tag4}" --alertmanager-tag "${am_tag4}")

        if [[ "${include_grafana4}" == "Y" ]]; then
          graf_tag4="$(ask "Grafana 태그(버전)" "12.3.4")"
          cmd4+=(--include-grafana --grafana-tag "${graf_tag4}")
        fi
        if [[ "${include_pkgs4}" != "Y" ]]; then
          cmd4+=(--skip-pkgs)
        fi
        if [[ "${include_images4}" != "Y" ]]; then
          cmd4+=(--skip-images)
        fi

        run_cmd "${cmd4[@]}" || true
        pause
        ;;
      5)
        local bundle5 with_ui5 install_pkgs5 install_images5
        bundle5="$(ask "반입한 번들 경로를 입력해주세요." "$(pwd)")"
        with_ui5="$(ask_yn "Grafana(UI)도 함께 기동할까요?" "N")"
        install_pkgs5="$(ask_yn "번들의 Docker 패키지를 설치할까요?" "Y")"
        install_images5="$(ask_yn "번들의 Docker 이미지를 로드할까요?" "Y")"

        local cmd5=("${DOCKER_BUNDLE_SCRIPT}" install-bundle --bundle "${bundle5}")
        if [[ "${with_ui5}" == "Y" ]]; then
          cmd5+=(--with-ui)
        fi
        if [[ "${install_pkgs5}" != "Y" ]]; then
          cmd5+=(--skip-pkgs)
        fi
        if [[ "${install_images5}" != "Y" ]]; then
          cmd5+=(--skip-images)
        fi

        run_cmd "${cmd5[@]}" || true
        pause
        ;;
      6)
        return 0
        ;;
      *)
        print_warn "잘못된 선택입니다."
        ;;
    esac
  done
}

node_single_menu() {
  while true; do
    echo ""
    echo "=== Node Exporter(단일 서버) ==="
    echo "1) 현재 서버에 설치/업데이트 (온라인 다운로드)"
    echo "2) tar.gz 파일로 설치/업데이트 (오프라인)"
    echo "3) tar.gz 다운로드 (설치 없이)"
    echo "4) 이전 메뉴"
    echo ""

    local choice
    choice="$(ask "번호를 선택해주세요" "1")"

    case "${choice}" in
      1)
        local version1 port1 extra_args1
        version1="$(ask "node_exporter 버전" "${DEFAULT_NODE_VERSION}")"
        port1="$(ask "포트" "${DEFAULT_NODE_PORT}")"
        extra_args1="$(ask "추가 실행 인자" "${DEFAULT_EXTRA_ARGS}")"
        run_cmd "${NODE_INSTALL_SCRIPT}" --version "${version1}" --port "${port1}" --extra-args "${extra_args1}" || true
        pause
        ;;
      2)
        local version2 port2 tarball2 extra_args2
        version2="$(ask "node_exporter 버전" "${DEFAULT_NODE_VERSION}")"
        port2="$(ask "포트" "${DEFAULT_NODE_PORT}")"
        tarball2="$(ask "node_exporter tar.gz 파일 경로" "")"
        extra_args2="$(ask "추가 실행 인자" "${DEFAULT_EXTRA_ARGS}")"

        if [[ -z "${tarball2}" ]]; then
          print_error "tar.gz 경로는 필수입니다."
          pause
          continue
        fi

        run_cmd "${NODE_INSTALL_SCRIPT}" --version "${version2}" --port "${port2}" --tarball "${tarball2}" --extra-args "${extra_args2}" || true
        pause
        ;;
      3)
        local version3 outdir3
        version3="$(ask "node_exporter 버전" "${DEFAULT_NODE_VERSION}")"
        outdir3="$(ask "다운로드 저장 경로" "$(pwd)")"
        run_cmd "${NODE_INSTALL_SCRIPT}" --version "${version3}" --outdir "${outdir3}" --download-only || true
        pause
        ;;
      4)
        return 0
        ;;
      *)
        print_warn "잘못된 선택입니다."
        ;;
    esac
  done
}

main_menu() {
  while true; do
    echo ""
    echo "============================================================"
    echo "Monitoring Infra Tool"
    echo "============================================================"
    echo "1) Monitoring Stack 관리 (Prometheus/Alertmanager/Grafana)"
    echo "2) Docker/오프라인 번들 관리"
    echo "3) Node Exporter 단일 서버 설치/업데이트"
    echo "4) 종료"
    echo ""

    local choice
    choice="$(ask "번호를 선택해주세요" "1")"

    case "${choice}" in
      1)
        stack_menu
        ;;
      2)
        docker_bundle_menu
        ;;
      3)
        node_single_menu
        ;;
      4)
        print_info "Bye."
        exit 0
        ;;
      *)
        print_warn "잘못된 선택입니다."
        ;;
    esac
  done
}

main() {
  need_script "${STACK_SCRIPT}"
  need_script "${DOCKER_BUNDLE_SCRIPT}"
  need_script "${NODE_INSTALL_SCRIPT}"

  main_menu
}

main "$@"
