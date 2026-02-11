#!/bin/sh
set -eu

: "${PUID:=1000}"
: "${PGID:=1000}"
: "${SPRING_PROFILES_ACTIVE:=prod}"

case "$PUID" in
  ''|*[!0-9]*)
    echo "Invalid PUID: $PUID" >&2
    exit 1
    ;;
esac

case "$PGID" in
  ''|*[!0-9]*)
    echo "Invalid PGID: $PGID" >&2
    exit 1
    ;;
esac

if [ "$(id -u)" -ne 0 ]; then
  exec "$@"
fi

groupmod -o -g "$PGID" appgroup
usermod -o -u "$PUID" -g "$PGID" appuser
chown -R appuser:appgroup /app

if command -v runuser >/dev/null 2>&1; then
  exec runuser -u appuser -- "$@"
fi

exec su -s /bin/sh appuser -c 'exec "$@"' -- "$@"
