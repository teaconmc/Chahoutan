# Stage 1: build jar

FROM alpine:3

RUN apk add --no-cache openjdk17 bash

COPY ./ ./
RUN ./gradlew build --info --stacktrace

RUN find build/libs/ \
    -regex "build/libs/Chahoutan-\(0\|[1-9][0-9]*\)\.\(0\|[1-9][0-9]*\)\.\(0\|[1-9][0-9]*\).jar" \
    -exec cp {} /tmp/chahoutan.jar \;

# Stage 1: build final image

FROM alpine:3

RUN apk add --no-cache tzdata openjdk17 bash
RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo Asia/Shanghai >/etc/timezone

RUN mkdir -p /app/data
COPY --from=0 /tmp/chahoutan.jar /app/chahoutan.jar

VOLUME /app/data
WORKDIR /app/data

ENTRYPOINT ["java", "-Xmx512M", "-jar", "/app/chahoutan.jar"]
