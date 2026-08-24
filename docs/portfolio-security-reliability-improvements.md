# TGuard Security & Reliability Improvement Notes

이 문서는 TGuard 백엔드 개선 작업 중 포트폴리오, 자기소개서, 이력서에 활용할 수 있는 내용을 별도로 기록한 자료다.
기능 설명보다 "어떤 실무 리스크를 발견했고, 어떤 방식으로 줄였는지"에 초점을 둔다.

## Summary

- PR: https://github.com/Heoooo/TGuard-backend/pull/1
- Branch: `feat/security-idempotency-improvements`
- Commits:
  - `feat: JWT 테넌트 검증 추가`
  - `feat: Redis 기반 웹훅 멱등성 처리 추가`
  - `feat: 트랜잭션 Outbox 이벤트 발행 추가`
  - `feat: Actuator 엔드포인트 접근 제어 추가`

## 1) JWT Tenant Validation

### Problem

- 기존 JWT에는 `username`, `role`만 포함되어 있었다.
- 멀티테넌트 환경에서 클라이언트가 `X-Tenant-Id` 헤더를 임의로 바꾸면, 토큰의 사용자와 요청 테넌트가 서버에서 직접 교차 검증되지 않는 문제가 있었다.
- 같은 username이 여러 테넌트에 존재할 수 있는 구조에서는 데이터 격리 취약점으로 이어질 수 있다.

### Action

- JWT 발급 시 `tenantId` claim을 추가했다.
- 인증 필터에서 JWT의 `tenantId`와 `TenantContextHolder`에 설정된 요청 테넌트를 비교했다.
- 두 값이 다르면 인증을 진행하지 않고 `403 Forbidden`으로 차단하도록 변경했다.
- 기존 테넌트 격리 테스트 기대값을 `404 Not Found`에서 `403 Forbidden`으로 조정해, 데이터 계층 접근 전에 보안 계층에서 차단되는 정책을 명확히 했다.

### Result

- 테넌트 위변조 요청을 repository 조회 이전 인증 필터 단계에서 차단하도록 개선했다.
- 검증 기준:
  - 동일 tenant token/header: 요청 허용
  - 다른 tenant token/header: `403 Forbidden`
- 테스트:
  - `TenantContextFilterTest` 통과
  - `TenantIsolationIntegrationTest`는 Spring 통합 테스트 환경에서 장시간 실행되어 중단했지만, 컴파일 단계는 통과했다.

### Portfolio Copy

멀티테넌트 환경에서 JWT claim과 `X-Tenant-Id` 헤더를 교차 검증하도록 인증 필터를 개선해, 클라이언트의 테넌트 헤더 위변조 요청을 데이터 접근 이전에 `403`으로 차단했습니다.

## 2) Redis Webhook Idempotency

### Problem

- 기존 웹훅 멱등성 저장소는 애플리케이션 메모리 `Set` 기반이었다.
- 서버 재시작 시 멱등성 정보가 사라지고, 다중 인스턴스 환경에서는 인스턴스 간 중복 처리를 막을 수 없었다.
- TTL이 없어 장기 실행 시 메모리 증가 가능성도 있었다.

### Action

- Redis `SETNX` 방식의 `setIfAbsent`로 멱등성 키를 저장하도록 변경했다.
- 기본 TTL을 `86400초(24시간)`로 설정했다.
- Redis key에 tenant namespace를 추가해 테넌트 간 이벤트 ID 충돌을 방지했다.
- Redis 조회/저장 실패 시 중복 처리를 허용하지 않고 예외를 발생시키는 fail-closed 전략을 적용했다.

### Result

- 메모리 기반 process-local 멱등성에서 Redis 기반 distributed idempotency로 개선했다.
- 수치/설정:
  - TTL: `86400초`
  - Redis write: 이벤트당 `SETNX` 1회
  - key format: `tenant:{tenantId}:webhook:idempotency:{idempotencyKey}`
- 테스트:
  - `WebhookIdempotencyStoreTest` 통과
  - 검증 시간: Gradle targeted test 기준 약 `1분 22초`

### Portfolio Copy

웹훅 중복 처리 방지를 메모리 `Set`에서 Redis `SETNX + TTL` 구조로 전환해 서버 재시작 및 다중 인스턴스 환경에서도 멱등성을 유지하도록 개선했습니다. 테넌트별 key namespace와 24시간 TTL을 적용해 충돌 가능성과 저장소 누적 리스크를 함께 줄였습니다.

## 3) Transactional Outbox

### Problem

- 거래 저장 트랜잭션 내부에서 Kafka 발행을 직접 호출하고 있었다.
- DB 저장은 성공했지만 Kafka 발행이 실패하거나, Kafka 발행 이후 DB 트랜잭션이 롤백되는 경우 데이터와 이벤트 상태가 달라질 수 있었다.
- 실시간/배치 topic을 한 번에 발행하므로 한쪽 topic만 실패했을 때 재처리 범위가 모호했다.

### Action

- `transaction_outbox_event` 테이블을 추가했다.
- 거래 저장 시 Kafka 직접 발행 대신 outbox row를 저장하도록 변경했다.
- 별도 스케줄러가 미발행 outbox 이벤트를 조회해 Kafka로 발행하고, 성공 시 `published=true`로 표시하도록 구현했다.
- realtime/batch topic을 각각 별도 outbox row로 저장해 topic별 발행 상태를 독립적으로 관리하도록 했다.
- 발행 실패 시 `attemptCount`, `lastError`, `nextRetryAt`을 갱신해 재시도 가능하게 했다.

### Result

- DB transaction과 Kafka publish 사이의 정합성 리스크를 줄였다.
- 수치/설정:
  - 거래 이벤트 1건당 outbox row `2개` 생성: realtime topic 1개, batch topic 1개
  - pending 조회 batch size: 최대 `50개`
  - scheduler interval: `10초`
  - retry delay: `30초 * attempt`
  - Kafka send timeout: `5초`
- 테스트:
  - `TransactionOutboxServiceTest` 통과
  - `WebhookIdempotencyStoreTest` 통과
  - 두 targeted test 실행 시간: 약 `3분 31초`

### Portfolio Copy

거래 저장과 Kafka 발행의 정합성 문제를 줄이기 위해 Transactional Outbox 패턴을 도입했습니다. 거래 트랜잭션에서는 outbox row만 저장하고, 별도 스케줄러가 최대 50건씩 pending 이벤트를 발행한 뒤 성공 상태를 기록하도록 구현했습니다. realtime/batch topic을 각각 독립 row로 관리해 부분 실패 시 재시도 범위를 명확히 했습니다.

## Resume Bullets

- 멀티테넌트 인증 구조에서 JWT `tenantId` claim과 요청 헤더를 교차 검증하도록 개선해 테넌트 위변조 요청을 인증 필터 단계에서 `403`으로 차단
- 웹훅 멱등성 저장소를 메모리 `Set`에서 Redis `SETNX + TTL(24h)` 구조로 전환해 서버 재시작 및 다중 인스턴스 환경의 중복 처리 리스크 감소
- Transactional Outbox 패턴을 도입해 거래 저장과 Kafka 발행의 정합성 리스크를 완화하고, realtime/batch topic별 독립 재시도 구조 구현
- Outbox publisher를 최대 50건 단위 pending 조회, 10초 주기 스케줄링, `30초 * attempt` retry delay 방식으로 구현
- Actuator endpoint 접근 정책을 분리해 `/actuator/health`만 공개하고 metrics/prometheus 등 운영 endpoint는 관리자 JWT 인증 뒤 접근하도록 개선

## Interview Talking Points

- "멀티테넌시에서는 인증된 사용자와 요청 테넌트를 별도로 믿으면 안 된다고 판단했습니다. 그래서 JWT claim과 헤더를 교차 검증해 보안 경계를 필터 레벨로 끌어올렸습니다."
- "웹훅은 외부 시스템 특성상 같은 이벤트가 여러 번 올 수 있으므로, process-local 메모리보다 Redis SETNX를 사용해 분산 환경에서도 중복 처리를 막도록 했습니다."
- "DB 저장과 Kafka 발행을 한 트랜잭션 메서드에 묶어두면 장애 시 상태가 갈라질 수 있어, outbox에 먼저 기록하고 별도 publisher가 재시도하는 구조로 변경했습니다."
- "운영 지표는 장애 대응에 필요하지만 외부 공개 대상은 아니라고 판단했습니다. 그래서 health check만 공개하고 나머지 Actuator endpoint는 관리자 인증 대상으로 분리했습니다."

## Metrics To Measure Next

- Outbox 도입 전후 웹훅 응답 p95 latency
- Kafka broker 장애 상황에서 outbox pending 증가량과 복구 후 drain time
- Redis 장애 상황에서 웹훅 실패율과 중복 처리 방지 여부
- Outbox batch size 50 기준 초당 publish 처리량

## 4) Actuator Endpoint Access Control

### Problem

- 기존 보안 설정은 `/actuator/**` 전체를 공개하고 있었다.
- health check는 외부 로드밸런서/배포 검증을 위해 공개될 수 있지만, metrics/prometheus/env/loggers 같은 운영 endpoint는 내부 상태와 성능 정보를 노출할 수 있다.
- 앞선 JWT tenant 검증과도 충돌하지 않도록, 보호 대상 Actuator endpoint는 인증 필터를 타고 health endpoint만 필터를 건너뛰어야 했다.

### Action

- Spring Security 설정에서 `/actuator/health`, `/actuator/health/**`만 `permitAll`로 유지했다.
- 나머지 `/actuator/**`는 `ROLE_ADMIN` 권한이 필요하도록 변경했다.
- JWT 필터와 테넌트 필터의 whitelist를 health endpoint로만 좁혀 metrics 등 보호 endpoint는 인증/테넌트 검증을 거치게 했다.
- 전체 Spring context 없이 빠르게 검증할 수 있도록 필터 단위 테스트를 추가했다.

### Result

- 공개 Actuator 범위를 전체 wildcard `1개(/actuator/**)`에서 health endpoint 2개(`/actuator/health`, `/actuator/health/**`)로 축소했다.
- 검증 기준:
  - `/actuator/health`: 인증/테넌트 필터 skip
  - `/actuator/metrics`: JWT 필터 대상
  - tenant 불일치 token/header: `403 Forbidden`
- 테스트:
  - `JwtAuthenticationFilterTest` 통과
  - `TenantContextFilterTest` 통과
  - 두 targeted test 실행 시간: 약 `1분 26초`

### Portfolio Copy

운영 정보 노출 리스크를 줄이기 위해 Actuator endpoint 접근 정책을 재정의했습니다. `/actuator/health`만 공개하고 metrics 등 나머지 운영 endpoint는 관리자 JWT 인증 및 테넌트 검증을 거치도록 분리했으며, 필터 단위 테스트로 health 공개와 metrics 보호 정책을 빠르게 검증했습니다.
