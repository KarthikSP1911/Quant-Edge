#!/usr/bin/env bash
# Starts the backend (Spring Boot) and frontend (Next.js) dev servers, each in
# its own visible terminal window.
#
# Backend env vars are written into a generated launcher script rather than
# `source`-d directly, since values like DATABASE_URL contain unescaped `&`,
# which a plain `source`/`.` would parse as shell job control and truncate.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT_DIR/.run"
mkdir -p "$RUN_DIR"

BACKEND_PORT=8080
FRONTEND_PORT=3000

port_in_use() {
  (echo > "/dev/tcp/127.0.0.1/$1") >/dev/null 2>&1
}

# Opens a new visible console window running the given script.
open_window() {
  local title="$1" script="$2"
  cmd.exe /c start "$title" bash "$(cygpath -w "$script" 2>/dev/null || echo "$script")"
}

write_env_exports() {
  local env_file="$1"
  [ -f "$env_file" ] || return 0
  while IFS='=' read -r key value; do
    case "$key" in
      ''|'#'*) continue ;;
    esac
    printf 'export %s=%q\n' "$key" "$value"
  done < "$env_file"
}

# --- Backend -----------------------------------------------------------------
if port_in_use "$BACKEND_PORT"; then
  echo "Backend: port $BACKEND_PORT already in use — assuming it's already running, skipping."
else
  BACKEND_LAUNCHER="$RUN_DIR/backend_launch.sh"
  {
    echo "#!/usr/bin/env bash"
    echo "cd '$ROOT_DIR/backend'"
    write_env_exports "$ROOT_DIR/.env"
    write_env_exports "$ROOT_DIR/backend/.env"
    echo "./mvnw spring-boot:run"
    echo "echo"
    echo "read -p 'Backend exited. Press Enter to close.'"
  } > "$BACKEND_LAUNCHER"
  echo "Backend: opening terminal window on :$BACKEND_PORT"
  open_window "QuantEdge Backend" "$BACKEND_LAUNCHER"
fi

# --- Frontend ------------------------------------------------------------------
if port_in_use "$FRONTEND_PORT"; then
  echo "Frontend: port $FRONTEND_PORT already in use — assuming it's already running, skipping."
else
  FRONTEND_LAUNCHER="$RUN_DIR/frontend_launch.sh"
  {
    echo "#!/usr/bin/env bash"
    echo "cd '$ROOT_DIR/frontend'"
    echo "npm run dev"
    echo "echo"
    echo "read -p 'Frontend exited. Press Enter to close.'"
  } > "$FRONTEND_LAUNCHER"
  echo "Frontend: opening terminal window on :$FRONTEND_PORT"
  open_window "QuantEdge Frontend" "$FRONTEND_LAUNCHER"
fi
