# 📦 데이터베이스 마이그레이션 가이드

Local 데이터베이스의 위치 데이터를 Docker 환경으로 복사하는 도구입니다.

---

## ✅ 마이그레이션 전 데이터 검증 (권장)

마이그레이션 전에 데이터를 검증하고 정리하는 것을 **강력히 권장**합니다!

### 1단계: 데이터 검증

```bash
./db-migration/validate-data.sh
```

**자동으로 확인하는 항목:**
- ✅ ID 중복 검사
- ✅ 위치명+좌표 중복 검사
- ✅ NULL 값 검사 (location_name, country, city)
- ✅ Geometry 유효성 검사
- ✅ 좌표 범위 검사 (위도: -90~90, 경도: -180~180)
- ✅ 외래 키 참조 검사 (media, guide)

### 2단계: 문제 수정

검증 결과에 따라:

#### A. 중복만 있는 경우:
```bash
./db-migration/clean-duplicates.sh
```
- 동일한 위치명+좌표를 가진 중복 제거
- 가장 오래된 레코드 유지
- 외래 키 자동 업데이트

#### B. 데이터 오류가 있는 경우:
```bash
./db-migration/fix-data-issues.sh
```
- NULL 값 처리
- 잘못된 geometry 삭제
- 좌표 범위 오류 삭제
- 자동 백업 생성

---

## 🚀 빠른 시작 (1분)

### 가장 간단한 방법: 자동 마이그레이션

```bash
# Local → Docker 환경으로 한 번에 마이그레이션
./db-migration/migrate-all.sh docker

# Local → NAS-Docker 환경으로
./db-migration/migrate-all.sh nas-docker

# Local → 모든 Docker 환경으로
./db-migration/migrate-all.sh both
```

**끝!** 이게 전부입니다. 🎉

---

## 📋 필요한 경우

### 상황 1: 단계별로 직접 실행하고 싶어요

#### Step 1: 데이터 백업
```bash
./db-migration/export-location-data.sh
```

**결과**: `db-migration/location_data.sql` 파일 생성

#### Step 2: 데이터 가져오기
```bash
# Docker 환경으로
./db-migration/import-location-data.sh docker

# 또는 NAS-Docker 환경으로
./db-migration/import-location-data.sh nas-docker
```

---

### 상황 2: 백업 파일을 다른 곳에 보관하고 싶어요

```bash
# 백업 파일 생성
./db-migration/export-location-data.sh

# 안전한 곳에 복사
cp db-migration/location_data.sql ~/backups/location_data_$(date +%Y%m%d).sql

# 나중에 필요할 때 복원
cp ~/backups/location_data_20251203.sql db-migration/location_data.sql
./db-migration/import-location-data.sh docker
```

---

### 상황 3: 특정 테이블만 백업/복원하고 싶어요

현재는 `location` 테이블만 지원하지만, 다른 테이블도 추가 가능합니다:

```bash
# 예: member 테이블도 백업하려면
pg_dump -h localhost -p 5432 -U $POSTGRES_USER_LOCAL \
  -d snapguidedb \
  --table=member \
  --data-only \
  --column-inserts \
  > db-migration/member_data.sql
```

---

## ⚙️ 작동 원리

### 1. export-location-data.sh

```
Local PostgreSQL (port 5432)
         ↓
  pg_dump (location 테이블만)
         ↓
  location_data.sql 파일 생성
  (PostGIS geometry 타입 포함)
```

**주요 기능**:
- ✅ PostGIS geometry 타입 보존
- ✅ INSERT 문으로 변환 (호환성 최대화)
- ✅ 소유자/권한 정보 제외 (깔끔한 마이그레이션)

### 2. import-location-data.sh

```
location_data.sql 파일
         ↓
  Docker PostgreSQL (port 5433)
  또는 NAS-Docker PostgreSQL (port 5434)
         ↓
  데이터 삽입 완료
```

**주요 기능**:
- ✅ 기존 데이터 처리 옵션 (삭제/추가)
- ✅ 결과 검증
- ✅ 샘플 데이터 출력

### 3. migrate-all.sh

```
export → import → verify → report
```

**자동으로**:
1. 백업 생성
2. 선택한 환경으로 가져오기
3. 데이터 개수 검증
4. 결과 리포트 출력

---

## 🛠️ 전제 조건

### 1. PostgreSQL 클라이언트 도구 설치

```bash
# macOS
brew install postgresql

# Ubuntu/Debian
sudo apt-get install postgresql-client

# 설치 확인
psql --version
pg_dump --version
```

### 2. .env 파일 설정

`.env` 파일에 다음 변수가 있어야 합니다:

```bash
# Local DB
POSTGRES_HOST_LOCAL=localhost
POSTGRES_PORT_LOCAL=5432
POSTGRES_DB_LOCAL=snapguidedb
POSTGRES_USER_LOCAL=your_user
POSTGRES_PASSWORD_LOCAL=your_password

# Docker DB
POSTGRES_DB=snapguidedb
POSTGRES_USER=your_user
POSTGRES_PASSWORD=your_password
```

### 3. Docker 컨테이너 실행 중

```bash
# Docker Compose 실행
docker-compose up -d

# 상태 확인
docker ps | grep postgres
```

---

## 🔍 문제 해결

### Q1: "psql: command not found" 에러

**A**: PostgreSQL 클라이언트가 설치되지 않았습니다.

```bash
# macOS
brew install postgresql

# Linux
sudo apt-get install postgresql-client
```

---

### Q2: "데이터베이스에 연결할 수 없습니다" 에러

**A**: 다음을 확인하세요:

1. **Local DB**: PostgreSQL이 실행 중인가?
   ```bash
   brew services list | grep postgresql
   # 또는
   pg_isready -h localhost -p 5432
   ```

2. **Docker DB**: 컨테이너가 실행 중인가?
   ```bash
   docker ps | grep postgres
   ```

3. **.env 파일**: DB 정보가 정확한가?
   ```bash
   cat .env | grep POSTGRES
   ```

---

### Q3: "기존 데이터가 있습니다" 선택지가 나옵니다

**A**: 3가지 옵션 중 선택하세요:

1. **기존 데이터 삭제 후 가져오기 (권장)**
   - 깔끔한 마이그레이션
   - Local 데이터와 정확히 일치

2. **기존 데이터 유지하고 추가**
   - 중복 데이터 발생 가능
   - ID 충돌 가능성

3. **취소**
   - 안전하게 중단

**권장**: 옵션 1 선택

---

### Q4: 데이터 개수가 일치하지 않습니다

**A**: 수동으로 확인하세요:

```bash
# Local 데이터 개수
PGPASSWORD=your_password psql -h localhost -p 5432 -U your_user -d snapguidedb -c "SELECT COUNT(*) FROM location;"

# Docker 데이터 개수
PGPASSWORD=your_password psql -h localhost -p 5433 -U your_user -d snapguidedb -c "SELECT COUNT(*) FROM location;"

# 차이가 있다면 로그 확인
docker-compose logs db
```

---

### Q5: PostGIS geometry 타입이 깨집니다

**A**: Docker 이미지가 PostGIS를 포함하는지 확인:

```bash
# docker-compose.yml 확인
grep postgis docker-compose.yml

# 올바른 이미지:
# image: postgis/postgis:15-3.4-alpine
```

---

## 📊 마이그레이션 검증

### 1. 데이터 개수 확인

```bash
# Local
PGPASSWORD=your_password psql -h localhost -p 5432 -U your_user -d snapguidedb -c "SELECT COUNT(*) FROM location;"

# Docker
PGPASSWORD=your_password psql -h localhost -p 5433 -U your_user -d snapguidedb -c "SELECT COUNT(*) FROM location;"
```

### 2. 샘플 데이터 확인

```bash
PGPASSWORD=your_password psql -h localhost -p 5433 -U your_user -d snapguidedb -c "
SELECT
    id,
    location_name,
    country,
    city,
    ST_AsText(coordinate) as coordinate
FROM location
LIMIT 5;
"
```

### 3. Geometry 데이터 확인

```bash
PGPASSWORD=your_password psql -h localhost -p 5433 -U your_user -d snapguidedb -c "
SELECT
    COUNT(*) as total,
    COUNT(coordinate) as with_coordinate,
    ST_GeometryType(coordinate) as geom_type
FROM location
GROUP BY geom_type;
"
```

---

## 🎯 실전 시나리오

### 시나리오 1: 데이터 검증 후 마이그레이션 (권장)

```bash
# 1. 데이터 검증
./db-migration/validate-data.sh

# 2. 문제가 발견되면 수정
# - 중복만 있는 경우:
./db-migration/clean-duplicates.sh

# - 데이터 오류가 있는 경우:
./db-migration/fix-data-issues.sh

# 3. 재검증
./db-migration/validate-data.sh

# 4. 마이그레이션 실행
./db-migration/migrate-all.sh docker
```

---

### 시나리오 2: 새 Docker 환경 설정

```bash
# 1. Docker Compose 시작
docker-compose up -d

# 2. DB 초기화 대기 (약 10초)
sleep 10

# 3. 데이터 마이그레이션
./db-migration/migrate-all.sh docker

# 4. 애플리케이션 재시작
docker-compose restart backend

# 5. 확인
curl http://localhost:8082/api/locations | jq
```

---

### 시나리오 3: 정기 백업

```bash
#!/bin/bash
# backup-cron.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=~/snapguide-backups

mkdir -p $BACKUP_DIR

# 백업 실행
./db-migration/export-location-data.sh

# 날짜별 보관
cp db-migration/location_data.sql \
   $BACKUP_DIR/location_data_${DATE}.sql

# 30일 이상 된 백업 삭제
find $BACKUP_DIR -name "location_data_*.sql" -mtime +30 -delete

echo "백업 완료: ${DATE}"
```

crontab 설정:
```bash
# 매일 새벽 3시 백업
0 3 * * * /path/to/snapguide/backup-cron.sh
```

---

### 시나리오 4: 개발 → 스테이징 → 프로덕션

```bash
# 1. 개발 DB에서 백업
./db-migration/export-location-data.sh

# 2. 백업 파일을 스테이징 서버로 복사
scp db-migration/location_data.sql staging-server:~/

# 3. 스테이징 서버에서 복원
ssh staging-server
cd ~/snapguide
./db-migration/import-location-data.sh docker

# 4. 검증 후 프로덕션 적용
# (동일한 과정 반복)
```

---

## 📚 관련 파일

```
db-migration/
├── README.md                    # 이 문서
├── validate-data.sh             # 데이터 검증 스크립트 ⭐ NEW
├── clean-duplicates.sh          # 중복 데이터 정리 스크립트 ⭐ NEW
├── fix-data-issues.sh           # 데이터 오류 수정 스크립트 ⭐ NEW
├── export-location-data.sh      # 백업 스크립트
├── import-location-data.sh      # 복원 스크립트
├── migrate-all.sh               # 자동 마이그레이션
└── location_data.sql            # 백업 파일 (생성됨)
```

---

## ⚠️ 주의사항

1. **Google Maps API 비용**
   - 이 마이그레이션은 API를 사용하지 않습니다
   - 기존 데이터를 그대로 복사합니다

2. **데이터 일관성**
   - 외래 키 제약 조건 주의
   - `media`, `guide` 테이블과의 관계 확인

3. **프로덕션 환경**
   - 마이그레이션 전 반드시 백업
   - 유지보수 시간대에 실행 권장

4. **대용량 데이터**
   - 10만 개 이상 시 시간이 걸릴 수 있음
   - 필요시 배치 처리 고려

---

## 🎓 추가 학습

- [PostgreSQL pg_dump 문서](https://www.postgresql.org/docs/current/app-pgdump.html)
- [PostGIS 데이터 타입](https://postgis.net/docs/using_postgis_dbmanagement.html)
- [Docker PostgreSQL 설정](https://hub.docker.com/_/postgres)

---

**마이그레이션 성공을 기원합니다! 🚀**
