FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY artwork-service/pom.xml artwork-service/pom.xml
COPY artwork-service/src artwork-service/src
RUN apt-get update && apt-get install -y --no-install-recommends maven libvips-tools && rm -rf /var/lib/apt/lists/*     && mvn -q -pl artwork-service -am package

FROM eclipse-temurin:25-jre
RUN apt-get update && apt-get install -y --no-install-recommends libvips-tools \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/artwork-service/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
