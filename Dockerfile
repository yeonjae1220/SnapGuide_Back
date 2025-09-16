# 1. 빌드를 위한 베이스 이미지 (JDK 포함)
FROM openjdk:17-jdk-slim AS builder

#RUN apt-get update && apt-get install -y imagemagick

# 작업 디렉토리 설정
WORKDIR /build

# 빌드에 필요한 파일들만 먼저 복사하여 불필요한 재빌드 방지
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .
COPY gradle ./gradle
RUN ./gradlew dependencies

# 소스코드 전체 복사
COPY . .

# Gradle을 사용하여 애플리케이션 빌드
RUN ./gradlew build -x test


# 2. 최종 실행을 위한 베이스 이미지 (JRE 만으로 경량화)
#FROM openjdk:17-jre-slim
FROM openjdk:17-slim

# ▼▼▼▼▼▼▼▼▼▼ ImageMagick 설치 구문을 여기에 추가/이동 ▼▼▼▼▼▼▼▼▼▼
# 루트 사용자로 전환하여 패키지 설치
USER root
RUN apt-get update && apt-get install -y imagemagick && rm -rf /var/lib/apt/lists/*
# 다시 일반 사용자로 전환 (이전에 추가했던 보안 설정)
#USER appuser
# ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

# 작업 디렉토리 설정
WORKDIR /app

# 이 두개는 우선 작동 확인 후 추가 해보기
# 1. nobody 그룹에 appuser 라는 이름의 사용자를 추가합니다.
#RUN addgroup --system nobody && adduser --system --ingroup nobody appuser
# 2. 앞으로 이 컨테이너는 appuser 권한으로 실행되도록 설정합니다.
#USER appuser

# 빌드 단계에서 생성된 jar 파일을 복사
COPY --from=builder /build/build/libs/*.jar app.jar

# 컨테이너 실행 시 실행될 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]