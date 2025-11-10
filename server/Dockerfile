# Stage 1: Cache Gradle dependencies
FROM gradle:jdk21 AS cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME=/home/gradle/cache_home

COPY *.gradle.kts gradle.properties /home/gradle/app/
COPY server/build.gradle.kts gradle.properties /home/gradle/app/server/
COPY gradle /home/gradle/app/gradle

# Since we have excluded the composeApp source folder, we have to use a modified version of the build.gradle.kts file,
# which doesn't use any Android/Compose dependencies.
RUN mkdir -p /home/gradle/app/shared
RUN touch /home/gradle/app/shared/build.gradle.kts
RUN echo 'plugins {' >> /home/gradle/app/shared/build.gradle.kts
RUN echo '    alias(libs.plugins.kotlinMultiplatform)' >> /home/gradle/app/shared/build.gradle.kts
RUN echo '    alias(libs.plugins.kotlinxSerialization)' >> /home/gradle/app/shared/build.gradle.kts
RUN echo '}' >> /home/gradle/app/shared/build.gradle.kts
RUN echo 'kotlin {' >> /home/gradle/app/shared/build.gradle.kts
RUN echo '    jvm()' >> /home/gradle/app/shared/build.gradle.kts
RUN echo '    sourceSets {' >> /home/gradle/app/shared/build.gradle.kts
RUN echo '        commonMain.dependencies {' >> /home/gradle/app/shared/build.gradle.kts
RUN echo '            implementation(libs.kotlinx.serializationJson)' >> /home/gradle/app/shared/build.gradle.kts
RUN echo '            api(libs.kotlinx.datetime)' >> /home/gradle/app/shared/build.gradle.kts
RUN echo '            implementation(libs.kotlin.crypto.random)' >> /home/gradle/app/shared/build.gradle.kts
RUN echo '            implementation(libs.kotlin.crypto.sha2)' >> /home/gradle/app/shared/build.gradle.kts
RUN echo '        }' >> /home/gradle/app/shared/build.gradle.kts
RUN echo '    }' >> /home/gradle/app/shared/build.gradle.kts
RUN echo '    kotlin {' >> /home/gradle/app/shared/build.gradle.kts
RUN echo '        compilerOptions {' >> /home/gradle/app/shared/build.gradle.kts
RUN echo '            optIn.add("kotlin.uuid.ExperimentalUuidApi")' >> /home/gradle/app/shared/build.gradle.kts
RUN echo '        }' >> /home/gradle/app/shared/build.gradle.kts
RUN echo '    }' >> /home/gradle/app/shared/build.gradle.kts
RUN echo '} ' >> /home/gradle/app/shared/build.gradle.kts

# Since we are not copying the composeApp source folder, we have to remove it from settings.gradle.kts
RUN sed -i '/^include(":composeApp")$/d' /home/gradle/app/settings.gradle.kts

# Create a dummy version.properties file
RUN echo 'VERSION_NAME=0.0.0' >> /home/gradle/app/version.properties

WORKDIR /home/gradle/app
RUN gradle clean build -i --stacktrace

# Stage 2: Build Application
FROM gradle:jdk21 AS build

ARG SENTRY_AUTH_TOKEN

COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY --chown=gradle:gradle . /home/gradle/src

WORKDIR /home/gradle/src

# Build the fat JAR, Gradle also supports shadow and boot JAR by default.
RUN gradle buildFatJar -PSENTRY_AUTH_TOKEN=${SENTRY_AUTH_TOKEN} --no-daemon

# Stage 3: Create the Runtime Image
FROM amazoncorretto:21-alpine-jdk AS runtime
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/server/build/libs/server-all.jar /app/server.jar
ENTRYPOINT ["java","-jar","/app/server.jar"]
