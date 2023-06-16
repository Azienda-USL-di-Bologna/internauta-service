# Dockerfile
# Autore: Giuseppe Russo
# Versione: 1.0.0

# Build next spring data rest framework
FROM maven:3.8.6-openjdk-8 AS next_spring

WORKDIR /home
COPY submodules/next-spring-data-rest-framework ./next-spring-data-rest-framework
RUN cd next-spring-data-rest-framework && mvn clean install -DskipTests

# Build jenesis projection
FROM next_spring AS jenesis_projection

WORKDIR /home
COPY submodules/jenesis-projections-generator ./jenesis-projections-generator
RUN cd jenesis-projections-generator && mvn clean install

# Build utils types
FROM jenesis_projection AS internauta_utils_types

WORKDIR /home
COPY submodules/internauta-utils ./internauta-utils
RUN cd internauta-utils && mvn --projects jpa-tools --also-make clean install

# Build internauta model
FROM internauta_utils_types AS internauta_model

WORKDIR /home
COPY submodules/internauta-model ./internauta-model
RUN cd internauta-model && mvn clean install

# Build internauta utils
FROM internauta_model AS internauta_utils

WORKDIR /home/internauta-utils
RUN mvn clean install -DskipTests

# Build blackbox
FROM internauta_utils AS blackbox_permessi

WORKDIR /home
COPY submodules/blackbox-permessi ./blackbox-permessi
RUN cd blackbox-permessi && mvn clean install

# Build di internauta Service
FROM blackbox_permessi AS internauta_builder

WORKDIR /home
COPY . .
RUN mvn clean install -DskipTests

# Build immagine finale
FROM eclipse-temurin:17-jre-jammy

ARG BITBUCKET_BUILD_NUMBER

ENV BUILD_NUMBER=$BITBUCKET_BUILD_NUMBER 

LABEL maintainer="g.russo@dilaxia.com"
LABEL description="Immagine docker Internauta Service"
LABEL version=$BUILD_NUMBER

RUN echo "Europe/Rome" > /etc/timezone && dpkg-reconfigure -f noninteractive tzdata

WORKDIR /app

COPY --from=internauta_builder /home/target/internauta-service-0.0.1-SNAPSHOT.jar /app/internauta-service-0.0.1-SNAPSHOT.jar

# Esegui l'applicazione quando l'immagine Docker viene avviata
ENTRYPOINT ["java", "-jar", "/app/internauta-service-0.0.1-SNAPSHOT.jar"]