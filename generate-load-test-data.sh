#!/bin/bash

# ========================================
# k6 부하테스트 데이터 생성 및 로드 스크립트
# ========================================
# 사용법:
#   ./generate-load-test-data.sh          # 전체 프로세스 실행
#   ./generate-load-test-data.sh csv      # CSV만 생성
#   ./generate-load-test-data.sh load     # CSV를 DB에 로드만
# ========================================

set -e  # 에러 발생 시 중단

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 설정
POSTGRES_CONTAINER="snapguide-postgres-db"
POSTGRES_USER="yeonjae"
POSTGRES_DB="snapguide"
CSV_DIR="src/main/resources/load-test-data"
SQL_FILE="$CSV_DIR/import.sql"

# 함수 정의
print_step() {
    echo -e "\n${BLUE}===================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}===================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# CSV 생성 함수
generate_csv() {
    print_step "Step 1: CSV 파일 생성"

    echo "Gradle task 실행 중..."
    # JAVA_HOME 자동 설정 (Java 17 우선)
    if [ -d "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home"
    fi

    if JAVA_HOME=$JAVA_HOME ./gradlew runDataGenerator --quiet; then
        print_success "CSV 파일 생성 완료"
        echo ""
        ls -lh $CSV_DIR/*.csv 2>/dev/null | awk '{print "  📄 " $9 " (" $5 ")"}'
    else
        print_error "CSV 생성 실패. 아래 명령어로 직접 실행해보세요:"
        echo "  JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home ./gradlew runDataGenerator"
        exit 1
    fi
}

# CSV를 PostgreSQL에 로드하는 함수
load_to_db() {
    print_step "Step 2: PostgreSQL에 데이터 로드"

    # Docker 컨테이너 확인
    if ! docker ps | grep -q $POSTGRES_CONTAINER; then
        print_error "PostgreSQL 컨테이너가 실행 중이지 않습니다."
        echo "docker-compose up -d 를 실행하세요."
        exit 1
    fi

    print_success "PostgreSQL 컨테이너 확인 완료"

    # CSV 파일을 컨테이너로 복사
    echo "CSV 파일을 컨테이너로 복사 중..."
    docker cp $CSV_DIR/*.csv $POSTGRES_CONTAINER:/tmp/
    print_success "CSV 파일 복사 완료"

    # import.sql 실행
    echo "import.sql 실행 중..."
    docker exec -i $POSTGRES_CONTAINER psql -U $POSTGRES_USER -d $POSTGRES_DB < $SQL_FILE

    print_success "데이터 로드 완료"

    # 데이터 확인
    echo -e "\n📊 로드된 데이터 통계:"
    docker exec -it $POSTGRES_CONTAINER psql -U $POSTGRES_USER -d $POSTGRES_DB -c "
        SELECT '회원' AS 항목, COUNT(*) AS 개수 FROM member WHERE email LIKE 'loadtest%'
        UNION ALL
        SELECT '가이드', COUNT(*) FROM guide
        UNION ALL
        SELECT '미디어', COUNT(*) FROM media
        UNION ALL
        SELECT '댓글', COUNT(*) FROM comment;
    "
}

# Location ID 추출 함수
export_location_ids() {
    print_step "Step 0: Location ID 추출 (선택사항)"

    # Docker 컨테이너 확인
    if ! docker ps | grep -q $POSTGRES_CONTAINER; then
        print_warning "PostgreSQL 컨테이너가 실행 중이지 않습니다."
        print_warning "기본값(1~1000)을 사용합니다."
        return
    fi

    echo "DB에서 Location ID 추출 중..."

    # SQL 파일을 컨테이너로 복사
    docker cp $CSV_DIR/export-location-ids.sql $POSTGRES_CONTAINER:/tmp/

    # SQL 실행
    docker exec -it $POSTGRES_CONTAINER psql -U $POSTGRES_USER -d $POSTGRES_DB \
        -f /tmp/export-location-ids.sql

    # location_ids.csv를 로컬로 복사
    docker cp $POSTGRES_CONTAINER:/tmp/location_ids.csv $CSV_DIR/

    print_success "Location ID 추출 완료: $CSV_DIR/location_ids.csv"
}

# 메인 로직
main() {
    local mode=${1:-all}

    case $mode in
        csv)
            generate_csv
            ;;
        load)
            load_to_db
            ;;
        all)
            export_location_ids  # Location ID 먼저 추출
            generate_csv
            load_to_db
            print_step "완료!"
            echo "다음 단계:"
            echo "  1. k6 설치: brew install k6 (macOS)"
            echo "  2. k6 테스트 실행: k6 run k6/scripts/load-test-guide-api.js"
            ;;
        export-locations)
            export_location_ids
            ;;
        *)
            print_error "사용법: $0 [csv|load|all|export-locations]"
            exit 1
            ;;
    esac
}

# 스크립트 실행
main "$@"
