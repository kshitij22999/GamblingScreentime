# Shared toolchain setup for the run scripts (sourced, not executed).
export NODE_BIN="${NODE_BIN:-$HOME/.nvm/versions/node/v18.20.4/bin}"
export JAVA_HOME="${JAVA_HOME_17:-/usr/lib/jvm/java-17-openjdk-amd64}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$NODE_BIN:$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
