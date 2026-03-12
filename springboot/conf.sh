#!/usr/bin/env bash
set -euo pipefail

# 관리할 앱 목록
APPS=("admin" "api" "gateway")

# 무중단/배포 순서(의존성 반영)
# 예: gateway가 api를 물고, api가 admin을 물면 gateway 먼저 올리고 마지막에 admin 올리는 식으로
DEPLOY_ORDER=("gateway" "api" "admin")

# 앱 홈(각 앱마다 jar, logs, run, releases 등을 보관)
declare -A APP_HOME=(
  ["admin"]="/opt/admin"
  ["api"]="/opt/api"
  ["gateway"]="/opt/gateway"
)

# current/previous 링크
declare -A CURRENT_JAR=(
  ["admin"]="${APP_HOME[admin]}/current.jar"
  ["api"]="${APP_HOME[api]}/current.jar"
  ["gateway"]="${APP_HOME[gateway]}/current.jar"
)
declare -A PREVIOUS_JAR=(
  ["admin"]="${APP_HOME[admin]}/previous.jar"
  ["api"]="${APP_HOME[api]}/previous.jar"
  ["gateway"]="${APP_HOME[gateway]}/previous.jar"
)

# 릴리즈 저장 디렉토리
declare -A RELEASES_DIR=(
  ["admin"]="${APP_HOME[admin]}/releases"
  ["api"]="${APP_HOME[api]}/releases"
  ["gateway"]="${APP_HOME[gateway]}/releases"
)

# Blue/Green 포트
declare -A PORT_BLUE=(
  ["admin"]="8081"
  ["api"]="8091"
  ["gateway"]="8101"
)
declare -A PORT_GREEN=(
  ["admin"]="8082"
  ["api"]="8092"
  ["gateway"]="8102"
)

# Health endpoint
declare -A HEALTH_PATH=(
  ["admin"]="/actuator/health"
  ["api"]="/actuator/health"
  ["gateway"]="/actuator/health"
)

# Profile
declare -A SPRING_PROFILE=(
  ["admin"]="${SPRING_PROFILES_ACTIVE:-prod}"
  ["api"]="${SPRING_PROFILES_ACTIVE:-prod}"
  ["gateway"]="${SPRING_PROFILES_ACTIVE:-prod}"
)

# Java
JAVA_BIN="${JAVA_BIN:-java}"
JAVA_OPTS_COMMON="${JAVA_OPTS_COMMON:- -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul -XX:+UseG1GC }"
declare -A JAVA_OPTS_APP=(
  ["admin"]=""
  ["api"]=""
  ["gateway"]=""
)

# 타임아웃
START_TIMEOUT_SEC="${START_TIMEOUT_SEC:-45}"
STOP_TIMEOUT_SEC="${STOP_TIMEOUT_SEC:-25}"

# 포트 점유 프로세스 처리 정책
# - "ask"   : 포트를 점유한 프로세스가 있으면 사용자에게 종료 여부를 물어봄
# - "auto"  : 포트를 점유한 PID가 발견되면 자동으로 종료 처리
# - "never" : 포트가 사용 중이면 스크립트 에러로 종료
PORT_TAKEOVER_MODE="${PORT_TAKEOVER_MODE:-ask}"

# Nginx upstream 전환(옵션)
NGINX_UPSTREAM_DIR="${NGINX_UPSTREAM_DIR:-/etc/nginx/conf.d/upstreams}"
NGINX_RELOAD_CMD="${NGINX_RELOAD_CMD:-systemctl reload nginx}"
UPSTREAM_NAME_SUFFIX="_upstream"
