# queue-service 작업 로드맵

queue-service(대기열)를 만드는 순서와 결정을 적습니다. 2026-10-14 사용자(B)의 구현 순서안을 바탕으로 Claude와 논의해 정했습니다.
작업은 이 문서를 기준으로 shrimp-task-manager로 나눠 진행합니다. CRUD를 한 번에 만들지 않고, 기능 하나씩 제대로 동작하는지 확인하며 진행합니다.
shrimp 작업 설명과 이 문서가 다르면 **이 문서가 우선**입니다. 작업을 시작할 때마다 이 문서의 최신 결정을 먼저 읽습니다.
참고 프로젝트는 `D:\Study\portfolio\ticketplay`이고, 우리 서비스(MSA, 게이트웨이 헤더 인증)에 맞게 바꿔서 씁니다.

- **[결정됨]**: 사용자와 함께 정한 것
- **[제안]**: Claude 의견, 아직 정하지 않음
- **[미정]**: 그 단계에서 정할 것

## 범위 (세미)

- 개인 예매 대기열: 대회 · 종목 단위로 줄 세우기, 활성(active) 승격, 순번 조회, 주문 쪽 입장 확인
- 빼는 것
  - 크루 예매의 대기열 진입: 최종 (10/13 결정, 세미는 개인 예매 + 대기열만)
  - [결정됨] 분산 락: 세미에서는 넣지 않는다. 스케줄러 서버를 일부러 따로 두었다. 최종 프로젝트에서 성능을 끌어올릴 때 일정에 따라 사용자가 정한다.

## 일정 제약

| 날짜 | 무엇 |
|---|---|
| 10/22 · 10/23 | 세미 제출 · 발표 |

- 주문(order-service)은 이다윗 님 담당이다. 주문 쪽 변경은 전달 목록으로 넘기고, 대기열과 주문을 끝까지 이어서 확인하는 일은 주문이 반영된 뒤에 한다.
- 주문은 아직 상품 API를 가짜 어댑터(`FakeSeatHoldAdapter`)로 대신한다. 그래서 대기열 쪽은 단독 테스트까지 먼저 확인한다.

## 결정 사항 (2026-10-14)

- [결정됨] 순서는 아래 1~6단계를 따른다 (사용자 순서안에 2단계를 새로 넣고, 순번 조회를 승격 앞으로 옮김).
- [결정됨] 뼈대 구성 (PRO-172)
  - order-service와 같은 구성: JPA · MySQL · QueryDSL · Kafka 포함
  - Redis 설정은 member-service 방식 (대기 시간 2초, dev `localhost:6379`, prod `${REDIS_HOST}`/`${REDIS_PORT}`)
  - `X-Member-Id`는 order-service 방식 (`in/MemberIdHeader` 상수 + `QueueExceptionHandler`)
- [결정됨] 대기열 진입 단위는 **대회 ID 하나 + 종목 ID 하나**다.
  - 대회(1) : 종목(N) 관계는 최종에서 대회를 기준으로 종목 옵션을 골라 대기열에 들어가도록 확장하려고 둔 것이다.
  - 세미에서 실제로 구현하는 관계는 대회(1) : 종목(1)이다. 최종에서 확장해도 대회 ID 하나에 종목 ID 하나로 대기열에 들어간다.
- [결정됨] 대기열에 들어올 수 있는 때는 판매 상태가 **`ON_SALE`**이고 **`individualOpenAt ≤ now < applicationCloseAt`**일 때다. `SCHEDULED`는 판매 예정이라 들어올 수 없다.
  - `ON_SALE`은 판매자에게만 보이는 상태다. 구매자에게 예매하기 버튼이 보이는 것은 `individualOpenAt` 시각부터다(`RaceSaleStatus.java:9`).
- [결정됨] 접수 일정은 **Kafka 복제**로 받는다. 대기열은 대회 · 종목 **복제 테이블**을 둔다.
  - 이유: 트래픽이 몰리는 대기열에서는 Cache-Aside(상품 내부 API + 캐시, 참고 `D:\Study\redis\redisinspring`)보다 복제한 대회 · 종목을 쓰는 쪽이 튼튼하고 유리하다.
  - 상품이 바뀌면 바뀐 정보가 대기열 복제 테이블에 **멱등하게** 반영되어야 한다.
- [결정됨] 여러 기기(아이폰 · 갤럭시 · 태블릿 · PC): **회원 단위 한 자리**다. 이미 줄에 있는 회원이 다시 등록하면 NX로 처리해 **순번을 유지**한다.
- [결정됨] 승격은 **주기마다 정해진 인원**을 올린다. 동시 active 상한은 두지 않는다.
  - 상한을 두면 active 회원이 결제를 마치거나 나가야 자리가 비어 다음 승격이 된다. 모두가 10분 동안 결제하지 않으면 나머지 회원은 10분 동안 같은 순번만 보게 된다.
- [결정됨] 정책: 주문 · 결제 페이지로 넘어가면 TTL 10분을 준다. 프론트 시안의 타이머 값은 예시일 뿐이다.
- [제안] Redis는 공용 `bukang-redis` 한 대를 쓴다. ticketplay의 `RedisConfig`(Sentinel, 레플리카 우선 읽기)는 가져오지 않는다. 레플리카에서 읽으면 승격 직후 조회가 옛 값을 볼 수 있다.

## 단계

### 1. yaml · gradle 검토 (+ Redis Testcontainers)

- 완료(10/14, PRO-172): 모듈 뼈대 `1bc5a73`(포트 8086, yaml 4개, compose · Dockerfile 등록), 게이트웨이 라우팅 `fcded5e`(`/api/v1/queue/**`)
- 완료(10/15): 진짜 Redis를 띄우는 테스트 수단을 넣고, 연결만 SET/GET으로 확인했다. ZSet · Lua 테스트는 스크립트를 만드는 3단계 · 5단계에서 쓴다.
  - member-service처럼 `com.redis:testcontainers-redis` + `testcontainers-junit-jupiter`. Lua는 가짜(mock) Redis로 검증할 수 없다(`member-service/.../RefreshTokenRepositoryTest.java:23`).
  - [결정됨] Docker가 없으면 실패시킨다(옵션 없는 `@Testcontainers`, member 방식). 팀원 PC는 고려하지 않는다(사용자 10/15). 전체 build는 이미 member 테스트 때문에 Docker가 필요하다.
  - [결정됨] 연결 확인 테스트 `out/RedisConnectionTest`(`redis:8.2`에 SET/GET)는 3단계 뒤에도 계속 둔다(사용자 10/15).
- yaml 4개(application, dev, prod, test)는 검토 결과 고칠 곳이 없다.
- Kafka(2단계 복제 수신)와 JPA(2단계 복제 테이블)는 둘 다 쓰므로 그대로 둔다.
- 확인(10/15): 사용자가 `RedisConnectionTest`를 실행해 통과, `./gradlew :queue-service:checkstyleMain :queue-service:checkstyleTest` 경고 0건

### 2. 상품 일정 이벤트 발행 → 대기열 복제본 수신

- 왜: 3단계 등록 API가 "지금 이 대회 · 종목에 줄을 세울 수 있는가"를 판정해야 한다.
- [결정됨] 대회와 종목을 함께 복제한다(대회 · 종목 복제 테이블, `QUEUE_` 접두사).
- [결정됨] 상품이 바뀌면 바뀐 정보를 복제 테이블에 멱등하게 반영한다. 같은 메시지가 두 번 와도 결과가 같다.
- [결정됨] 들어올 수 있는 때: 판매 상태 `ON_SALE` + 시각 조건 `individualOpenAt ≤ now < applicationCloseAt`.
  - 시각 조건도 보는 이유: `ON_SALE`은 판매 시작일(크루 예매 시작과 개인 예매 오픈 중 이른 날짜) 0시에 바뀌어 개인 예매 오픈 시각 전에도 `ON_SALE`일 수 있다. 또 `CLOSED`는 대회일 다음 날 0시에 바뀌어, 접수 마감 뒤에도 그때까지 `ON_SALE`로 남는다(`Race.java:233-239`, `RaceSaleStatus.java:8`).
- [결정됨] 상품의 상태 배치가 판매 상태를 바꾸면, 상품이 Kafka 생산자로서 대기열에 동기화 메시지를 보낸다.
  - 상태는 등록 때 `SCHEDULED`이고, 5분마다 도는 상태 배치가 `ON_SALE`로 바꾼다(`RaceSaleStatusScheduler`, `ProductFacade.java:149-161`). 이 메시지가 없으면 복제본은 `SCHEDULED`에 머물러 대기열이 열리지 않는다.
  - 알려진 한계: 개인 예매 오픈이 판매 시작일 0시 정각이면, 상태 배치(5분 주기)가 `ON_SALE`로 바꾸고 메시지가 올 때까지 최대 5분쯤 줄을 설 수 없다. 상품은 같은 이유로 자리 확보 판정에 `SCHEDULED`도 넣었다(`Race.java:289`).
    - [결정됨] 막지 않는다. 자정을 예매 시작 시각으로 잡는 판매자는 드문 엣지 케이스라, 실제 운영에서 문제가 생기면 그때 대응한다(사용자 10/14).
- 상품 쪽 (사용자 담당): 커밋 뒤 메시지를 보낸다. 지금 상품은 일정 메시지를 보내지 않는다(등록 · 수정은 `race.filelink.requested`, 취소는 `order.race.cancel`만).
- 대기열 쪽: [결정됨] `in/QueueKafkaConsumer`가 `@KafkaListener`로 받아 복제 테이블에 저장한다.
- [제안] 세부 (B1~B8)
  - B1 메시지: 대회 하나에 메시지 하나(key = `raceId`), 대회와 종목을 함께 담은 스냅샷. 담는 값은 `raceId`, 종목 목록(`raceCourseId` 등), `individualOpenAt`, `applicationCloseAt`, `saleStatus`, 순서 판단 값(B5)이다.
  - B2 보내는 시점: 등록 · 수정 · 취소 커밋 뒤. 상태 배치가 판매 상태를 바꾼 뒤(`ON_SALE`/`CLOSED`)에도 보낸다([결정됨]). 발행은 `ProductFacade.publish()`(`ProductFacade.java:186`)를 쓴다. 매진(`SOLD_OUT`)은 보내지 않는다. 결제 대기가 풀리면 `OPEN`으로 돌아오는 일시적인 값이다.
  - B3 토픽: `queuing.race.changed` (상수는 common `KafkaTopics`, 이벤트 클래스는 common `shared/race/event/`). 취소는 스냅샷의 `saleStatus = CANCELED`로 받으므로 `order.race.cancel`은 구독하지 않는다.
  - B5 순서 판단 값(늦게 온 옛 메시지 건너뛰기, 회원 동기화 방식 `ProductSyncMemberUseCase.java:21-25`). [미정] 무엇으로 할지:
    - `race.updated_at`: 대회 칸이 바뀐 수정과 취소 · 상태 배치에서는 갱신된다(`RaceRepository.java:61,71,78`). 하지만 종목만 바꾼 수정에서는 바뀌지 않는다. 종목은 자기 `race_course.updated_at`만 바뀐다(`RaceCourse.java:43`, `RaceUpdateUseCase.java:56-85`).
    - 발행 시각: 메시지를 만들 때의 시각
    - 상품 수정 때 `race.updated_at`도 항상 갱신하도록 상품을 고친다
    - 종목 `updated_at`을 기준으로 둔다(사용자 안, 10/14). 종목 `updated_at`만으로는 대회 칸 변경(일정 · 취소 · 상태 배치)을 가릴 수 없다. 이때는 `race.updated_at`만 바뀌기 때문이다.
    - [제안] 행마다 자기 `updated_at`으로 비교한다(사용자 안을 넓힌 것): 대회 칸은 `race.updated_at`, 종목 칸은 종목마다 `race_course.updated_at`. 그러면 대회만 바뀐 수정과 종목만 바뀐 수정을 모두 가린다.
      - 남는 경우는 종목 삭제다. 삭제하면 대회 · 남은 종목의 `updated_at`이 바뀌지 않는다. 다만 종목은 판매 시작일 전에만 뺄 수 있다(`RaceCourse.java:187-191`, `Race.java:268-270`). 그때는 아직 `SCHEDULED`라 대기열이 열리지 않았고, 판매 시작일 0시 상태 배치 메시지(`race.updated_at` 갱신)가 종목 목록을 바로잡는다.
    - 참고: key가 `raceId`라 같은 대회 메시지는 한 파티션 안에서 순서가 지켜진다. 순서가 뒤집히는 것은 실패한 메시지가 재시도 토픽을 거칠 때다.
    - 확인(10/15, 코드와 Hibernate 7.4.5 소스를 읽어 확인, 테스트 실행은 안 함): 대회 칸만 고치면 `race.updated_at`만, 종목 칸만 고치면 그 종목의 `race_course.updated_at`만 바뀐다. `Race`에 `@Version`이 없어 종목 변경이 대회로 전파되지 않는다. `race_course.updated_at`은 자리 확보 · 판매 · 해제 · 복구 때도 바뀐다(`RaceCourseRepository.java:28-50`).
    - 알려진 한계: 취소 · 상태 배치는 대회 행 잠금을 잡기 전에 시각을 정한다(`ProductFacade.java:93,152`). 같은 대회의 수정과 겹치면 `race.updated_at`이 거꾸로 가서, 대기열이 새 메시지(취소 · `ON_SALE`)를 옛것으로 보고 건너뛸 수 있다.
      - [결정됨] 막지 않는다. 같은 대회의 수정과 취소를 잠금 대기 시간(밀리초) 안에 겹쳐 누르는 것은 사람의 동작이 아니다. 대기열에서 `updated_at`은 다른 칸(판매 상태 · 일정)이 옛 값으로 덮이지 않게 하는 비교용 값일 뿐이고, 판매 상태의 원본은 상품이다(사용자 10/15).
    - 사용자 안(10/15): 대회 메시지와 종목 메시지를 나눈다(토픽 · 리스너를 대회용 · 종목용으로 나누고, 행마다 자기 `updated_at`으로 비교). [미정] 채택 여부는 2단계에서 B1과 함께 정한다.
      - 나누면 종목 삭제 메시지가 따로 필요하다(스냅숏은 목록을 통째로 바꿔 빠진 종목이 저절로 지워진다).
      - 두 토픽 사이의 순서는 보장되지 않는다. 종목 메시지가 대회 메시지보다 먼저 올 수 있어 복제 테이블에 외래 키를 걸지 않고, 진입 판정은 둘 다 있어야 통과로 한다.
  - B6 재시도 · DLT: `QueueKafkaConsumer`도 `@RetryableTopic` 3번(1초 → 2초), `IllegalArgumentException`은 바로 DLT로 둔다. DLT는 로그만 남기는 전용 컨슈머(이름 예: `QueueKafkaDltConsumer`)가 받는다. 설정은 상품 컨슈머를 참고한다(`ProductKafkaConsumer.java:34-37,54-64`, `ProductKafkaDltConsumer.java:28-47`).
  - B7 대기열 전에 등록된 대회: 세미는 더미 대회를 새로 등록해 채운다. "전체 다시 보내기"는 필요하면 최종에서 만든다.
  - B8 복제 테이블에 없는 대회 · 종목으로 등록: 404로 막는다. 상품 API로 대신 조회하지 않는다(복제를 고른 이유와 같음).
- 알려진 한계: 상품의 발행 실패는 로그만 남긴다(10/13 아웃박스 없음 결정). 메시지를 놓치면 복제본이 계속 틀린다.
  - 다만 자리 확보 때 상품이 접수 기간을 다시 확인해 409로 막으므로(`RaceHoldUseCase.java:62-67`) 자리가 잘못 잡히지는 않는다.
- 확인: 상품에서 대회 · 종목을 등록 · 수정 · 취소하고 상태 배치가 돌면 대기열 복제 테이블에 반영되는지 본다(Redpanda Console http://localhost:8091). 같은 메시지를 두 번 보내도 결과가 같은지(멱등), 옛 메시지를 건너뛰는지 확인한다.

### 3. 등록 API (회원 단위 NX)

- 요청은 대회 ID 하나 + 종목 ID 하나다. 회원은 `X-Member-Id`로 받고, ZSet 멤버는 회원 ID다. 여러 기기는 한 자리를 함께 쓴다.
  - 두 번째 기기를 막지는 않는다. 같은 회원의 중복 예매는 주문이 막는다: 대회당 살아 있는 신청 1건(`uk_participation_active`, `Participation.java:36`), [결제하기]의 신청 행 잠금(`OrderCreateUseCase.java:43-44`).
- [결정됨] waiting과 active를 **함께** 확인하는 Lua 하나로 등록한다.
  - waiting에 있으면 NX로 순번을 유지한다.
  - NX는 waiting만 본다. 승격된 회원은 waiting에서 빠지므로 NX만 쓰면 다시 줄에 들어간다. 그 뒤 다시 승격되면 active 끝 시각이 덮어써져 활성 시간이 늘어난다(ticketplay `RegistrationService.java:41`).
  - 확인과 추가를 명령 두 번으로 나누면 그 틈에 승격 Lua가 끼어들 수 있어서 Lua 하나로 묶는다.
- score는 등록 시각(epoch ms)이다. 같은 밀리초 동점은 멤버 문자열 사전순으로 정렬된다(`"10"`이 `"9"`보다 앞). 영향은 작다.
- 접수 판정은 2단계 복제 테이블로 한다.
- 키는 `.claude/rules/redis-key-convention.md`(콜론 계층, 키는 한곳에서 만든다)를 따른다. Lua는 member의 `RefreshTokenRepository` 방식(`resources/redis/*.lua` + `DefaultRedisScript`)을 따른다.
  - 사용자 후보(10/14): `race:{raceId}:racecourse:{raceCourseId}` 아래에 대기열 정보를 둔다.
- ticketplay에서 바꿀 것: 인증(`@AuthenticationPrincipal` 아이디 → `X-Member-Id` 숫자), 경로(`/api/reservation/...` → `/api/v1/queue/...`), 계층 없는 `queues` 키
- [미정] 경로 · 키 이름(위 사용자 후보 기준)
- [미정] active인 회원이 다시 등록할 때 응답: 지금 상태를 그대로 돌려주기 / 거부
- [미정] Redis 장애 때: 막기(fail-closed, 예매 중단) / 통과(fail-open, 주문 · 상품에 트래픽이 몰림)
- 확인: Testcontainers Redis로 등록 · 다른 기기 재등록(순번 유지) · active 회원 재등록 · `ON_SALE`이 아닌 대회 등록 · 개인 예매 오픈 전과 접수 마감 뒤 등록

### 4. 순번 조회 API (브라우저 폴링)

- 대기 화면이 몇 초마다 부르는 API다. 3단계 등록과 5단계 승격을 눈으로 확인하는 수단이기도 하다.
- ticketplay는 `/status`(순번 · 앞 인원 · 전체)와 `/active`(입장 여부)를 따로 두고 3초마다 둘 다 불렀다(`waiting.html:169-202`).
- [미정] 응답 모양: 순번 · 앞 인원 · 입장 여부를 한 번에 줄지 나눌지, `expiresAt`을 넣을지, 줄에도 없고 active도 아닐 때 응답(ticketplay는 400)
- [제안] 이 API는 DB 커넥션을 잡지 않는다. Facade 클래스 단위 `@Transactional(readOnly = true)` 관례를 그대로 따르면 Redis만 읽어도 커넥션을 잡는다.
  - ticketplay는 이것을 빼서 HikariCP 대기가 188에서 0으로 줄고 처리량이 41% 늘었다(커밋 `d06140c`).
- 프론트 전달: 대기가 30분을 넘으면 Access Token이 만료돼 폴링이 401을 받는다. 프론트에는 재발급 흐름이 아직 없다(`frontend/.../fetcher.ts:30` TODO).
- 확인: 등록 뒤 조회로 순번 · 앞 인원 확인

### 5. 승격 스케줄러

- waiting 앞에서부터 active로 옮기고, active 끝 시각은 승격 시각 + 10분이다.
  - Redis TTL은 키 단위라 회원마다 줄 수 없다. 그래서 active ZSet의 score에 끝 시각을 넣어 판정한다(ticketplay와 같음).
- [결정됨] 만료된 active는 **승격 스케줄러 한 곳에서만** 지운다. ticketplay는 스케줄러와 순번 조회 두 곳에서 지워서 만료 후처리(좌석 해제)가 건너뛰어졌다(`QueueScheduler.java:48-55`, `RegistrationService.java:81-82`). 6단계의 자리 해제 주체와 이어서 정한다.
- [결정됨] `@EnableScheduling`을 넣고, 스케줄러는 `@Profile("!test")`로 테스트에서 끈다(order · product 관례).
- [결정됨] 승격 규칙: 주기마다 정해진 인원(동시 active 상한 없음, 결정 사항 참고). 동시에 active인 회원 수는 저절로 "주기마다 올리는 인원 × (600초 ÷ 주기)"를 넘지 않는다(예: 3초마다 10명이면 최대 2,000명).
- [미정] 주기와 한 번에 올릴 인원
- [결정됨] 한 대기열 키(대회 · 종목)에서 오류가 나도 다른 대회 대기열의 승격에는 영향이 없어야 한다. 대기열 키마다 오류를 따로 처리한다.
- [결정됨] 방식: `@Scheduled` + UseCase(order · product 관례). Spring Batch를 쓴다고 더 효율적이지 않다(10/14 Claude 판단, 사용자 동의).
  - 승격은 몇 초마다 Redis 명령 몇 개(Lua)를 실행하는 일이다. Batch의 장점(대량 데이터 청크 처리, 실패 지점부터 재시작, 실행 기록)을 쓸 곳이 없다.
  - 실패해도 다음 주기가 곧 재시도이므로 재시작 기능이 필요 없다.
  - Batch는 Job을 실행할 때마다 실행 기록을 남긴다. JDBC 저장소(file-service 방식)면 3초 주기에 하루 약 28,800번 `BATCH_` 테이블에 행이 쌓인다. 기록을 남기지 않는 `ResourcelessJobRepository`(spring-batch-core 6)도 있지만, 그래도 얻는 것이 없다.
- [미정] 시각: `Clock` 빈 주입(member 방식) / Facade · 스케줄러가 `now`를 만들어 UseCase에 넘기기(order · product 방식). epoch ms와 `LocalDateTime`(상품 `expiresAt`)을 바꿀 때 시간대를 정한다.
- [미정] 이탈 API: 없으면 창을 닫은 회원도 순번을 유지한 채 승격돼 10분 동안 active로 남는다.
- 확인: 고정 시각으로 UseCase를 직접 불러 승격 · 만료 정리 확인 (Testcontainers Redis)

### 6. 주문 연동 API

- 주문이 [결제하기] 때 "이 회원이 이 대회 · 종목의 active인가"와 active 끝 시각(`expiresAt`)을 확인할 수단이다.
  - ticketplay는 한 앱 안에서 `registrationService.isActive(...)`를 직접 불렀다(`WorkController.java:60-62`). 결제가 끝나면 `leaveActive(...)`로 active에서 뺐다(`PaymentService.java:44-45`). 우리는 서비스가 나뉘어 있어 서비스 간 통신이 필요하다.
- 필요한 이유
  - 대기열 건너뛰기를 막는다. 주문 API는 게이트웨이로 누구나 부를 수 있고, 브라우저가 폴링으로 받은 결과는 믿을 수 없다.
  - 상품 자리 확보 API가 `expiresAt`(대기열 활성 시간이 끝나는 시각)을 필수로 받는다(`HoldRequestDto.java:27-31`). 지금 주문의 `SeatHoldPort.hold(raceCourseId, participationId, memberId)`에는 이 값이 없다(`SeatHoldPort.java:8`).
- [결정됨] 정책: 주문 · 결제 페이지로 넘어가면 TTL 10분을 준다.
- [미정] 입장 여부와 `expiresAt`을 주문에 주는 방법. 고르는 안에 따라 이 단계의 모양이 바뀐다.
  - 주문이 대기열 내부 API(`/internal/v1/queue/...`)를 부른다 → 이 단계에서 내부 API를 만든다
  - 브라우저가 대기열에서 받은 값을 주문 요청에 실어 보낸다 → 대기열이 서명 토큰 등 위조할 수 없는 값을 발급한다
  - 게이트웨이가 주문 경로에서 검사한다 → 게이트웨이가 Redis를 보게 된다
- [미정] 이다윗 님과 정할 것
  - 참가 신청서 작성을 대기열 앞에서 할지 뒤에서 할지 (지금 신청서 API는 대기열 검사 없이 만들어진다, `ParticipationApplyUseCase.java:45-50`)
  - 장바구니 결제를 대기열에 넣을지. 대기열 진입은 대회 하나 + 종목 하나인데, 장바구니 결제는 한 요청에 참가 신청을 최대 10건 받는다(`CartCheckoutRequestDto.java:18,23`, 항목마다 [결제하기]를 부름 `CartCheckoutUseCase.java:97`).
  - active가 끝났을 때 자리 해제 주체. 10/13 결정은 "주문이 `product.hold.cancel` 발행"이지만, 주문에는 아직 발행 코드가 없고 만료 스케줄러는 1분 주기다(`OrderExpiryScheduler.java:23`).
  - 결제가 끝났을 때 active 반납 방법 (반납하지 않고 10분 만료에 맡김 / 결제 완료 메시지 / 내부 API)
- 주문 코드는 수정하지 않는다. 주문 쪽 변경(입장 확인 호출, `expiresAt` 전달 등)은 이다윗 님 전달 목록에 넣는다.
- 확인: 대기열 쪽은 MockMvc 단독 테스트. 주문과 이어서 확인하는 일은 주문 반영 뒤에 한다.

## 남은 일 (단계 밖)

- [결정됨] 이슈와 PR: 대기열 작업을 모두 마친 뒤 Linear 이슈(PRO) 정리와 PR을 한 번에 한다(사용자 10/14). 지금 이슈는 부모 PRO-111, 1단계 뼈대 PRO-172(In Progress)뿐이다.
- 브랜치: `feat/PRO-172-queue-skeleton`(`1bc5a73` 뼈대, `fcded5e` 게이트웨이 라우팅, ROADMAP 커밋들). 아직 푸시하지 않았다.
- 문서 맞추기
  - `docs/policy/party-lottery.md:16`과 `backend/product-service/ROADMAP.md:15`에는 대기열이 "최종"으로 적혀 있다. 10/13 결정(세미 = 개인 예매 + 대기열)에 맞춘다.
  - `CONTEXT.md`에는 대기열 · 활성 시간 용어가 없다.
  - `docs/erd/cash.md:10`의 "결제 기한은 개인 10분(임시 확보부터)"이 위 TTL 10분 정책과 같은 뜻으로 읽히는지 확인한다.
- 프론트: 지금 예매 흐름(상세 → 신청 → 결제)에 대기 화면을 넣을 자리와 API 계약을 프론트 담당과 맞춘다.
- "중복 기기 불가"의 정책 원문(피그잼 2차 8장)은 이번 논의에서 확인하지 않았다.

## 논의 기록

### 2026-10-14 뼈대 (PRO-172)

- 사용자 지시: 다른 `*-service`와 같은 구조로 만든다. Redis · Docker · `X-Member-Id` · yaml(dev · prod · test) · gradle은 다른 서비스와 맞춘다.
- 사용자 선택: JPA 포함, Kafka 포함, `X-Member-Id`는 order 방식, Linear는 PRO-111 아래 하위 이슈 + 세미 제출(PRO-111 마일스톤도 최종 발표 → 세미 제출)
- Claude 기본값(보고함): 포트 8086, 게이트웨이 경로 `/api/v1/queue/**`, compose 앱 컨테이너는 포트를 열지 않음, 공용 `bukang-redis` 한 대, `@EnableScheduling`은 구현 때, 커밋은 모듈 뼈대와 게이트웨이 라우팅 두 개

### 2026-10-14 구현 순서 논의

- 사용자 순서안: ① yaml · gradle 검토 ② 주문으로 넘겨줄 데이터 논의(상품 정보, 유저 정보?, 판매자 정보?) ③ Waiting 등록 API(중복 기기 불가) ④ Waiting → Active 승격(TTL 10분) 스케줄러(배치?) ⑤ 주문 서비스에서 상태 폴링을 위한 API
- Claude 의견
  - ②: 상품 · 판매자 정보는 주문이 상품 내부 API(`/internal/v1/product/courses/{id}`)로 직접 가져오고, 회원은 `X-Member-Id`로 받는다. 대기열이 줄 것은 입장 여부와 `expiresAt`이다.
  - ⑤: 브라우저 폴링(순번 조회)과 주문의 입장 확인 두 가지로 읽힌다. 경로 · 인증 · 응답이 모두 다르다.
  - ③: 지금 요청 정보(`X-Member-Id`)로는 기기를 구분할 수 없다. Access Token에 기기 정보가 없다.
  - 빠진 것: 대기열 단위, 여는 시점과 일정 출처, 이탈 · 반납, Redis 장애 정책, Redis 테스트 수단
- 사용자 결정
  - 대기열 단위: 종목 (세미 1:1)
  - 일정 출처: Kafka 복제 (상품 내부 API + Cache-Aside 대신)
  - 여러 기기: 회원이 이미 줄에 있으면 NX
  - NX면 순번 유지 → Claude 확인: 맞다. 단 waiting과 active를 함께 확인해야 한다(3단계).
- 사용자 질문: "주문 연동 API는 원래 없던 단계인가?" → ⑤를 두 갈래로 나눈 것이고, ticketplay에서는 API 없이 같은 앱 안에서 `isActive`를 직접 불렀다. 남은 질문(입장 확인 방법)에 따라 6단계 모양이 바뀐다.
- 최종 순서(사용자 확인): 1 yaml · gradle(+ Redis Testcontainers) → 2 상품 일정 발행 · 복제 수신 → 3 등록 → 4 순번 조회 → 5 승격 → 6 주문 연동

### 2026-10-14 ROADMAP 검토 (사용자 피드백)

- 사용자 결정
  - 진입 단위: 대회 ID 하나 + 종목 ID 하나. 1:N은 최종 확장(대회를 기준으로 종목 옵션 선택)용이고, 세미 구현은 1:1이다.
  - 들어올 수 있는 판매 상태는 `ON_SALE`이다(`SCHEDULED`는 판매 예정).
  - 대회와 종목을 함께 복제한다. 바뀐 정보는 복제 테이블에 멱등하게 반영한다. Redis 키 후보 `race:{raceId}:racecourse:{raceCourseId}`
  - 복제를 고른 이유: 트래픽이 몰리는 대기열에서 Cache-Aside보다 대회 · 종목 복제가 튼튼하고 유리하다.
  - 대기열 컨슈머 이름은 `QueueKafkaConsumer`
  - 만료된 active는 승격 스케줄러 한 곳에서만 지운다. 스케줄러는 `@Profile("!test")`
  - 분산 락은 세미에서 넣지 않는다(스케줄러 서버를 따로 둠). 최종에서 성능을 끌어올릴 때 사용자가 정한다.
  - 승격은 주기마다 정해진 인원이고, 동시 active 상한은 두지 않는다(상한이면 모두가 결제하지 않는 10분 동안 대기열이 멈춘다).
  - 정책: 주문 · 결제 페이지로 넘어가면 TTL 10분. 프론트 시안 타이머는 예시다.
  - 이슈 정리와 PR은 대기열 작업을 다 마친 뒤 한 번에 한다.
- 사용자 지적: "종목마다 예외를 잡아 다음 종목을 계속 처리한다"는 문장은 시스템을 잘못 이해한 것이다 → 이 [제안]을 지웠다.
- Claude 답
  - `updated_at`: 종목만 바꾼 수정은 `race_course.updated_at`만 바뀌고 `race.updated_at`은 그대로다(B5에 반영).
  - Spring Batch: 승격에는 더 효율적이지 않다(5단계 [제안]에 이유).
  - `ON_SALE`을 기준으로 하면 상태 배치가 바꾼 상태도 메시지로 보내야 한다(B2에 반영).

### 2026-10-14 ROADMAP 검토 2차

- 사용자 결정
  - Spring Batch 대신 `@Scheduled` + UseCase (Batch는 승격에 이득이 없다)
  - 한 대기열 키에서 오류가 나도 다른 대회 대기열 승격에는 영향이 없어야 한다. 앞서 "다음 종목을 계속 처리"라고 적은 것은 같은 대회의 다음 종목으로 읽힐 수 있는 표현이었다.
  - 상품 상태 배치가 판매 상태를 바꾸면 상품이 Kafka로 대기열에 동기화 메시지를 보낸다.
  - 진입 조건은 `ON_SALE` + `individualOpenAt ≤ now < applicationCloseAt`. `ON_SALE`은 판매자에게만 보이는 상태이고, 구매자 예매하기 버튼은 `individualOpenAt`부터 보인다.
- 사용자 질문: "종목의 `updated_at`을 기준으로 두면?" → 종목 `updated_at`만으로는 대회 칸 변경을 못 가린다. 행마다 자기 `updated_at`으로 비교하는 안을 [제안]으로 B5에 적었다.

### 2026-10-15 B5 `updated_at` 확인

- 사용자 요청: 대회만 고치면 대회 `updated_at`만, 종목만 고치면 종목 `updated_at`만 바뀌는지 확인 → 맞다(B5 확인 줄).
- 사용자 안: 동기화 컨슈머를 대회용 · 종목용으로 나누기 → B5 아래에 [미정]으로 적었다.
- Claude가 찾은 한계: 취소 · 상태 배치가 수정과 겹치면 `race.updated_at`이 거꾸로 갈 수 있다 → 사용자 결정: 막지 않는다(사람이 낼 수 없는 겹침, `updated_at`은 비교용 값일 뿐).
- 사용자 결정(10/14): 개인 예매 오픈이 판매 시작일 0시 정각일 때의 최대 5분 지연도 막지 않는다(2단계 알려진 한계).
