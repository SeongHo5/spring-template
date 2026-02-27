#!/usr/bin/env bash

print_info() {
  echo "[INFO] $1"
}

print_warn() {
  echo "[WARN] $1"
}

print_error() {
  echo "[ERROR] $1"
}

print_ok() {
  echo "[DONE] $1"
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    print_error "필수 명령어가 없습니다: $1"
    return 1
  }
}

run_as_root() {
  if [[ "${EUID:-$(id -u)}" -eq 0 ]]; then
    "$@"
    return $?
  fi

  if ! command -v sudo >/dev/null 2>&1; then
    print_error "sudo 명령어를 찾지 못했습니다. root로 실행하거나 sudo를 설치해주세요."
    return 1
  fi

  sudo "$@"
}

trim() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "$value"
}

detect_pkg_family() {
  if command -v dnf >/dev/null 2>&1 || command -v yum >/dev/null 2>&1; then
    echo "rpm"
  elif command -v apt-get >/dev/null 2>&1; then
    echo "deb"
  else
    echo "unknown"
  fi
}

docker_needs_sudo() {
  command -v docker >/dev/null 2>&1 || return 0

  local output
  output="$(docker info 2>&1 || true)"
  if echo "$output" | grep -qi "permission denied"; then
    return 0
  fi

  return 1
}

docker_cmd() {
  if docker_needs_sudo; then
    run_as_root docker "$@"
  else
    docker "$@"
  fi
}

docker_compose_cmd() {
  docker_cmd compose "$@"
}
