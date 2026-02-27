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

# 모니터링 기본 이미지 버전(요청하신 compose 기준)
DEFAULT_PROMETHEUS_TAG="v3.10.0"
DEFAULT_ALERTMANAGER_TAG="v0.31.0"
DEFAULT_NODE_EXPORTER_TAG="v0.10.2"
DEFAULT_GRAFANA_TAG="12.3.4"

print_info()  { echo "[INFO] $1"; }
print_warn()  { echo "[WARN] $1"; }
print_error() { echo "[ERROR] $1"; }
print_ok()    { echo "[DONE] $1"; }

run_as_root() {
  if [[ "${EUID:-$(id -u)}" -eq 0 ]]; then
    "$@"
  else
    command -v sudo >/dev/null 2>&1 || { print_error "sudo 명령어를 찾지 못했습니다. root로 실행하거나 sudo를 설치해주세요."; return 1; }
    sudo "$@"
  fi
}

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
  command -v "$1" >/dev/null 2>&1 || { print_error "필수 명령어가 없습니다: $1"; return 1; }
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

show_env_block() {
  echo "  - OS            : ${OS_ID} ${OS_VERSION}"
  echo "  - ARCH          : ${ARCH}"
  echo "  - PKG_FAMILY    : ${PKG_FAMILY}"
}

confirm_offline_bundle_env() {
  echo ""
  echo "****************************************************************"
  echo "중요 안내"
  echo ""
  echo "오프라인 번들은 '설치 대상 서버'와 동일한 OS/버전/아키텍처 환경에서 준비해야 합니다."
  echo "환경이 다르면 의존성 또는 glibc 차이로 설치가 실패할 수 있습니다."
  echo "****************************************************************"
  echo ""
  echo "현재 번들을 준비 중인 서버 환경은 다음과 같습니다."
  show_env_block
  echo ""

  local ok
  ok="$(ask_yn "설치 대상 서버도 위 환경과 완전히 동일합니까? (Y 입력 시에만 진행)" "N")"
  if [[ "$ok" != "Y" ]]; then
    print_warn "번들 준비를 중단합니다."
    return 1
  fi

  return 0
}

########################################
# SMTP 아웃바운드 체크
########################################

check_smtp_outbound() {
  print_info "SMTP 아웃바운드 연결을 확인합니다. (${SMTP_HOST}:${SMTP_PORT})"
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
  need_cmd dnf

  print_info "dnf-plugins-core 설치를 진행합니다."
  run_as_root dnf -y install dnf-plugins-core

  local repo_file="/etc/yum.repos.d/docker-ce.repo"
  if [[ -f "$repo_file" ]]; then
    print_info "Docker 공식 저장소가 이미 추가되어 있습니다."
    return 0
  fi

  print_info "Docker 공식 저장소를 추가합니다."
  run_as_root dnf config-manager --add-repo \
    https://download.docker.com/linux/centos/docker-ce.repo
}

install_docker_rhel_online() {
  if is_docker_installed; then
    print_ok "Docker가 이미 설치되어 있습니다."
    return 0
  fi

  print_info "Docker가 설치되어 있지 않습니다. 온라인 설치를 진행합니다."
  ensure_docker_repo_rhel

  print_info "Docker 패키지 설치를 진행합니다."
  run_as_root dnf -y install docker-ce docker-ce-cli containerd.io \
    docker-buildx-plugin docker-compose-plugin

  print_info "Docker 서비스를 활성화 및 시작합니다."
  run_as_root systemctl enable --now docker

  print_ok "Docker 설치가 완료되었습니다."
}

########################################
# Docker 온라인 설치 (Ubuntu 계열)
########################################

ensure_docker_repo_ubuntu() {
  need_cmd apt-get
  need_cmd curl
  need_cmd gpg
  need_cmd dpkg

  print_info "사전 패키지 설치를 진행합니다."
  run_as_root apt-get update -y
  run_as_root apt-get install -y ca-certificates curl gnupg

  print_info "Docker GPG 키를 등록합니다."
  run_as_root install -m 0755 -d /etc/apt/keyrings
  if [[ ! -f /etc/apt/keyrings/docker.gpg ]]; then
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
      run_as_root gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    run_as_root chmod a+r /etc/apt/keyrings/docker.gpg
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
      run_as_root tee "$list_file" >/dev/null
  else
    print_info "Docker 저장소가 이미 추가되어 있습니다."
  fi

  run_as_root apt-get update -y
}

install_docker_ubuntu_online() {
  if is_docker_installed; then
    print_ok "Docker가 이미 설치되어 있습니다."
    return 0
  fi

  print_info "Docker가 설치되어 있지 않습니다. 온라인 설치를 진행합니다."
  ensure_docker_repo_ubuntu

  print_info "Docker 패키지 설치를 진행합니다."
  run_as_root apt-get install -y docker-ce docker-ce-cli containerd.io \
    docker-buildx-plugin docker-compose-plugin

  run_as_root systemctl enable --now docker
  print_ok "Docker 설치가 완료되었습니다."
}

########################################
# 오프라인 번들 준비 (외부망 준비)
########################################

write_monitoring_templates() {
  local outdir="$1"
  mkdir -p "${outdir}/monitoring/prometheus" \
           "${outdir}/monitoring/alertmanager/templates"

  cat > "${outdir}/monitoring/docker-compose.yml" <<EOF
version: "3.8"

services:
  prometheus:
    image: prom/prometheus:${DEFAULT_PROMETHEUS_TAG}
    container_name: monitoring-prometheus
    restart: unless-stopped
    command:
      - --config.file=/etc/prometheus/prometheus.yml
      - --storage.tsdb.path=/prometheus
      - --storage.tsdb.retention.time=30d
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - ./prometheus/rules.yml:/etc/prometheus/rules.yml:ro
      - prometheus-data:/prometheus
    ports:
      - "9090:9090"
    depends_on:
      - node-exporter
      - alertmanager

  alertmanager:
    image: prom/alertmanager:${DEFAULT_ALERTMANAGER_TAG}
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

  # Linux 호스트 기준 컨테이너 방식 예시입니다.
  # 운영 환경에서 더 정확한 호스트 메트릭이 필요하면 node_exporter를 host에 직접 설치하는 구성이 더 낫습니다.
  node-exporter:
    image: prom/node-exporter:${DEFAULT_NODE_EXPORTER_TAG}
    container_name: monitoring-node-exporter
    restart: unless-stopped
    command:
      - --path.rootfs=/host
      - --collector.systemd
      - --collector.processes
    pid: host
    volumes:
      - /:/host:ro,rslave
    ports:
      - "9100:9100"

  # 추후 UI가 필요할 때만 활성화합니다.
  # docker compose --profile ui up -d
  grafana:
    image: grafana/grafana:${DEFAULT_GRAFANA_TAG}
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
EOF

  cat > "${outdir}/monitoring/prometheus/prometheus.yml" <<'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets: ["alertmanager:9093"]

scrape_configs:
  - job_name: "node-exporter"
    static_configs:
      - targets: ["node-exporter:9100"]
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
    expr: 100 - (node_filesystem_avail_bytes{fstype!="tmpfs"} / node_filesystem_size_bytes{fstype!="tmpfs"} * 100) > 80
    for: 10m
    labels:
      severity: critical
    annotations:
      summary: "High disk usage"
      description: "Disk usage > 80% for 10 minutes"
EOF

  cat > "${outdir}/monitoring/alertmanager/alertmanager.yml" <<EOF
global:
  # 참고: 465는 보통 SMTPS(Implicit TLS) 포트입니다.
  # Alertmanager는 STARTTLS 구성이 일반적이어서, 메일 서버 정책에 따라 587 사용이 더 잘 맞을 수 있습니다.
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

  # 템플릿 폴더 placeholder
  cat > "${outdir}/monitoring/alertmanager/templates/README.txt" <<'EOF'
이 폴더는 Alertmanager 알림 템플릿을 넣는 위치입니다.
EOF

  print_ok "모니터링 샘플 설정 파일을 생성했습니다."
}

prepare_offline_bundle_rhel() {
  local outdir="$1"
  mkdir -p "${outdir}/pkgs"
  need_cmd dnf

  ensure_docker_repo_rhel

  print_info "Docker 설치 RPM 패키지를(의존성 포함) 다운로드합니다."
  run_as_root dnf -y download --resolve --destdir "${outdir}/pkgs" \
    docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

  print_ok "RPM 패키지 번들을 준비했습니다."
}

prepare_offline_bundle_ubuntu() {
  local outdir="$1"
  mkdir -p "${outdir}/pkgs"

  need_cmd apt-get

  print_info "Docker 저장소 설정을 진행합니다."
  if ! command -v curl >/dev/null 2>&1; then
    run_as_root apt-get update -y
    run_as_root apt-get install -y curl
  fi
  if ! command -v gpg >/dev/null 2>&1; then
    run_as_root apt-get update -y
    run_as_root apt-get install -y gnupg
  fi

  ensure_docker_repo_ubuntu

  print_info "기존 apt 캐시를 정리합니다(불필요한 .deb 섞임 방지)."
  run_as_root apt-get clean

  print_info "Docker 패키지를 다운로드 전용으로 수집합니다."
  run_as_root apt-get update -y
  run_as_root apt-get install -y --download-only docker-ce docker-ce-cli containerd.io \
    docker-buildx-plugin docker-compose-plugin

  print_info "다운로드된 .deb 파일을 번들에 복사합니다."
  local cache="/var/cache/apt/archives"
  shopt -s nullglob
  local debs=("$cache"/*.deb)
  shopt -u nullglob

  if [[ ${#debs[@]} -eq 0 ]]; then
    print_error "다운로드된 .deb 파일을 찾지 못했습니다."
    print_error "Docker 저장소 설정 또는 네트워크 상태를 확인해주세요."
    exit 1
  fi

  cp -v "${debs[@]}" "${outdir}/pkgs/"
  print_ok "DEB 패키지 번들을 준비했습니다."
}

save_monitoring_images() {
  local outdir="$1"
  local include_grafana="$2"

  mkdir -p "${outdir}/images"
  need_cmd docker

  local prom_tag am_tag ne_tag graf_tag
  prom_tag="$(ask "prom/prometheus 버전을 입력해주세요" "${DEFAULT_PROMETHEUS_TAG}")"
  am_tag="$(ask "prom/alertmanager 버전을 입력해주세요" "${DEFAULT_ALERTMANAGER_TAG}")"
  ne_tag="$(ask "prom/node-exporter 버전을 입력해주세요" "${DEFAULT_NODE_EXPORTER_TAG}")"
  graf_tag="${DEFAULT_GRAFANA_TAG}"
  if [[ "$include_grafana" == "Y" ]]; then
    graf_tag="$(ask "grafana/grafana 버전을 입력해주세요" "${DEFAULT_GRAFANA_TAG}")"
  fi

  local images=("prom/prometheus:${prom_tag}" "prom/alertmanager:${am_tag}" "prom/node-exporter:${ne_tag}")
  if [[ "$include_grafana" == "Y" ]]; then
    images+=("grafana/grafana:${graf_tag}")
  fi

  print_info "이미지를 다운로드(pull)합니다."
  for img in "${images[@]}"; do
    run_as_root docker pull "$img"
  done

  print_info "이미지를 tar 파일로 저장합니다."
  run_as_root docker save -o "${outdir}/images/monitoring-images.tar" "${images[@]}"

  print_ok "이미지 번들을 준비했습니다. (monitoring-images.tar)"
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

  print_info "번들 경로: ${bundle}"

  if [[ -d "${bundle}/pkgs" ]]; then
    if command -v dnf &>/dev/null; then
      print_info "RPM 패키지 설치를 진행합니다."
      run_as_root dnf localinstall -y "${bundle}/pkgs/"*.rpm
    elif command -v apt &>/dev/null; then
      print_info "DEB 패키지 설치를 진행합니다."
      # apt는 로컬 파일 설치 시 의존성 해석이 dpkg보다 편합니다(번들에 의존성 포함 가정).
      run_as_root apt -y install "${bundle}/pkgs/"*.deb || {
        print_warn "apt 설치가 실패했습니다. dpkg 방식으로 재시도합니다."
        run_as_root dpkg -i "${bundle}/pkgs/"*.deb || true
        print_warn "의존성 문제가 남아 있다면, 번들에 누락된 .deb가 없는지 확인해주세요."
      }
    elif command -v dpkg &>/dev/null; then
      print_info "DEB 패키지 설치를 진행합니다."
      run_as_root dpkg -i "${bundle}/pkgs/"*.deb || true
      print_warn "의존성 문제가 남아 있다면, 번들에 누락된 .deb가 없는지 확인해주세요."
    else
      print_error "지원되지 않는 패키지 관리자입니다."
      exit 1
    fi
  else
    print_warn "pkgs 디렉터리가 없습니다. 패키지 설치는 건너뜁니다."
  fi

  if command -v systemctl >/dev/null 2>&1; then
    print_info "Docker 서비스를 활성화 및 시작합니다."
    run_as_root systemctl enable --now docker || true
  fi

  if [[ -f "${bundle}/images/monitoring-images.tar" ]]; then
    need_cmd docker
    print_info "Docker 이미지를 로드합니다."
    docker load -i "${bundle}/images/monitoring-images.tar"
  else
    print_warn "이미지 파일이 없습니다. 이미지 로드는 건너뜁니다."
  fi

  if [[ -f "${bundle}/monitoring/docker-compose.yml" ]]; then
    need_cmd docker
    print_info "Monitoring 서비스를 기동합니다."
    (cd "${bundle}/monitoring" && docker compose up -d)
    print_info "Grafana가 필요하면 다음 명령으로 올리시면 됩니다."
    print_info "  (cd \"${bundle}/monitoring\" && docker compose --profile ui up -d)"
  else
    print_warn "docker-compose.yml 파일이 없습니다. 기동은 건너뜁니다."
  fi

  print_ok "오프라인 설치가 완료되었습니다."

  print_info "SMTP 아웃바운드 체크를 다시 진행합니다."
  check_smtp_outbound || true
}

########################################
# 메뉴
########################################

menu_prepare_bundle() {
  # 요구사항: 오프라인 번들 준비 선택 시 '가장 먼저' 안내/확인
  confirm_offline_bundle_env || return 0

  local outdir
  outdir="$(ask "번들을 생성할 경로(폴더)를 입력해주세요" "$(pwd)/bundle-$(date +%Y%m%d-%H%M%S)")"
  mkdir -p "$outdir"
  print_ok "번들 디렉터리를 생성했습니다. (${outdir})"

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
      print_warn "Docker가 설치되어 있지 않습니다. 이미지 번들 생성에는 Docker가 필요합니다."
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
        print_warn "이미지 번들 생성은 건너뜁니다."
      fi
    fi

    if is_docker_installed; then
      local with_grafana
      with_grafana="$(ask_yn "Grafana 이미지도 함께 포함하시겠습니까?" "N")"
      save_monitoring_images "$outdir" "$with_grafana"
    fi
  fi

  print_ok "번들 준비가 완료되었습니다."
  print_info "해당 폴더 전체를 폐쇄망 서버로 반입하시면 됩니다."
  print_info "폐쇄망 서버에서는 이 스크립트의 '오프라인 번들 설치' 메뉴를 사용해주세요."
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
  choice="$(ask "번호를 선택해주세요" "1")"

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
      bundle="$(ask "반입한 번들 디렉터리 경로를 입력해주세요" "$(pwd)")"
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
