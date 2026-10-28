# Neboltai backend – high-resolution collection prototype

Microservice prototype for a collection of very large images.

## Architecture

```text
Client / OpenSeadragon
        |
        | public HTTP :8080
        v
+-----------------------+
| Reactive API Gateway  |
| Spring Cloud Gateway  |
+----+-------------+----+
     |             |
     |             +---------------------> Cantaloupe (PRIVATE)
     |                                       |
     v                                       v
Artwork Service                         MinIO (PRIVATE)
     |      \
PostgreSQL   +---> MinIO previews (PRIVATE)
```

Only `api-gateway` publishes a host port. MinIO and Cantaloupe are isolated from the host and Internet by Docker networks.

## Java / Spring versions

This edition uses only stable, production-ready releases:

- **Java 25 LTS**
- **Spring Boot 4.1.1**
- **Spring Cloud 2025.1.3 (Oakwood)**

Spring Cloud 2025.1.x is the stable release train compatible with Spring Boot 4.1.x. No Spring milestone repositories are required by this project.

## Main GET access points

### 1. Search artworks (paged)

```http
GET http://localhost:8080/api/artworks/search?authorId=2&yearFrom=2018&page=0&size=20
```

Filters: `authorId`, `mediaType`, `mediaName`, `yearFrom`, `yearTo`, `page`, `size` (max 100). The response is `{items, page, size, totalElements, totalPages}`; each item has the artwork metadata (`authorId`, `authorName`, `mediaId`, `mediaName`, `mediaTypeId`, `mediaType`, `width`, `height`) plus `previewUrl`.

### Reference data

- `GET /api/authors` — all authors as `[{id, author}]`, sorted by full name (family name, first name, second name; empty parts omitted).
- `GET /api/medias` — all media as `[{id, name}]`, sorted by name.
- `GET /api/mediatypes` — all media types as `[{id, type}]`, sorted by type.

These endpoints return arrays without pagination.

### 2. Preview JPEG

```http
GET http://localhost:8080/api/artworks/preview/7d1e0c10-0001-4a00-8000-000000000001
```

Use `previewUrl` from the search result as `<img src>`.

### 3. Crop with parameters

```http
GET http://localhost:8080/api/images/7d1e0c10-0004-4a00-8000-000000000004/crop?x=200&y=300&width=1200&height=900&targetWidth=600
```

The Gateway converts this to an internal IIIF request to Cantaloupe and streams the JPEG back to the client.

## Upload an image

`POST /api/artworks/addImage` accepts a multipart file part named `file`:

```sh
curl -F "file=@photo.tif" http://localhost:8080/api/artworks/addImage
```

JPEG and TIFF are detected from the file contents. The original is stored unchanged
in `collection-masters`, and a JPEG preview of width 300 pixels (height proportional
to the original) is stored in `collection-previews`. Both object keys are the same
generated UUID, without an extension. TIFF previews use the first page.
The response is `201 Created` with `imageId`, `fileName`, `width`, `height`,
`previewWidth`, `previewHeight` and `previewUrl`. Pass `imageId` and the metadata
to `/api/artworks/addMediaData` to create the database record.

Uploads default to 2 GB per file (2049 MB per multipart request). Configure artwork-service with
`IMAGE_UPLOAD_MAX_FILE_SIZE` and `IMAGE_UPLOAD_MAX_REQUEST_SIZE` to change the limit
(the request limit must also allow multipart overhead). Empty uploads return 400,
unsupported image formats return 415, and storage failures return 503.
If an upload fails, the service attempts to remove both objects.

## Delete an image

```sh
curl -X DELETE http://localhost:8080/api/artworks/deleteImage/IMAGE_UUID
```

Returns `204 No Content` after deleting the master, preview and artwork metadata.
Author and media reference data remain available. Missing objects and metadata are
ignored, so repeated requests are safe. A MinIO failure returns 503 and prevents
metadata deletion. MinIO and PostgreSQL do not share a transaction: a partial
failure can leave objects deleted while metadata remains; repeat the request to
complete deletion. Existing browser and Cantaloupe caches are not purged.

## OpenSeadragon / raw IIIF through Gateway

OpenSeadragon should never use the internal Cantaloupe address. Use only:

```text
http://localhost:8080/iiif/3/7d1e0c10-0001-4a00-8000-000000000001/info.json
```

The Gateway proxies `/iiif/**` to private Cantaloupe. Later, authentication can be enabled at the Gateway without changing Cantaloupe or OpenSeadragon URL structure.

The `islandora/cantaloupe` image generates `/opt/cantaloupe/cantaloupe.properties`
from the `CANTALOUPE_*` variables in Compose. The reference file under
`infrastructure/cantaloupe/` is not mounted or loaded by this image. Configure
S3 credentials through `MINIO_ROOT_USER` and `MINIO_ROOT_PASSWORD`, and set
`PUBLIC_BASE_URL` to the externally reachable Gateway URL for IIIF tile links.
Production `CANTALOUPE_IMAGE` must support the same Islandora environment variables.
After changing settings, run `docker compose up -d --no-deps cantaloupe`.

Compose enables `CANTALOUPE_PROCESSOR_DOWNSCALE_LINEAR=true` to convert images to
linear RGB before resampling. This avoids a Cantaloupe 5.0.7 `ResampleOp`
`ArrayIndexOutOfBoundsException` observed with grayscale TIFF crops and tiles.
The original in MinIO is unchanged. Color conversion adds processing work on cache misses.

Cantaloupe defaults to a 4 GB maximum heap (`CANTALOUPE_HEAP_MAX` can override it).
Its HTTP thread pool is bounded to 16 threads with a 128-entry accept queue to
keep the server bounded. Gateway uses fixed connection pools of 2 connections for
IIIF tiles and 2 for crop requests, queuing acquisition for up to 300 seconds. This
limits simultaneous decodes to 4 across both entry points and avoids the heap exhaustion observed
when loading many uncached tiles of a 20000 x 10000 TIFF at once. Requests may wait
while workers are busy; queue saturation may return 503. Allow memory for the JVM
outside the heap and for the other services when sizing the Docker host.

## Authentication status

Development mode intentionally has **no authentication**.

`DevSecurityConfig` allows all requests.

A future JWT Resource Server configuration is already prepared in:

- `SecureSecurityConfig.java`
- `application-secure.yml`

Later you can run the Gateway with profile `secure` and connect Keycloak, Spring Authorization Server, Auth0, etc. All `/api/**` and `/iiif/**` endpoints will then require a valid JWT.

## Running everything with Docker

Requirements:

- Docker Desktop
- Docker Compose
- Internet access for the first image/dependency download

From the project root:

```bash
docker compose up --build
```

Wait until all services start.

Then test:

```text
http://localhost:8080/api/artworks/search
http://localhost:8080/iiif/3/7d1e0c10-0001-4a00-8000-000000000001/info.json
```

Crop example:

```text
http://localhost:8080/api/images/7d1e0c10-0004-4a00-8000-000000000004/crop?x=100&y=100&width=1000&height=1000&targetWidth=500
```

Stop:

```bash
docker compose down
```

Delete development data as well:

```bash
docker compose down -v
```

## Opening in IntelliJ IDEA

1. Install a JDK 25 LTS distribution and IntelliJ IDEA.
2. Unzip this project.
3. In IntelliJ choose **File -> Open**.
4. Select the root folder `neboltai-backend` (or select its root `pom.xml`).
5. Choose **Open as Project** / **Trust Project** if IntelliJ asks.
6. IntelliJ should recognize the root `pom.xml` as a Maven multi-module project.
7. Open **File -> Project Structure -> Project** and set **SDK = JDK 25**.
8. Open **Settings -> Build, Execution, Deployment -> Build Tools -> Maven**. You can use IntelliJ's bundled Maven; no separate Maven installation is required.
9. In the Maven tool window click **Reload All Maven Projects**.
10. Enable annotation processing for Lombok if IDEA does not do it automatically: **Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors -> Enable annotation processing**.
11. Install the Lombok plugin in IntelliJ if it is not already enabled.

For the simplest first run, use Docker Compose from IntelliJ's Terminal:

```bash
docker compose up --build
```

The only public backend port is `8080`.

## Running Spring services from IDEA instead of containers

You can run `ApiGatewayApplication` and `ArtworkServiceApplication` directly from IDEA, but then PostgreSQL, MinIO, and Cantaloupe must still be available. For a first run, Docker Compose is easier because it wires the internal DNS names and private networks automatically.

## Swagger

Swagger lives only in api-gateway and documents only the public endpoints (`/api/artworks/search`, `/api/artworks/preview/{imageId}`, `/api/images/{identifier}/crop`):

- UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

artwork-service has no Swagger. In the `secure` profile these paths are open without a token.

## Project structure

```text
neboltai-backend/
├── pom.xml
├── docker-compose.yml
├── api-gateway/
│   ├── pom.xml
│   └── src/main/...
├── artwork-service/
│   ├── pom.xml
│   └── src/main/...
└── infrastructure/
    ├── cantaloupe/
    ├── minio-init/
    └── liquibase/
```

## Next development steps

The intentionally small prototype is prepared for later additions: upload endpoints and immutable master versioning in artwork-service; real artwork/artist/keyword relations; Authorization Server; per-role authorization; OpenAPI aggregation; tests with Testcontainers; production Cantaloupe derivative cache; observability and rate limiting.
