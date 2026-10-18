#!/bin/sh
set -eu

MASTERS=collection-masters
PREVIEWS=collection-previews

until mc alias set local http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null 2>&1; do
  echo "Waiting for MinIO..."
  sleep 2
done

mc mb --ignore-existing "local/$MASTERS"
mc mb --ignore-existing "local/$PREVIEWS"

put() {
  if mc stat "$2" >/dev/null 2>&1 </dev/null; then
    echo "exists: $2"
  else
    mc cp "$1" "$2" </dev/null
  fi
}

# Layout: <bucket>/<uuid> (no extension). UUIDs must match artwork.image_id in catalogdb
# (infrastructure/liquibase/changelog/catalog/db.changelog-root.yaml).
# Format: <file base name in /images> <original extension> <uuid>
while read -r base ext uuid; do
  [ -z "$base" ] && continue
  put "/images/$base.$ext" "local/$MASTERS/$uuid"
  put "/images/$base-pre.jpg" "local/$PREVIEWS/$uuid"
done <<'EOF'
photo-1 jpg 7d1e0c10-0001-4a00-8000-000000000001
photo-2 tif 7d1e0c10-0002-4a00-8000-000000000002
photo-3 tif 7d1e0c10-0003-4a00-8000-000000000003
photo-4 tif 7d1e0c10-0004-4a00-8000-000000000004
photo-5 jpg 7d1e0c10-0005-4a00-8000-000000000005
photo-6 jpg 7d1e0c10-0006-4a00-8000-000000000006
photo-7 jpg 7d1e0c10-0007-4a00-8000-000000000007
EOF

echo "MinIO buckets initialized."
