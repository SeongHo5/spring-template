#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMMON_LIB="${SCRIPT_DIR}/lib/common.sh"

# shellcheck disable=SC1091
source "${COMMON_LIB}"

DEFAULT_VERSION="v1.10.2"
DEFAULT_PORT="19100"
DEFAULT_EXTRA_ARGS="--collector.systemd --collector.processes"

usage() {
  cat <<USAGE
사용법:
  $(basename "$0") [--version <vX.Y.Z>] [--port <port>] [--tarball <path>] [--extra-args "..."]

옵션:
  --version      node_exporter 버전 (기본값: ${DEFAULT_VERSION})
  --port         수집 포트 (기본값: ${DEFAULT_PORT})
  --tarball      로컬 tar.gz 파일 경로(오프라인 설치)
  --extra-args   추가 실행 인자
USAGE
}

arch_to_node_exporter() {
  local arch
  arch="$(uname -m)"
  case "${arch}" in
    x86_64|amd64)
      echo "amd64"
      ;;
    aarch64|arm64)
      echo "arm64"
      ;;
    armv7l|armv7)
      echo "armv7"
      ;;
    armv6l|armv6)
      echo "armv6"
      ;;
    *)
      echo "unsupported"
      ;;
  esac
}

download_tarball() {
  local version="$1"
  local outdir="$2"

  need_cmd curl

  local arch_name
  arch_name="$(arch_to_node_exporter)"
  [[ "${arch_name}" != "unsupported" ]] || {
    print_error "지원하지 않는 아키텍처입니다: $(uname -m)"
    return 1
  }

  local version_no_v="${version#v}"
  local filename="node_exporter-${version_no_v}.linux-${arch_name}.tar.gz"
  local url="https://github.com/prometheus/node_exporter/releases/download/v${version_no_v}/${filename}"

  print_info "node_exporter 다운로드: ${url}" >&2
  curl -fL "${url}" -o "${outdir}/${filename}"
  printf '%s\n' "${outdir}/${filename}"
}

ensure_node_exporter_user() {
  if id node_exporter >/dev/null 2>&1; then
    return 0
  fi

  print_info "node_exporter 시스템 계정을 생성합니다."
  run_as_root useradd --system --no-create-home --shell /usr/sbin/nologin node_exporter || \
    run_as_root useradd --system --no-create-home --shell /sbin/nologin node_exporter
}

install_binary() {
  local tarball="$1"

  local tmpdir
  tmpdir="$(mktemp -d)"

  tar -xzf "${tarball}" -C "${tmpdir}"

  local bin_path
  bin_path="$(find "${tmpdir}" -maxdepth 2 -type f -name node_exporter | head -n 1 || true)"
  [[ -n "${bin_path}" ]] || {
    print_error "압축 해제 후 node_exporter 바이너리를 찾지 못했습니다."
    rm -rf "${tmpdir}"
    return 1
  }

  run_as_root install -m 0755 "${bin_path}" /usr/local/bin/node_exporter
  rm -rf "${tmpdir}"
}

write_env_file() {
  local port="$1"
  local extra_args="$2"

  local escaped_extra
  escaped_extra="${extra_args//\\/\\\\}"
  escaped_extra="${escaped_extra//\"/\\\"}"

  local tmp_env
  tmp_env="$(mktemp)"
  printf 'NODE_EXPORTER_ARGS="--web.listen-address=:%s %s"\n' "${port}" "${escaped_extra}" > "${tmp_env}"

  run_as_root mkdir -p /etc/node_exporter
  run_as_root install -m 0644 "${tmp_env}" /etc/node_exporter/node_exporter.env
  rm -f "${tmp_env}"
}

write_service_file() {
  local tmp_service
  tmp_service="$(mktemp)"

  cat > "${tmp_service}" <<'EOF_SERVICE'
[Unit]
Description=Prometheus Node Exporter
Wants=network-online.target
After=network-online.target

[Service]
Type=simple
User=node_exporter
Group=node_exporter
EnvironmentFile=/etc/node_exporter/node_exporter.env
ExecStart=/usr/local/bin/node_exporter $NODE_EXPORTER_ARGS
Restart=always
RestartSec=2

[Install]
WantedBy=multi-user.target
EOF_SERVICE

  run_as_root install -m 0644 "${tmp_service}" /etc/systemd/system/node_exporter.service
  rm -f "${tmp_service}"
}

start_service() {
  if ! command -v systemctl >/dev/null 2>&1; then
    print_error "systemd 환경이 아닙니다. node_exporter 서비스 시작을 건너뜁니다."
    return 1
  fi

  run_as_root systemctl daemon-reload
  run_as_root systemctl enable --now node_exporter

  if run_as_root systemctl is-active --quiet node_exporter; then
    print_ok "node_exporter가 정상 기동되었습니다."
  else
    print_error "node_exporter 상태를 확인해주세요: systemctl status node_exporter"
    return 1
  fi
}

main() {
  local version="${DEFAULT_VERSION}"
  local port="${DEFAULT_PORT}"
  local tarball=""
  local extra_args="${DEFAULT_EXTRA_ARGS}"

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --version)
        version="$2"
        shift 2
        ;;
      --port)
        port="$2"
        shift 2
        ;;
      --tarball)
        tarball="$2"
        shift 2
        ;;
      --extra-args)
        extra_args="$2"
        shift 2
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

  [[ "${version}" == v* ]] || version="v${version}"

  local work_tmp
  work_tmp="$(mktemp -d)"
  trap 'rm -rf "${work_tmp:-}"' RETURN

  local tar_path="${tarball}"
  if [[ -z "${tar_path}" ]]; then
    tar_path="$(download_tarball "${version}" "${work_tmp}")"
  else
    [[ -f "${tar_path}" ]] || {
      print_error "tarball 파일을 찾을 수 없습니다: ${tar_path}"
      return 1
    }
  fi

  ensure_node_exporter_user
  install_binary "${tar_path}"
  write_env_file "${port}" "${extra_args}"
  write_service_file
  start_service

  print_ok "node_exporter 설치/업데이트 완료 (port: ${port}, version: ${version})"
}

main "$@"
