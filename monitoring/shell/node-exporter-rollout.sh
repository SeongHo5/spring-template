#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMMON_LIB="${SCRIPT_DIR}/lib/common.sh"
NODE_INSTALL_SCRIPT="${SCRIPT_DIR}/node-exporter-install.sh"
DEFAULT_TARGETS_FILE="${SCRIPT_DIR}/../prometheus/targets/node-exporter.yml"
DEFAULT_STACK_SCRIPT="${SCRIPT_DIR}/monitoring-stack.sh"

# shellcheck disable=SC1091
source "${COMMON_LIB}"

DEFAULT_VERSION="v1.10.2"
DEFAULT_PORT="19100"
DEFAULT_EXTRA_ARGS="--collector.systemd --collector.processes"

usage() {
  cat <<USAGE
사용법:
  $(basename "$0") --inventory <csv> [옵션]

옵션:
  --inventory <path>     inventory CSV 파일 (필수)
  --version <vX.Y.Z>     node_exporter 버전 (기본값: ${DEFAULT_VERSION})
  --port <port>          기본 포트 (기본값: ${DEFAULT_PORT})
  --extra-args "..."     기본 추가 인자
  --ssh-key <path>       SSH private key 파일
  --targets-file <path>  Prometheus file_sd 파일 경로
  --stack-script <path>  monitoring-stack.sh 경로
  --dry-run              실제 배포/파일 변경 없이 계획만 출력
  --skip-rollout         원격 설치는 생략하고 targets 파일만 동기화
  --skip-reload          targets 반영 후 Prometheus reload 생략

CSV 형식(헤더 포함 권장):
  host,user,ssh_port,target_host,target_port,extra_args,enabled
USAGE
}

declare -a PLAN_RECORDS=()
declare -a PLAN_TARGETS=()
declare -a ROLLOUT_TARGETS=()

add_target() {
  local value="$1"
  PLAN_TARGETS+=("${value}")
}

add_rollout_target() {
  local value="$1"
  ROLLOUT_TARGETS+=("${value}")
}

load_inventory() {
  local inventory="$1"
  local default_port="$2"
  local default_extra_args="$3"

  [[ -f "${inventory}" ]] || {
    print_error "inventory 파일이 없습니다: ${inventory}"
    return 1
  }

  local line_no=0
  while IFS= read -r raw || [[ -n "${raw}" ]]; do
    line_no=$((line_no + 1))

    local line
    line="${raw%%$'\r'}"
    line="$(trim "${line}")"

    [[ -n "${line}" ]] || continue
    [[ "${line:0:1}" == "#" ]] && continue

    IFS=',' read -r host user ssh_port target_host target_port extra_args enabled _ <<< "${line}"

    host="$(trim "${host:-}")"
    user="$(trim "${user:-}")"
    ssh_port="$(trim "${ssh_port:-}")"
    target_host="$(trim "${target_host:-}")"
    target_port="$(trim "${target_port:-}")"
    extra_args="$(trim "${extra_args:-}")"
    enabled="$(trim "${enabled:-}")"

    local host_lower
    host_lower="$(printf '%s' "${host}" | tr '[:upper:]' '[:lower:]')"
    if [[ "${host_lower}" == "host" ]]; then
      continue
    fi

    if [[ -z "${host}" ]]; then
      print_warn "${line_no}번째 줄: host가 비어 있어 건너뜁니다."
      continue
    fi

    local enabled_lower
    enabled_lower="$(printf '%s' "${enabled}" | tr '[:upper:]' '[:lower:]')"
    case "${enabled_lower}" in
      n|no|false|0)
        print_info "${host}: enabled=false로 건너뜁니다."
        continue
        ;;
    esac

    [[ -n "${user}" ]] || user="$(id -un)"
    [[ -n "${ssh_port}" ]] || ssh_port="22"
    [[ -n "${target_host}" ]] || target_host="${host}"
    [[ -n "${target_port}" ]] || target_port="${default_port}"
    [[ -n "${extra_args}" ]] || extra_args="${default_extra_args}"

    PLAN_RECORDS+=("${host}|${user}|${ssh_port}|${target_host}|${target_port}|${extra_args}")
    add_target "${target_host}:${target_port}"
  done < "${inventory}"

  if [[ ${#PLAN_RECORDS[@]} -eq 0 ]]; then
    print_warn "유효한 대상 서버가 없습니다."
  fi
}

print_plan() {
  local version="$1"
  local dry_run="$2"
  local skip_rollout="$3"

  echo ""
  print_info "rollout 계획"
  print_info "  - 대상 서버 수: ${#PLAN_RECORDS[@]}"
  print_info "  - node_exporter 버전: ${version}"
  print_info "  - dry-run: ${dry_run}"
  print_info "  - skip-rollout: ${skip_rollout}"

  local rec
  for rec in "${PLAN_RECORDS[@]}"; do
    IFS='|' read -r host user ssh_port target_host target_port extra_args <<< "${rec}"
    print_info "  - ${user}@${host}:${ssh_port} -> target ${target_host}:${target_port}"
    print_info "    extra_args: ${extra_args}"
  done
}

run_rollout() {
  local version="$1"
  local ssh_key="$2"
  local dry_run="$3"

  need_cmd ssh
  [[ -f "${NODE_INSTALL_SCRIPT}" ]] || {
    print_error "node-exporter-install.sh를 찾을 수 없습니다: ${NODE_INSTALL_SCRIPT}"
    return 1
  }

  local failed=0
  local rec
  for rec in "${PLAN_RECORDS[@]}"; do
    IFS='|' read -r host user ssh_port target_host target_port extra_args <<< "${rec}"

    local ssh_target="${user}@${host}"
    local ssh_opts=(-p "${ssh_port}")
    if [[ -n "${ssh_key}" ]]; then
      ssh_opts+=( -i "${ssh_key}" )
    fi

    local version_q port_q extra_q
    version_q="$(printf '%q' "${version}")"
    port_q="$(printf '%q' "${target_port}")"
    extra_q="$(printf '%q' "${extra_args}")"

    local remote_cmd="bash -s -- --version ${version_q} --port ${port_q} --extra-args ${extra_q}"

    if [[ "${dry_run}" == "Y" ]]; then
      print_info "[DRY-RUN] ${ssh_target} => ${remote_cmd}"
      add_rollout_target "${target_host}:${target_port}"
      continue
    fi

    print_info "배포 중: ${ssh_target}"
    if ssh "${ssh_opts[@]}" "${ssh_target}" "${remote_cmd}" < "${NODE_INSTALL_SCRIPT}"; then
      print_ok "배포 성공: ${ssh_target}"
      add_rollout_target "${target_host}:${target_port}"
    else
      print_error "배포 실패: ${ssh_target}"
      failed=$((failed + 1))
    fi
  done

  if [[ "${dry_run}" == "Y" ]]; then
    return 0
  fi

  if [[ ${failed} -gt 0 ]]; then
    print_warn "실패한 서버가 ${failed}개 있습니다. 성공한 서버만 targets에 반영합니다."
  fi
}

sync_targets_file() {
  local targets_file="$1"
  local default_port="$2"
  local dry_run="$3"
  local skip_rollout="$4"

  local -a source_targets=()
  if [[ "${skip_rollout}" == "Y" || "${dry_run}" == "Y" ]]; then
    source_targets=("${PLAN_TARGETS[@]}")
  else
    source_targets=("${ROLLOUT_TARGETS[@]}")
  fi

  local tmp
  tmp="$(mktemp)"

  {
    echo "- targets:"
    if [[ ${#source_targets[@]} -eq 0 ]]; then
      echo "    - 127.0.0.1:${default_port}"
    else
      printf '%s\n' "${source_targets[@]}" | awk 'NF && !seen[$0]++ {print "    - " $0}'
    fi
    echo "  labels:"
    echo "    job: node-exporter"
  } > "${tmp}"

  if [[ "${dry_run}" == "Y" ]]; then
    print_info "[DRY-RUN] 생성될 targets 파일: ${targets_file}"
    cat "${tmp}"
    rm -f "${tmp}"
    return 0
  fi

  mkdir -p "$(dirname "${targets_file}")"
  install -m 0644 "${tmp}" "${targets_file}"
  rm -f "${tmp}"

  print_ok "targets 파일 동기화 완료: ${targets_file}"
}

reload_prometheus() {
  local stack_script="$1"
  local dry_run="$2"
  local skip_reload="$3"

  if [[ "${skip_reload}" == "Y" ]]; then
    print_info "--skip-reload 옵션으로 Prometheus reload를 생략합니다."
    return 0
  fi

  if [[ "${dry_run}" == "Y" ]]; then
    print_info "[DRY-RUN] Prometheus reload 실행 예정: ${stack_script} reload-prometheus"
    return 0
  fi

  if [[ ! -x "${stack_script}" ]]; then
    print_warn "stack script가 없거나 실행 권한이 없습니다: ${stack_script}"
    return 0
  fi

  if "${stack_script}" reload-prometheus; then
    print_ok "Prometheus reload 완료"
  else
    print_warn "Prometheus reload에 실패했습니다. 수동으로 확인해주세요."
  fi
}

main() {
  local inventory=""
  local version="${DEFAULT_VERSION}"
  local port="${DEFAULT_PORT}"
  local extra_args="${DEFAULT_EXTRA_ARGS}"
  local ssh_key=""
  local targets_file="${DEFAULT_TARGETS_FILE}"
  local stack_script="${DEFAULT_STACK_SCRIPT}"
  local dry_run="N"
  local skip_rollout="N"
  local skip_reload="N"

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --inventory)
        inventory="$2"
        shift 2
        ;;
      --version)
        version="$2"
        shift 2
        ;;
      --port)
        port="$2"
        shift 2
        ;;
      --extra-args)
        extra_args="$2"
        shift 2
        ;;
      --ssh-key)
        ssh_key="$2"
        shift 2
        ;;
      --targets-file)
        targets_file="$2"
        shift 2
        ;;
      --stack-script)
        stack_script="$2"
        shift 2
        ;;
      --dry-run)
        dry_run="Y"
        shift
        ;;
      --skip-rollout)
        skip_rollout="Y"
        shift
        ;;
      --skip-reload)
        skip_reload="Y"
        shift
        ;;
      -h|--help)
        usage
        return 0
        ;;
      *)
        print_error "알 수 없는 옵션입니다: $1"
        usage
        return 1
        ;;
    esac
  done

  [[ -n "${inventory}" ]] || {
    print_error "--inventory는 필수입니다."
    usage
    return 1
  }

  [[ "${version}" == v* ]] || version="v${version}"

  if [[ -n "${ssh_key}" && ! -f "${ssh_key}" ]]; then
    print_error "ssh key 파일이 없습니다: ${ssh_key}"
    return 1
  fi

  load_inventory "${inventory}" "${port}" "${extra_args}"
  print_plan "${version}" "${dry_run}" "${skip_rollout}"

  if [[ "${skip_rollout}" != "Y" ]]; then
    run_rollout "${version}" "${ssh_key}" "${dry_run}"
  else
    print_info "--skip-rollout 옵션으로 원격 설치를 생략합니다."
  fi

  sync_targets_file "${targets_file}" "${port}" "${dry_run}" "${skip_rollout}"
  reload_prometheus "${stack_script}" "${dry_run}" "${skip_reload}"

  print_ok "node_exporter 롤아웃 작업이 완료되었습니다."
}

main "$@"
