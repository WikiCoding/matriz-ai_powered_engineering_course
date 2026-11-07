#!/usr/bin/env bash
set -euo pipefail

repository=${1:?Missing owner/repository}
backend_release=${2:?Missing backend commit SHA}
frontend_release=${3:?Missing frontend commit SHA}
[[ "$repository" =~ ^[a-zA-Z0-9_.-]+/[a-zA-Z0-9_.-]+$ ]] || exit 2
[[ "$backend_release" =~ ^[0-9a-f]{40}$ && "$frontend_release" =~ ^[0-9a-f]{40}$ ]] || exit 2

# Installed beside docker-compose.yml in the root login home directory.
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
[[ -f docker-compose.yml ]] || { echo "Missing docker-compose.yml beside deployment script" >&2; exit 2; }
command -v sed >/dev/null
command -v grep >/dev/null
docker compose version >/dev/null
docker compose -f docker-compose.yml config --quiet

# Match only the two tagged GHCR images, preserving quotes and other settings.
image_pattern=$(printf '%s' "ghcr.io/${repository,,}" | sed 's/[.]/\\./g')
backend_pattern="(^[[:space:]]*image:[[:space:]]*[\"']?${image_pattern}-backend:)[^\"'[:space:]]+"
frontend_pattern="(^[[:space:]]*image:[[:space:]]*[\"']?${image_pattern}-frontend:)[^\"'[:space:]]+"
[[ $(grep -Ec "$backend_pattern" docker-compose.yml) == 1 ]] || { echo "Expected one tagged backend image" >&2; exit 2; }
[[ $(grep -Ec "$frontend_pattern" docker-compose.yml) == 1 ]] || { echo "Expected one tagged frontend image" >&2; exit 2; }

docker compose -f docker-compose.yml down
sed -E -i \
  -e "s|${backend_pattern}|\1${backend_release}|" \
  -e "s|${frontend_pattern}|\1${frontend_release}|" \
  docker-compose.yml
docker compose -f docker-compose.yml up -d
