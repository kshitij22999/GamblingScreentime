#!/usr/bin/env bash
# Starts the Metro bundler with Node 18.
set -euo pipefail
source "$(dirname "$0")/env.sh"
cd "$APP_DIR"
exec npx react-native start "$@"
