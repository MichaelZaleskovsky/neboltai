FROM chainguard/minio-client:latest AS mc

FROM debian:stable-slim
COPY --from=mc /usr/bin/mc /usr/local/bin/mc
