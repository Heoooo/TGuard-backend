## TGuard Backend – 다중 테넌트 실시간 금융 이상징후 감지 플랫폼

> Slack, Kafka, Redis, Twilio를 통합해 실시간 결제 이상 징후를 탐지하고, 테넌트별 고객에게 즉시 알림을 보내는 Spring Boot 3 기반 백엔드입니다.

---

### 목차
1. [프로젝트 개요](#프로젝트-개요)
2. [문제 정의와 목표](#문제-정의와-목표)
3. [핵심 기능](#핵심-기능)
4. [아키텍처](#아키텍처)
5. [기술 스택과 채택 이유](#기술-스택과-채택-이유)
6. [환경 구성](#환경-구성)
7. [빌드 & 테스트](#빌드--테스트)
8. [성능/부하 테스트](#성능부하-테스트)
9. [멀티 테넌시 검증](#멀티-테넌시-검증)
10. [운영 & 관측성](#운영--관측성)
11. [향후 개선 로드맵](#향후-개선-로드맵)

---

### 프로젝트 개요
- **이름**: TGuard Backend  
- **도메인**: SaaS형 금융 거래 이상징후 탐지 및 경보 시스템  
- **주요 고객**: 카드/페이먼트 서비스를 운영하는 여러 테넌트(입점사)
- **핵심 가치**: 실시간 거래 스트림을 분석해 잠재적 사기를 탐지하고, 고객 및 운영 담당자에게 즉시 알림을 제공

---

### 문제 정의와 목표
| 문제점 | 해결 전략 |
| --- | --- |
| 실시간 결제 사기를 놓치면 금전/신뢰 피해 발생 | Kafka 기반 실시간 파이프라인과 Redis 캐시로 즉시 분석 |
| 테넌트별 데이터가 섞여 보안 리스크 | 요청-스레드 단위 테넌트 컨텍스트, DB 스키마 제약, 테스트로 검증 |
| 반복 실패 이벤트로 인한 백오피스 장애 | DLQ + 재시도 서비스 + Slack 경고로 복원력 확보 |
| 운영자는 상황 파악이 어렵고 대응이 느림 | Slack/Twilio 알림, Micrometer 지표, Pipeline 모니터링 |

---

### 핵심 기능
- **웹훅 수신 & 멱등성 처리**: `/api/webhooks/payments`에서 서명 검증, Redis 기반 멱등성, JSON 파싱 후 내부 이벤트 전환
- **멀티 테넌트 인증/인가**: `X-Tenant-Id` 헤더와 JWT 조합, `TenantContextFilter`로 격리
- **실시간 탐지 엔진**: Kafka → `TransactionEventConsumer` → 룰 기반(`DetectionResultService`) 분석, Redis로 행동 패턴 추적
- **알림 시스템**: Slack(운영 경보), Twilio SMS(고객 경보), Notification 엔티티로 이력 관리
- **배치 & DLQ 관리**: 실패 이벤트는 `DlqTransactionRetry`에 축적, 재시도 스케줄러와 경고 임계치(`warnAttempts`)로 운영 대응
- **REST API**: 카드/거래/규칙 CRUD, 헬스 체크, 인증 API 등

---

### 아키텍처
```
[External Payment Gateway]
        │  (웹훅 + HMAC 서명)
        ▼
[PaymentWebhookController] ── Redis 멱등성 ──▶ [TransactionService.recordFromWebhook]
        │                                        │
        │                                        ├─ DAO (PostgreSQL)
        │                                        └─ Kafka TransactionEvent
        ▼
[Kafka topics] ─▶ Realtime consumer ─▶ DetectionResultService ─▶ Slack/Twilio
                  (Spring Kafka, DLQ)

배치 파이프라인: Kafka Batch Topic → Batch 저장소 → 분석/보고
DLQ 파이프라인 : 실패 이벤트 → DlqRetryService 재시도 → Slack 경고
```

- 주요 흐름 시퀀스는 `docs/perf-testing.md`, `TransactionFlowIntegrationTest`에 일부 캡처됨
- 멀티 테넌트 컨텍스트는 요청 단위 ThreadLocal → JPA 쿼리 조건으로 반영

---

### 기술 스택과 채택 이유
| 영역 | 선택 기술 | 이유 |
| --- | --- | --- |
| 백엔드 프레임워크 | Spring Boot 3.5 + Spring MVC | Java 17 기반, 빈 구성 단순화, 대규모 생태계 |
| 보안 | Spring Security + JWT | 무상태 인증, 멀티 테넌트 헤더 필터, 확장성 |
| 데이터베이스 | PostgreSQL + Spring Data JPA | 멀티 테넌트 제약, 트랜잭션 관리, 풍부한 SQL 기능 |
| 메시징 | Apache Kafka | 실시간/배치 분리, 확장/보관, DLQ 전략 |
| 캐시 | Redis | 행동 패턴(장소/기기/속도) 추적, 멱등성 저장소 |
| 배치/재시도 | Spring Scheduling + DLQ 엔티티 | 2차 복구 프로세스 구현 및 알림 |
| 알림 | Slack Webhook, Twilio SMS | 운영자/고객에게 즉시 통지 |
| 테스트 | JUnit 5, Spring Test, Mockito, H2, k6 | 통합/슬라이스 테스트와 부하 측정 |

---

### 환경 구성
1. **필수 도구**
   - JDK 17
   - Gradle Wrapper(`./gradlew`)
   - Docker & Docker Compose (Postgres, Kafka, Redis 실행용)

2. **실행 전 준비**
   - `docker-compose up -d postgres kafka zookeeper`  
   - 환경 변수 또는 `application-local.yml` 등에 아래 값 설정  
     ```
     WEBHOOK_SECRET=...
     SLACK_WEBHOOK_URL=...
     TWILIO_ACCOUNT_SID=...
     TWILIO_AUTH_TOKEN=...
     TWILIO_FROM_PHONE=...
     JWT_SECRET=...
     ```
   - 개발/테스트용으로는 `application-example.yml`의 구조를 참고해 `.env` 혹은 `application-local.yml` 작성

3. **애플리케이션 실행**
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```

---

### 빌드 & 테스트
```bash
# 정적 분석 및 빌드
./gradlew clean build

# 단위/통합 테스트
./gradlew test
```

주요 테스트 시나리오:
- `TenantIsolationIntegrationTest`: 테넌트 헤더가 다르면 데이터 접근이 차단되는지 검증
- `TenantContextFilterTest`: 요청 lifecycle에서 컨텍스트 설정/정리 확인
- `TransactionFlowIntegrationTest`: 웹훅 → 탐지 → 알림 e2e 플로우
- `DlqRetryServiceTest`: DLQ 경고 임계치와 최대 시도 알림 로직 검증

---

### 성능/부하 테스트
- `scripts/perf/webhook-load-test.js`: k6 기반 부하 스크립트
  ```bash
  k6 run \
    -e BASE_URL=http://localhost:8080 \
    -e WEBHOOK_SECRET=local-secret \
    scripts/perf/webhook-load-test.js
  ```
- 자세한 설정과 시나리오: `docs/perf-testing.md` 참고
- Micrometer 지표와 연동하면 Kafka lag, DLQ 재시도율을 함께 모니터링 가능

---

### 멀티 테넌시 검증
- `TenantContextFilter`가 헤더를 읽어 `TenantContextHolder`에 저장 → 요청 종료 시 `finally`에서 `clear()`
- 테넌트별 유니크 제약 (`users`, `cards`, `transactions` 테이블)으로 DB 레벨 보호
- 테스트
  - `TenantIsolationIntegrationTest`: 서로 다른 `X-Tenant-Id`로 동일 카드 조회 시 404
  - `TenantContextFilterTest`: 기본 테넌트 fallback, 요청 종료 후 컨텍스트 클리어
- 향후: Flyway 마이그레이션과 Row-Level Security(RLS) 적용 검토

---

### 운영 & 관측성
- `PipelineMonitoringService`: 배치 큐, DLQ, 최근 처리량을 5분 간격으로 분석
- Slack 알림
  - DLQ 경고(`warnAttempts`)와 최대 시도 실패 시 `:rotating_light:` 알림
  - 배치 큐 지연/회복 메시지
- Twilio SMS
  - 고객에게 외부 이상 징후 내용 + 금액/시간/위치 전달
- 향후 계획
  - Micrometer + Prometheus + Grafana 대시보드
  - OpenTelemetry Trace 도입으로 웹훅부터 알림까지 end-to-end 추적

---

### 향후 개선 로드맵
1. **시큐리티 강화**
   - 웹훅 레이트리밋, 감사 로그 추가
   - 프로덕션 프로필에서 Redis 기반 멱등 스토어 적용 (현재는 개발 프로필 우선)
2. **데이터 정합성**
   - `Transaction` 금액을 `BigDecimal`로 전환, 통화 단위 컬럼 분리
   - 규칙 엔진 DSL화 및 관리 콘솔 연동
3. **인프라 자동화**
   - GitHub Actions CI, Terraform 인프라 구성
   - Helm Chart 기반 Kubernetes 배포
4. **문서 & 데모**
   - ERD, 시퀀스 다이어그램, Swagger 문서화
   - Postman/Insomnia 컬렉션 제공

---

### 문의
- Maintainer: jinhy (jinhy@example.com)  
- 개선 제안 및 이슈는 GitHub Issues를 통해 등록해주세요.

---

> 복잡한 결제 파이프라인에서 실시간 사기 방어가 어떻게 구현되는지, TGuard Backend의 소스와 테스트, 부하 시나리오를 통해 직접 확인해보세요.
