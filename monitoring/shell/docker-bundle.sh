#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MONITORING_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMMON_LIB="${SCRIPT_DIR}/lib/common.sh"

# shellcheck disable=SC1091
source "${COMMON_LIB}"

DEFAULT_PROMETHEUS_TAG="v3.10.0"
DEFAULT_ALERTMANAGER_TAG="v0.31.0"
DEFAULT_GRAFANA_TAG="12.3.4"

usage() {
  cat <<USAGE
사용법:
  $(basename "$0") install-docker-online [--family auto|rpm|deb]
  $(basename "$0") prepare-pkgs --outdir <dir> [--family auto|rpm|deb]
  $(basename "$0") prepare-images --outdir <dir> [--prometheus-tag <tag>] [--alertmanager-tag <tag>] [--include-grafana] [--grafana-tag <tag>]
  $(basename "$0") prepare-bundle --outdir <dir> [--family auto|rpm|deb] [--skip-pkgs] [--skip-images] [--prometheus-tag <tag>] [--alertmanager-tag <tag>] [--include-grafana] [--grafana-tag <tag>]
  $(basename "$0") install-bundle --bundle <dir> [--with-ui] [--skip-pkgs] [--skip-images]
USAGE
}

resolve_family() {
  local family="$1"
  if [[ "${family}" == "auto" ]]; then
    family="$(detect_pkg_family)"
  fi

  case "${family}" in
    rpm|deb)
      printf '%s' "${family}"
      ;;
    *)
      print_error "지원하지 않는 패키지 계열입니다: ${family}"
      return 1
      ;;
  esac
}

ensure_docker_repo_rhel() {
  need_cmd dnf

  run_as_root dnf -y install dnf-plugins-core
  local repo_file="/etc/yum.repos.d/docker-ce.repo"
  if [[ ! -f "${repo_file}" ]]; then
    run_as_root dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
  fi
}

install_docker_rhel_online() {
  if command -v docker >/dev/null 2>&1; then
    print_ok "Docker가 이미 설치되어 있습니다."
    return 0
  fi

  ensure_docker_repo_rhel
  run_as_root dnf -y install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  run_as_root systemctl enable --now docker
  print_ok "Docker 설치가 완료되었습니다."
}

ensure_docker_repo_ubuntu() {
  need_cmd apt-get
  need_cmd curl
  need_cmd gpg
  need_cmd dpkg

  run_as_root apt-get update -y
  run_as_root apt-get install -y ca-certificates curl gnupg

  run_as_root install -m 0755 -d /etc/apt/keyrings
  if [[ ! -f /etc/apt/keyrings/docker.gpg ]]; then
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | run_as_root gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    run_as_root chmod a+r /etc/apt/keyrings/docker.gpg
  fi

  local list_file="/etc/apt/sources.list.d/docker.list"
  if [[ ! -f "${list_file}" ]]; then
    local codename
    codename="$(
      . /etc/os-release
      echo "${VERSION_CODENAME:-}"
    )"
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu ${codename} stable" | run_as_root tee "${list_file}" >/dev/null
  fi

  run_as_root apt-get update -y
}

install_docker_ubuntu_online() {
  if command -v docker >/dev/null 2>&1; then
    print_ok "Docker가 이미 설치되어 있습니다."
    return 0
  fi

  ensure_docker_repo_ubuntu
  run_as_root apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  run_as_root systemctl enable --now docker
  print_ok "Docker 설치가 완료되었습니다."
}

prepare_offline_bundle_rhel() {
  local outdir="$1"
  mkdir -p "${outdir}/pkgs"

  need_cmd dnf
  ensure_docker_repo_rhel

  run_as_root dnf -y download --resolve --destdir "${outdir}/pkgs" \
    docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

  print_ok "RPM 패키지 번들을 준비했습니다."
}

prepare_offline_bundle_ubuntu() {
  local outdir="$1"
  mkdir -p "${outdir}/pkgs"

  need_cmd apt-get
  ensure_docker_repo_ubuntu

  run_as_root apt-get clean
  run_as_root apt-get update -y
  run_as_root apt-get install -y --download-only docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

  local cache="/var/cache/apt/archives"
  shopt -s nullglob
  local debs=("${cache}"/*.deb)
  shopt -u nullglob

  if [[ ${#debs[@]} -eq 0 ]]; then
    print_error "다운로드된 .deb 파일을 찾지 못했습니다."
    return 1
  fi

  cp -v "${debs[@]}" "${outdir}/pkgs/"
  print_ok "DEB 패키지 번들을 준비했습니다."
}

write_bundle_monitoring_templates() {
  local outdir="$1"
  local prom_tag="$2"
  local am_tag="$3"
  local graf_tag="$4"

  mkdir -p "${outdir}/monitoring/prometheus/targets" "${outdir}/monitoring/alertmanager/templates"

  cat > "${outdir}/monitoring/docker-compose.yml" <<EOF_COMPOSE
services:
  prometheus:
    image: prom/prometheus:${prom_tag}
    container_name: monitoring-prometheus
    restart: unless-stopped
    command:
      - --config.file=/etc/prometheus/prometheus.yml
      - --storage.tsdb.path=/prometheus
      - --storage.tsdb.retention.time=30d
      - --web.enable-lifecycle
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - ./prometheus/rules.yml:/etc/prometheus/rules.yml:ro
      - ./prometheus/targets:/etc/prometheus/targets:ro
      - prometheus-data:/prometheus
    ports:
      - "9090:9090"
    depends_on:
      - alertmanager

  alertmanager:
    image: prom/alertmanager:${am_tag}
    container_name: monitoring-alertmanager
    restart: unless-stopped
    command:
      - --config.file=/etc/alertmanager/alertmanager.yml
      - --storage.path=/alertmanager
    volumes:
      - ./alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml:ro
      - ./alertmanager/templates:/etc/alertmanager/templates:ro
      - alertmanager-data:/alertmanager
    ports:
      - "9093:9093"

  grafana:
    image: grafana/grafana:${graf_tag}
    container_name: monitoring-grafana
    restart: unless-stopped
    profiles:
      - ui
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: AdminPassword!
    volumes:
      - grafana-data:/var/lib/grafana
    ports:
      - "3000:3000"
    depends_on:
      - prometheus

volumes:
  prometheus-data:
  alertmanager-data:
  grafana-data:
EOF_COMPOSE

  cat > "${outdir}/monitoring/prometheus/prometheus.yml" <<'EOF_PROMETHEUS'
global:
  scrape_interval: 15s
  evaluation_interval: 30s
  external_labels:
    site: "UNKNOWN_SITE"

rule_files:
  - /etc/prometheus/rules.yml

alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - alertmanager:9093

scrape_configs:
  - job_name: prometheus
    static_configs:
      - targets:
          - prometheus:9090

  - job_name: node-exporter
    file_sd_configs:
      - files:
          - /etc/prometheus/targets/node-exporter.yml
EOF_PROMETHEUS

  if [[ -f "${MONITORING_DIR}/prometheus/rules.yml" ]]; then
    cp "${MONITORING_DIR}/prometheus/rules.yml" "${outdir}/monitoring/prometheus/rules.yml"
  else
    cat > "${outdir}/monitoring/prometheus/rules.yml" <<'EOF_RULES'
groups:
- name: system-alerts
  rules:
  - alert: HighCPUUsage
    expr: 100 - (avg by(instance)(irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "High CPU usage"
      description: "CPU usage > 80% for 5 minutes"
EOF_RULES
  fi

  if [[ -f "${MONITORING_DIR}/alertmanager/alertmanager.yml" ]]; then
    cp "${MONITORING_DIR}/alertmanager/alertmanager.yml" "${outdir}/monitoring/alertmanager/alertmanager.yml"
  else
    cat > "${outdir}/monitoring/alertmanager/alertmanager.yml" <<'EOF_ALERT'
global:
  smtp_smarthost: 'smtp.mailplug.co.kr:465'
  smtp_from: 'no-reply@pntbiz.com'
  smtp_auth_username: 'no-reply@pntbiz.com'
  smtp_auth_password: 'rkskek12345'
  smtp_require_tls: true

route:
  receiver: 'email-alert'

receivers:
- name: 'email-alert'
  email_configs:
  - to: 'username@pntbiz.com'
    send_resolved: true
EOF_ALERT
  fi

  if [[ -f "${MONITORING_DIR}/alertmanager/templates/email.tmpl" ]]; then
    cp "${MONITORING_DIR}/alertmanager/templates/email.tmpl" "${outdir}/monitoring/alertmanager/templates/email.tmpl"
  fi

  cat > "${outdir}/monitoring/prometheus/targets/node-exporter.yml" <<'EOF_TARGETS'
- targets:
    - host.docker.internal:19100
  labels:
EOF_TARGETS

  print_ok "모니터링 설정 템플릿을 준비했습니다."
}

prepare_images() {
  local outdir="$1"
  local prom_tag="$2"
  local am_tag="$3"
  local include_grafana="$4"
  local graf_tag="$5"

  mkdir -p "${outdir}/images"
  need_cmd docker

  local images=("prom/prometheus:${prom_tag}" "prom/alertmanager:${am_tag}")
  if [[ "${include_grafana}" == "Y" ]]; then
    images+=("grafana/grafana:${graf_tag}")
  fi

  print_info "이미지를 다운로드합니다."
  for img in "${images[@]}"; do
    docker_cmd pull "${img}"
  done

  docker_cmd save -o "${outdir}/images/monitoring-images.tar" "${images[@]}"
  print_ok "이미지 번들을 준비했습니다."
}

install_pkgs_from_bundle() {
  local bundle="$1"

  if [[ ! -d "${bundle}/pkgs" ]]; then
    print_warn "pkgs 디렉터리가 없습니다. 패키지 설치를 건너뜁니다."
    return 0
  fi

  shopt -s nullglob
  local rpms=("${bundle}/pkgs"/*.rpm)
  local debs=("${bundle}/pkgs"/*.deb)
  shopt -u nullglob

  if [[ ${#rpms[@]} -gt 0 ]] && command -v dnf >/dev/null 2>&1; then
    run_as_root dnf localinstall -y "${rpms[@]}"
    return 0
  fi

  if [[ ${#debs[@]} -gt 0 ]] && command -v apt >/dev/null 2>&1; then
    run_as_root apt -y install "${debs[@]}" || {
      print_warn "apt 설치 실패, dpkg로 재시도합니다."
      run_as_root dpkg -i "${debs[@]}" || true
    }
    return 0
  fi

  if [[ ${#debs[@]} -gt 0 ]] && command -v dpkg >/dev/null 2>&1; then
    run_as_root dpkg -i "${debs[@]}" || true
    return 0
  fi

  print_warn "설치 가능한 패키지를 찾지 못했습니다."
}

install_images_from_bundle() {
  local bundle="$1"
  local tar_file="${bundle}/images/monitoring-images.tar"

  if [[ ! -f "${tar_file}" ]]; then
    print_warn "이미지 번들 파일이 없습니다: ${tar_file}"
    return 0
  fi

  need_cmd docker
  docker_cmd load -i "${tar_file}"
  print_ok "이미지 로드가 완료되었습니다."
}

bring_up_monitoring_bundle() {
  local bundle="$1"
  local with_ui="$2"
  local monitoring_path="${bundle}/monitoring"

  if [[ ! -f "${monitoring_path}/docker-compose.yml" ]]; then
    print_warn "monitoring/docker-compose.yml이 없어 스택 기동을 건너뜁니다."
    return 0
  fi

  need_cmd docker
  if [[ "${with_ui}" == "Y" ]]; then
    (cd "${monitoring_path}" && docker_compose_cmd --profile ui up -d)
  else
    (cd "${monitoring_path}" && docker_compose_cmd up -d)
  fi

  print_ok "번들 기반 모니터링 스택 기동이 완료되었습니다."
}

cmd_install_docker_online() {
  local family="auto"

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --family)
        family="$2"
        shift 2
        ;;
      -h|--help)
        usage
        return 0
        ;;
      *)
        print_error "알 수 없는 옵션입니다: $1"
        return 1
        ;;
    esac
  done

  family="$(resolve_family "${family}")"
  if [[ "${family}" == "rpm" ]]; then
    install_docker_rhel_online
  else
    install_docker_ubuntu_online
  fi
}

cmd_prepare_pkgs() {
  local outdir=""
  local family="auto"

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --outdir)
        outdir="$2"
        shift 2
        ;;
      --family)
        family="$2"
        shift 2
        ;;
      -h|--help)
        usage
        return 0
        ;;
      *)
        print_error "알 수 없는 옵션입니다: $1"
        return 1
        ;;
    esac
  done

  [[ -n "${outdir}" ]] || {
    print_error "--outdir는 필수입니다."
    return 1
  }

  mkdir -p "${outdir}"
  family="$(resolve_family "${family}")"

  if [[ "${family}" == "rpm" ]]; then
    prepare_offline_bundle_rhel "${outdir}"
  else
    prepare_offline_bundle_ubuntu "${outdir}"
  fi
}

cmd_prepare_images() {
  local outdir=""
  local prom_tag="${DEFAULT_PROMETHEUS_TAG}"
  local am_tag="${DEFAULT_ALERTMANAGER_TAG}"
  local include_grafana="N"
  local graf_tag="${DEFAULT_GRAFANA_TAG}"

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --outdir)
        outdir="$2"
        shift 2
        ;;
      --prometheus-tag)
        prom_tag="$2"
        shift 2
        ;;
      --alertmanager-tag)
        am_tag="$2"
        shift 2
        ;;
      --include-grafana)
        include_grafana="Y"
        shift
        ;;
      --grafana-tag)
        graf_tag="$2"
        shift 2
        ;;
      -h|--help)
        usage
        return 0
        ;;
      *)
        print_error "알 수 없는 옵션입니다: $1"
        return 1
        ;;
    esac
  done

  [[ -n "${outdir}" ]] || {
    print_error "--outdir는 필수입니다."
    return 1
  }

  prepare_images "${outdir}" "${prom_tag}" "${am_tag}" "${include_grafana}" "${graf_tag}"
}

cmd_prepare_bundle() {
  local outdir=""
  local family="auto"
  local skip_pkgs="N"
  local skip_images="N"
  local prom_tag="${DEFAULT_PROMETHEUS_TAG}"
  local am_tag="${DEFAULT_ALERTMANAGER_TAG}"
  local include_grafana="N"
  local graf_tag="${DEFAULT_GRAFANA_TAG}"

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --outdir)
        outdir="$2"
        shift 2
        ;;
      --family)
        family="$2"
        shift 2
        ;;
      --skip-pkgs)
        skip_pkgs="Y"
        shift
        ;;
      --skip-images)
        skip_images="Y"
        shift
        ;;
      --prometheus-tag)
        prom_tag="$2"
        shift 2
        ;;
      --alertmanager-tag)
        am_tag="$2"
        shift 2
        ;;
      --include-grafana)
        include_grafana="Y"
        shift
        ;;
      --grafana-tag)
        graf_tag="$2"
        shift 2
        ;;
      -h|--help)
        usage
        return 0
        ;;
      *)
        print_error "알 수 없는 옵션입니다: $1"
        return 1
        ;;
    esac
  done

  [[ -n "${outdir}" ]] || {
    print_error "--outdir는 필수입니다."
    return 1
  }

  mkdir -p "${outdir}"
  write_bundle_monitoring_templates "${outdir}" "${prom_tag}" "${am_tag}" "${graf_tag}"

  if [[ "${skip_pkgs}" != "Y" ]]; then
    family="$(resolve_family "${family}")"
    if [[ "${family}" == "rpm" ]]; then
      prepare_offline_bundle_rhel "${outdir}"
    else
      prepare_offline_bundle_ubuntu "${outdir}"
    fi
  else
    print_info "--skip-pkgs 옵션으로 패키지 번들을 건너뜁니다."
  fi

  if [[ "${skip_images}" != "Y" ]]; then
    prepare_images "${outdir}" "${prom_tag}" "${am_tag}" "${include_grafana}" "${graf_tag}"
  else
    print_info "--skip-images 옵션으로 이미지 번들을 건너뜁니다."
  fi

  print_ok "통합 번들 준비가 완료되었습니다."
}

cmd_install_bundle() {
  local bundle=""
  local with_ui="N"
  local skip_pkgs="N"
  local skip_images="N"

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --bundle)
        bundle="$2"
        shift 2
        ;;
      --with-ui)
        with_ui="Y"
        shift
        ;;
      --skip-pkgs)
        skip_pkgs="Y"
        shift
        ;;
      --skip-images)
        skip_images="Y"
        shift
        ;;
      -h|--help)
        usage
        return 0
        ;;
      *)
        print_error "알 수 없는 옵션입니다: $1"
        return 1
        ;;
    esac
  done

  [[ -n "${bundle}" ]] || {
    print_error "--bundle은 필수입니다."
    return 1
  }
  [[ -d "${bundle}" ]] || {
    print_error "번들 경로가 존재하지 않습니다: ${bundle}"
    return 1
  }

  if [[ "${skip_pkgs}" != "Y" ]]; then
    install_pkgs_from_bundle "${bundle}"
  fi

  if command -v systemctl >/dev/null 2>&1; then
    run_as_root systemctl enable --now docker || true
  fi

  if [[ "${skip_images}" != "Y" ]]; then
    install_images_from_bundle "${bundle}"
  fi

  bring_up_monitoring_bundle "${bundle}" "${with_ui}"
}

main() {
  local command="${1:-help}"
  shift || true

  case "${command}" in
    install-docker-online)
      cmd_install_docker_online "$@"
      ;;
    prepare-pkgs)
      cmd_prepare_pkgs "$@"
      ;;
    prepare-images)
      cmd_prepare_images "$@"
      ;;
    prepare-bundle)
      cmd_prepare_bundle "$@"
      ;;
    install-bundle)
      cmd_install_bundle "$@"
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
