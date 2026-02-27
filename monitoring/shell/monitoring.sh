#!/usr/bin/env bash
set -Eeuo pipefail

########################################
# Offline/Online Docker + Monitoring Tool
# - OS/Arch 감지
# - SMTP 아웃바운드 체크 (smtp.mailplug.co.kr:465)
# - 온라인 설치 (RHEL/Ubuntu)
# - 오프라인 번들 준비 (패키지/이미지/설정)
# - 오프라인 번들 설치
########################################

SMTP_HOST="smtp.mailplug.co.kr"
SMTP_PORT="465"

print_info()  { echo "[INFO] $1"; }
print_warn()  { echo "[WARN] $1"; }
print_error() { echo "[ERROR] $1"; }
print_ok()    { echo "[DONE] $1"; }

ask() {
  local prompt="$1"
  local default="${2:-}"
  local answer=""
  if [[ -n "$default" ]]; then
    read -r -p "$prompt (기본값: $default) : " answer
    echo "${answer:-$default}"
  else
    read -r -p "$prompt : " answer
    echo "$answer"
  fi
}

ask_yn() {
  local prompt="$1"
  local default="${2:-Y}"
  local answer=""
  while true; do
    read -r -p "$prompt (Y/N, 기본값: $default) : " answer
    answer="${answer:-$default}"
    case "${answer^^}" in
      Y|YES) echo "Y"; return 0 ;;
      N|NO)  echo "N"; return 0 ;;
      *) print_warn "Y 또는 N으로 입력해주세요." ;;
    esac
  done
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || { print_error "필수 명령어가 존재하지 않습니다: $1"; return 1; }
}

########################################
# OS/Arch 감지
########################################

OS_ID="unknown"
OS_VERSION="unknown"
ARCH="unknown"
PKG_FAMILY="unknown" # rpm|deb|unknown

detect_os() {
  if [[ -f /etc/os-release ]]; then
    # shellcheck disable=SC1091
    . /etc/os-release
    OS_ID="${ID:-unknown}"
    OS_VERSION="${VERSION_ID:-unknown}"
  fi
  ARCH="$(uname -m || true)"

  if command -v dnf >/dev/null 2>&1 || command -v yum >/dev/null 2>&1; then
    PKG_FAMILY="rpm"
  elif command -v apt-get >/dev/null 2>&1; then
    PKG_FAMILY="deb"
  else
    PKG_FAMILY="unknown"
  fi

  print_info "OS: ${OS_ID} ${OS_VERSION}"
  print_info "ARCH: ${ARCH}"
  print_info "PKG_FAMILY: ${PKG_FAMILY}"
}

########################################
# SMTP 아웃바운드 체크
########################################

check_smtp_outbound() {
  print_info "SMTP 아웃바운드 연결 상태를 확인합니다. (${SMTP_HOST}:${SMTP_PORT})"
  print_info "방화벽/보안장비에서 아웃바운드가 허용되어 있어야 합니다."

  if timeout 5 bash -c ">/dev/tcp/${SMTP_HOST}/${SMTP_PORT}" 2>/dev/null; then
    print_ok "SMTP 아웃바운드 연결이 가능합니다. (${SMTP_HOST}:${SMTP_PORT})"
    return 0
  fi

  print_warn "SMTP 아웃바운드 연결이 불가능합니다. (${SMTP_HOST}:${SMTP_PORT})"
  return 1
}

########################################
# Docker 설치 여부 체크
########################################

is_docker_installed() {
  command -v docker >/dev/null 2>&1
}

########################################
# Docker 온라인 설치 (RHEL 계열)
########################################

ensure_docker_repo_rhel() {
  # dnf config-manager 필요
  print_info "dnf-plugins-core 설치를 진행합니다."
  sudo dnf -y install dnf-plugins-core

  # repo 추가 (중복 추가 방지)
  local repo_file="/etc/yum.repos.d/docker-ce.repo"
  if [[ -f "$repo_file" ]]; then
    print_info "Docker 공식 저장소가 이미 추가되어 있습니다."
    return 0
  fi

  print_info "Docker 공식 저장소를 추가합니다."
  sudo dnf config-manager --add-repo \
    https://download.docker.com/linux/centos/docker-ce.repo
}

install_docker_rhel_online() {
  # 먼저 체크
  if is_docker_installed; then
    print_ok "Docker가 이미 설치되어 있습니다."
    return 0
  fi

  print_info "Docker가 설치되어 있지 않습니다. 온라인 설치를 진행합니다."

  need_cmd dnf

  ensure_docker_repo_rhel

  print_info "Docker 패키지 설치를 진행합니다."
  sudo dnf -y install docker-ce docker-ce-cli containerd.io \
    docker-buildx-plugin docker-compose-plugin

  print_info "Docker 서비스를 활성화 및 시작합니다."
  sudo systemctl enable --now docker

  print_ok "Docker 설치가 완료되었습니다."
}

########################################
# Docker 온라인 설치 (Ubuntu 계열)
########################################

ensure_docker_repo_ubuntu() {
  need_cmd apt-get
  need_cmd curl
  need_cmd gpg

  print_info "사전 패키지 설치를 진행합니다."
  sudo apt-get update -y
  sudo apt-get install -y ca-certificates curl gnupg

  print_info "Docker GPG 키를 등록합니다."
  sudo install -m 0755 -d /etc/apt/keyrings
  if [[ ! -f /etc/apt/keyrings/docker.gpg ]]; then
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
      sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    sudo chmod a+r /etc/apt/keyrings/docker.gpg
  else
    print_info "Docker GPG 키가 이미 등록되어 있습니다."
  fi

  local list_file="/etc/apt/sources.list.d/docker.list"
  if [[ ! -f "$list_file" ]]; then
    print_info "Docker 저장소를 추가합니다."
    local codename
    codename="$(
      . /etc/os-release
      echo "${VERSION_CODENAME:-}"
    )"
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu ${codename} stable" | \
      sudo tee "$list_file" >/dev/null
  else
    print_info "Docker 저장소가 이미 추가되어 있습니다."
  fi

  sudo apt-get update -y
}

install_docker_ubuntu_online() {
  if is_docker_installed; then
    print_ok "Docker가 이미 설치되어 있습니다."
    return 0
  fi

  print_info "Docker가 설치되어 있지 않습니다. 온라인 설치를 진행합니다."

  ensure_docker_repo_ubuntu

  print_info "Docker 패키지 설치를 진행합니다."
  sudo apt-get install -y docker-ce docker-ce-cli containerd.io \
    docker-buildx-plugin docker-compose-plugin

  sudo systemctl enable --now docker

  print_ok "Docker 설치가 완료되었습니다."
}

########################################
# 오프라인 번들 준비 (외부망 준비)
########################################

write_monitoring_templates() {
  local outdir="$1"
  mkdir -p "${outdir}/monitoring/prometheus" "${outdir}/monitoring/alertmanager"

  cat > "${outdir}/monitoring/docker-compose.yml" <<'EOF'
version: "3.8"

services:
  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./prometheus/rules.yml:/etc/prometheus/rules.yml
    ports:
      - "9090:9090"
    restart: always

  alertmanager:
    image: prom/alertmanager:latest
    volumes:
      - ./alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml
    ports:
      - "9093:9093"
    restart: always

  node-exporter:
    image: prom/node-exporter:latest
    network_mode: host
    pid: host
    volumes:
      - "/:/host:ro,rslave"
    command:
      - "--path.rootfs=/host"
    restart: always
EOF

  cat > "${outdir}/monitoring/prometheus/prometheus.yml" <<'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "rules.yml"

scrape_configs:
  - job_name: "node"
    static_configs:
      - targets: ["localhost:9100"]
EOF

  cat > "${outdir}/monitoring/prometheus/rules.yml" <<'EOF'
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

  - alert: HighMemoryUsage
    expr: (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100 > 85
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High memory usage"
      description: "Memory usage > 85% for 5 minutes"

  - alert: HighDiskUsage
    expr: (node_filesystem_size_bytes{fstype!="tmpfs"} - node_filesystem_free_bytes{fstype!="tmpfs"})
          / node_filesystem_size_bytes{fstype!="tmpfs"} * 100 > 80
    for: 10m
    labels:
      severity: critical
    annotations:
      summary: "High disk usage"
      description: "Disk usage > 80% for 10 minutes"
EOF

  cat > "${outdir}/monitoring/alertmanager/alertmanager.yml" <<EOF
global:
  smtp_smarthost: '${SMTP_HOST}:${SMTP_PORT}'
  smtp_from: 'monitor@yourdomain.com'
  smtp_auth_username: 'monitor@yourdomain.com'
  smtp_auth_password: 'PASSWORD'
  smtp_require_tls: true

route:
  receiver: 'email-alert'
  group_wait: 30s
  group_interval: 10m
  repeat_interval: 4h

receivers:
- name: 'email-alert'
  email_configs:
  - to: 'admin@yourdomain.com'
    send_resolved: true
EOF

  print_ok "모니터링 샘플 설정 파일을 생성하였습니다."
}

prepare_offline_bundle_rhel() {
  local outdir="$1"
  mkdir -p "${outdir}/pkgs"

  need_cmd dnf

  print_info "안내드립니다."
  print_info "오프라인 번들 준비는 실제 설치할 서버와 동일한 운영체제 버전 및 아키텍처 환경에서 진행하셔야 합니다."
  print_info "그렇지 않으면 의존성 또는 glibc 차이로 설치가 실패할 수 있습니다."

  ensure_docker_repo_rhel

  if ! dnf -q list installed dnf-plugins-core >/dev/null 2>&1; then
    sudo dnf -y install dnf-plugins-core
  fi

  print_info "Docker 설치 패키지를 의존성 포함으로 다운로드합니다."
  # dnf download는 dnf-plugins-core에 포함됩니다.
  sudo dnf -y download --resolve --destdir "${outdir}/pkgs" \
    docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

  print_ok "RPM 패키지 번들을 준비하였습니다."
}

prepare_offline_bundle_ubuntu() {
  local outdir="$1"
  mkdir -p "${outdir}/pkgs"

  need_cmd apt-get

  print_info "안내드립니다."
  print_info "오프라인 번들 준비는 실제 설치할 서버와 동일한 운영체제 버전 및 아키텍처 환경에서 진행하셔야 합니다."
  print_info "그렇지 않으면 의존성 또는 glibc 차이로 설치가 실패할 수 있습니다."

  # Docker repo가 없으면 docker-ce 패키지를 받지 못합니다.
  print_info "Docker 저장소 설정을 진행합니다."
  if ! command -v curl >/dev/null 2>&1; then
    sudo apt-get update -y
    sudo apt-get install -y curl
  fi
  if ! command -v gpg >/dev/null 2>&1; then
    sudo apt-get update -y
    sudo apt-get install -y gnupg
  fi

  ensure_docker_repo_ubuntu

  print_info "Docker 패키지를 다운로드 전용으로 수집합니다."
  sudo apt-get install -y --download-only docker-ce docker-ce-cli containerd.io \
    docker-buildx-plugin docker-compose-plugin

  print_info "다운로드된 .deb 파일을 번들에 복사합니다."
  local cache="/var/cache/apt/archives"
  shopt -s nullglob
  local debs=("$cache"/*.deb)
  shopt -u nullglob

  if [[ ${#debs[@]} -eq 0 ]]; then
    print_error "다운로드된 .deb 파일을 찾지 못하였습니다."
    print_error "Docker 저장소 설정 또는 네트워크 상태를 확인해 주십시오."
    exit 1
  fi

  cp -v "${debs[@]}" "${outdir}/pkgs/" >/dev/null
  print_ok "DEB 패키지 번들을 준비하였습니다."
}

save_monitoring_images() {
  local outdir="$1"
  local include_grafana="$2"

  mkdir -p "${outdir}/images"
  need_cmd docker

  local prom_tag am_tag ne_tag graf_tag
  prom_tag="$(ask "prom/prometheus 버전을 입력해 주십시오" "latest")"
  am_tag="$(ask "prom/alertmanager 버전을 입력해 주십시오" "latest")"
  ne_tag="$(ask "prom/node-exporter 버전을 입력해 주십시오" "latest")"
  graf_tag="latest"
  if [[ "$include_grafana" == "Y" ]]; then
    graf_tag="$(ask "grafana/grafana 버전을 입력해 주십시오" "latest")"
  fi

  local images=("prom/prometheus:${prom_tag}" "prom/alertmanager:${am_tag}" "prom/node-exporter:${ne_tag}")
  if [[ "$include_grafana" == "Y" ]]; then
    images+=("grafana/grafana:${graf_tag}")
  fi

  print_info "이미지를 다운로드(pull)합니다."
  for img in "${images[@]}"; do
    docker pull "$img"
  done

  print_info "이미지를 tar 파일로 저장합니다."
  docker save -o "${outdir}/images/monitoring-images.tar" "${images[@]}"

  print_ok "이미지 번들을 준비하였습니다. (monitoring-images.tar)"
}

########################################
# 오프라인 번들 설치
########################################

install_from_bundle() {
  local bundle="$1"

  if [[ ! -d "$bundle" ]]; then
    print_error "번들 경로가 존재하지 않습니다."
    exit 1
  fi

  print_info "번들 경로는 ${bundle} 입니다."

  if [[ -d "${bundle}/pkgs" ]]; then
    if command -v dnf &>/dev/null; then
      print_info "RPM 패키지 설치를 진행합니다."
      sudo dnf localinstall -y "${bundle}/pkgs/"*.rpm
    elif command -v apt-get &>/dev/null; then
      print_info "DEB 패키지 설치를 진행합니다."
      sudo dpkg -i "${bundle}/pkgs/"*.deb
    else
      print_error "지원되지 않는 패키지 관리자입니다."
      exit 1
    fi
  else
    print_warn "pkgs 디렉토리가 존재하지 않습니다. 패키지 설치를 건너뜁니다."
  fi

  if command -v systemctl >/dev/null 2>&1; then
    print_info "Docker 서비스를 활성화 및 시작합니다."
    sudo systemctl enable --now docker || true
  fi

  if [[ -f "${bundle}/images/monitoring-images.tar" ]]; then
    need_cmd docker
    print_info "Docker 이미지를 로드합니다."
    docker load -i "${bundle}/images/monitoring-images.tar"
  else
    print_warn "이미지 파일이 존재하지 않습니다. 이미지 로드를 건너뜁니다."
  fi

  if [[ -f "${bundle}/monitoring/docker-compose.yml" ]]; then
    need_cmd docker
    print_info "Monitoring 서비스를 기동합니다."
    (cd "${bundle}/monitoring" && docker compose up -d)
  else
    print_warn "docker-compose.yml 파일이 존재하지 않습니다. 기동을 건너뜁니다."
  fi

  print_ok "오프라인 설치가 완료되었습니다."

  print_info "SMTP 아웃바운드 체크를 다시 진행합니다."
  check_smtp_outbound || true
}

########################################
# 메뉴
########################################

menu_prepare_bundle() {
  local outdir
  outdir="$(ask "번들을 생성할 경로(폴더)를 입력해 주십시오" "$(pwd)/bundle-$(date +%Y%m%d-%H%M%S)")"
  mkdir -p "$outdir"

  print_ok "번들 디렉토리를 생성하였습니다. (${outdir})"

  local include_templates
  include_templates="$(ask_yn "모니터링 설정 샘플 파일을 생성하시겠습니까?" "Y")"
  if [[ "$include_templates" == "Y" ]]; then
    write_monitoring_templates "$outdir"
  fi

  local include_pkgs
  include_pkgs="$(ask_yn "Docker 오프라인 설치용 패키지를 다운로드하시겠습니까?" "Y")"
  if [[ "$include_pkgs" == "Y" ]]; then
    if [[ "$PKG_FAMILY" == "rpm" ]]; then
      prepare_offline_bundle_rhel "$outdir"
    elif [[ "$PKG_FAMILY" == "deb" ]]; then
      prepare_offline_bundle_ubuntu "$outdir"
    else
      print_error "지원되지 않는 환경입니다."
      exit 1
    fi
  fi

  local include_images
  include_images="$(ask_yn "Prometheus/Alertmanager/node_exporter 이미지 번들을 생성하시겠습니까? (Docker 필요)" "Y")"
  if [[ "$include_images" == "Y" ]]; then
    if ! is_docker_installed; then
      print_warn "Docker가 설치되어 있지 않습니다. 이미지를 번들로 만들려면 Docker 설치가 필요합니다."
      local install_now
      install_now="$(ask_yn "지금 Docker 온라인 설치를 진행하시겠습니까?" "Y")"
      if [[ "$install_now" == "Y" ]]; then
        if [[ "$PKG_FAMILY" == "rpm" ]]; then
          install_docker_rhel_online
        elif [[ "$PKG_FAMILY" == "deb" ]]; then
          install_docker_ubuntu_online
        else
          print_error "지원되지 않는 환경입니다."
          exit 1
        fi
      else
        print_warn "이미지 번들 생성을 건너뜁니다."
      fi
    fi

    if is_docker_installed; then
      local with_grafana
      with_grafana="$(ask_yn "Grafana를 함께 포함하시겠습니까?" "N")"
      save_monitoring_images "$outdir" "$with_grafana"
    fi
  fi

  print_ok "번들 준비가 완료되었습니다."
  print_info "안내드립니다. 해당 폴더 전체를 폐쇄망 서버로 반입하시면 됩니다."
  print_info "폐쇄망 서버에서는 본 스크립트의 '오프라인 번들 설치' 메뉴를 이용하시면 됩니다."
}

menu_online_install() {
  if [[ "$PKG_FAMILY" == "rpm" ]]; then
    install_docker_rhel_online
  elif [[ "$PKG_FAMILY" == "deb" ]]; then
    install_docker_ubuntu_online
  else
    print_error "지원되지 않는 환경입니다."
    exit 1
  fi

  print_info "SMTP 아웃바운드 체크를 진행합니다."
  check_smtp_outbound || true
}

main() {
  detect_os

  echo ""
  echo "1) SMTP 아웃바운드 연결 체크를 진행합니다. (${SMTP_HOST}:${SMTP_PORT})"
  echo "2) Docker 온라인 설치를 진행합니다."
  echo "3) 오프라인 번들 준비(외부망 준비)를 진행합니다."
  echo "4) 오프라인 번들 설치(폐쇄망 설치)를 진행합니다."
  echo "5) 종료합니다."
  echo ""

  local choice
  choice="$(ask "번호를 선택해 주십시오" "1")"

  case "$choice" in
    1)
      check_smtp_outbound || true
      ;;
    2)
      menu_online_install
      ;;
    3)
      menu_prepare_bundle
      ;;
    4)
      local bundle
      bundle="$(ask "반입한 번들 디렉터리 경로를 입력해 주십시오" "$(pwd)")"
      install_from_bundle "$bundle"
      ;;
    5)
      print_info "프로그램을 종료합니다."
      ;;
    *)
      print_error "잘못된 선택입니다."
      exit 1
      ;;
  esac
}

main
