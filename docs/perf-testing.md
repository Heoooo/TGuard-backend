# 성능 및 부하 테스트 가이드

## 개요

웹훅 수신부터 Kafka 파이프라인, 탐지 서비스까지의 처리량과 지연 시간을 점검하기 위해 `k6` 스크립트를 제공한다.  
스クリ�?는 시크릿 서명, 멱등키 헤더, 테넌트 헤더를 포함해 실제 운영 흐름과 최대한 동일한 조건을 재현한다.

## 준비 사항

- Docker 로컬 환경에서 `docker-compose up postgres kafka zookeeper` 실행
- 애플리케이션을 `SPRING_PROFILES_ACTIVE=perf` 등으로 실행하고, `webhook.secret` 값을 `WEBHOOK_SECRET` 환경 변수와 동일하게 맞춘다.
- k6 설치 (https://k6.io/docs/get-started/installation/)

## 실행 방법

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e WEBHOOK_SECRET=test-secret \
  -e RATE=75 \
  -e DURATION=3m \
  scripts/perf/webhook-load-test.js
```

옵션 환경 변수 설명:

- `RATE`: 초당 도착 요청 수(기본 50 rps)
- `DURATION`: steady 시나리오 지속 시간
- `VUS`, `MAX_VUS`: 기본 VU 풀 크기 조정
- `SPIKE_*`: 스파이크 부하 시나리오 파라미터
- `SLEEP`: VU 내부 최소 휴면 시간(초), 디폴트 0.1

## 결과 해석

- `http_req_duration`: 전체 응답 지연. 95p 500ms 미만 유지가 목표.
- `webhook_processing`: 개별 요청 처리 시간 트렌드.
- `webhook_success`: 성공 응답 수 카운터. `http_req_failed` 1% 이하 유지.
- k6 출력 결과를 CSV/JSON으로 저장 후 Prometheus/Micrometer 지표와 비교하면 병목 파악이 쉽다.

## 권장 시나리오

1. **Steady 50 rps / 3분**: 기본 처리량 확인
2. **Spike 200 rps / 30초**: Kafka, Redis, DLQ가 버스트를 흡수하는지 점검
3. **Long soak 30 rps / 30분**: 메모리 누수, 컨슈머 lag 모니터링

## 후속 작업

- Micrometer로 `TransactionEventConsumer` 처리 시간, `DlqRetryService` 재시도 건수를 계측해 k6 결과와 상관 분석
- Grafana 대시보드에 k6 Trend 데이터를 포함시켜 반복 측정 시 비교 가능하도록 구성
