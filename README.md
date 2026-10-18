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

## OpenSeadragon / raw IIIF through Gateway

OpenSeadragon should never use the internal Cantaloupe address. Use only:

```text
http://localhost:8080/iiif/3/7d1e0c10-0001-4a00-8000-000000000001/info.json
```

The Gateway proxies `/iiif/**` to private Cantaloupe. Later, authentication can be enabled at the Gateway without changing Cantaloupe or OpenSeadragon URL structure.

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
