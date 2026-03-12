#!/usr/bin/env bash
set -euo pipefail

ts() { date "+%Y-%m-%d %H:%M:%S"; }

log_info()  { echo "[$(ts)] [INFO]  $*"; }
log_warn()  { echo "[$(ts)] [WARN]  $*"; }
log_error() { echo "[$(ts)] [ERROR] $*"; }

die() { log_error "$*"; exit 1; }
