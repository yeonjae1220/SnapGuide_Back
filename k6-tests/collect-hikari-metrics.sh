#!/bin/bash
# HikariCP 메트릭 수집 스크립트
# 사용법: ./collect-hikari-metrics.sh [duration_seconds]

DURATION=${1:-70}  # 기본 70초
OUTPUT_FILE="k6-tests/results/hikari-metrics-$(date +%Y%m%d-%H%M%S).csv"
BASE_URL="http://localhost:8082/actuator/metrics"

echo "timestamp,pending,active,idle,total,acquire_count,acquire_total_time" > "$OUTPUT_FILE"
echo "HikariCP 메트릭 수집 시작... (${DURATION}초 동안)"
echo "결과 파일: $OUTPUT_FILE"
echo ""

END_TIME=$((SECONDS + DURATION))

while [ $SECONDS -lt $END_TIME ]; do
    TIMESTAMP=$(date +%H:%M:%S)

    PENDING=$(curl -s "$BASE_URL/hikaricp.connections.pending" | jq -r '.measurements[0].value // 0')
    ACTIVE=$(curl -s "$BASE_URL/hikaricp.connections.active" | jq -r '.measurements[0].value // 0')
    IDLE=$(curl -s "$BASE_URL/hikaricp.connections.idle" | jq -r '.measurements[0].value // 0')
    TOTAL=$(curl -s "$BASE_URL/hikaricp.connections" | jq -r '.measurements[0].value // 0')
    ACQUIRE_COUNT=$(curl -s "$BASE_URL/hikaricp.connections.acquire" | jq -r '.measurements[0].value // 0')
    ACQUIRE_TOTAL=$(curl -s "$BASE_URL/hikaricp.connections.acquire" | jq -r '.measurements[1].value // 0')

    echo "$TIMESTAMP,$PENDING,$ACTIVE,$IDLE,$TOTAL,$ACQUIRE_COUNT,$ACQUIRE_TOTAL" >> "$OUTPUT_FILE"

    # 콘솔 출력
    if [ "$PENDING" != "0" ] && [ "$PENDING" != "0.0" ]; then
        echo -e "\033[31m$TIMESTAMP | Pending: $PENDING | Active: $ACTIVE | Idle: $IDLE | Total: $TOTAL | ⚠️  SPIN-WAIT 가능성!\033[0m"
    else
        echo "$TIMESTAMP | Pending: $PENDING | Active: $ACTIVE | Idle: $IDLE | Total: $TOTAL"
    fi

    sleep 1
done

echo ""
echo "=== 수집 완료 ==="
echo "결과 파일: $OUTPUT_FILE"

# 요약 출력
MAX_PENDING=$(tail -n +2 "$OUTPUT_FILE" | cut -d',' -f2 | sort -rn | head -1)
MAX_ACTIVE=$(tail -n +2 "$OUTPUT_FILE" | cut -d',' -f3 | sort -rn | head -1)
PENDING_COUNT=$(tail -n +2 "$OUTPUT_FILE" | cut -d',' -f2 | grep -v "^0" | grep -v "^0.0" | wc -l | tr -d ' ')

echo ""
echo "=== 분석 결과 ==="
echo "최대 Pending: $MAX_PENDING"
echo "최대 Active: $MAX_ACTIVE"
echo "Pending > 0 발생 횟수: $PENDING_COUNT"
echo ""

if [ "$PENDING_COUNT" -gt 0 ]; then
    echo -e "\033[31m⚠️  Spin-wait/Lock contention 발생 감지!\033[0m"
    echo "   - Pending이 0보다 큰 시점이 ${PENDING_COUNT}회 있었습니다."
    echo "   - 커넥션 풀 사이즈 증가 또는 쿼리 최적화를 고려하세요."
else
    echo -e "\033[32m✅ Spin-wait 미발생\033[0m"
    echo "   - 테스트 기간 동안 커넥션 대기가 없었습니다."
    echo "   - 현재 풀 사이즈가 적절합니다."
fi
