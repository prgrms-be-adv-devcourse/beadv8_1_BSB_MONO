# queue-service 작업 로드맵

queue-service(대기열)를 만드는 순서와 결정을 적습니다. 2026-10-14 사용자(B)의 구현 순서안을 바탕으로 Claude와 논의해 정했습니다.
작업은 이 문서를 기준으로 shrimp-task-manager로 나눠 진행합니다. CRUD를 한 번에 만들지 않고, 기능 하나씩 제대로 동작하는지 확인하며 진행합니다.
참고 프로젝트는 `D:\Study\portfolio\ticketplay`이고, 우리 서비스(MSA, 게이트웨이 헤더 인증)에 맞게 바꿔서 씁니다.

- **[결정됨]**: 사용자와 함께 정한 것
- **[제안]**: Claude 의견, 아직 정하지 않음
- **[미정]**: 그 단계에서 정할 것

## 범위 (세미)

- 개인 예매 대기열: 종목 단위로 줄 세우기, 활성(active) 승격, 순번 조회, 주문 쪽 입장 확인
- 빼는 것
  - 크루 예매의 대기열 진입: 최종 (10/13 결정, 세미는 개인 예매 + 대기열만)
  - [제안] 여러 서버에서 승격 스케줄러 중복 실행 막기(분산 락): 지금은 서버 한 대(compose)라서 최종으로 미룬다

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
- [결정됨] 대기열 단위는 **종목**(`race_course`)이다. 코드는 대회:종목 1:N을 막지 않지만, 세미에서는 종목을 더 추가하지 않고 1:1로 쓴다.
- [결정됨] 접수 일정은 **Kafka 복제**로 받는다(상품 내부 API + Cache-Aside 대신). 메시지 단위와 저장 위치는 2단계 B1 · B4([제안])에서 정한다.
  - 상품 내부 API + Cache-Aside(참고 `D:\Study\redis\redisinspring`)는 고르지 않았다. 오픈 순간에는 캐시가 비어 있어, 동시에 들어온 등록 요청이 모두 상품 API로 몰린다.
- [결정됨] 여러 기기(아이폰 · 갤럭시 · 태블릿 · PC): **회원 단위 한 자리**다. 이미 줄에 있는 회원이 다시 등록하면 NX로 처리해 **순번을 유지**한다.
- [제안] Redis는 공용 `bukang-redis` 한 대를 쓴다. ticketplay의 `RedisConfig`(Sentinel, 레플리카 우선 읽기)는 가져오지 않는다. 레플리카에서 읽으면 승격 직후 조회가 옛 값을 볼 수 있다.

## 단계

### 1. yaml · gradle 검토 (+ Redis Testcontainers)

- 완료(10/14, PRO-172): 모듈 뼈대 `1bc5a73`(포트 8086, yaml 4개, compose · Dockerfile 등록), 게이트웨이 라우팅 `fcded5e`(`/api/v1/queue/**`)
- 할 일: 실제 Redis로 ZSet · Lua를 확인할 수단을 넣는다.
  - member-service처럼 `com.redis:testcontainers-redis` + `testcontainers-junit-jupiter`. Lua는 가짜(mock) Redis로 검증할 수 없다(`member-service/.../RefreshTokenRepositoryTest.java:23`).
  - [미정] Docker가 없을 때: 건너뛰기(`@Testcontainers(disabledWithoutDocker = true)`, product · file 방식) / 실패시키기(옵션 없음, member 방식)
- Kafka는 2단계 복제 수신에 쓰므로 그대로 둔다. JPA는 2단계 복제본을 DB에 둘지(B4)에 따라 정리한다.
- 확인: `./gradlew :queue-service:build`, `./gradlew checkstyleMain checkstyleTest`

### 2. 상품 일정 이벤트 발행 → 대기열 복제본 수신

- 왜: 3단계 등록 API가 "지금 이 종목에 줄을 세울 수 있는가"를 판정해야 한다.
  - [제안] 판정은 상품과 같게 한다: 판매 상태가 `SCHEDULED`/`ON_SALE`이고 `individualOpenAt ≤ now < applicationCloseAt` (`product-service/.../domain/Race.java:288-294`).
  - 판매 상태만으로는 판정할 수 없다. `ON_SALE`은 판매 시작일 0시에 바뀌어 개인 예매 오픈 전에도 `ON_SALE`이고, `CLOSED`는 대회일 다음 날 0시에 바뀐다.
  - 일정은 대회(`race`)에 있고 종목에는 없다. [제안] 그래서 종목 → 대회 연결도 함께 복제한다(B1).
- 상품 쪽 (사용자 담당): 커밋 뒤 메시지를 보낸다. 지금 상품은 일정 메시지를 보내지 않는다(등록 · 수정은 `race.filelink.requested`, 취소는 `order.race.cancel`만).
- 대기열 쪽: `@KafkaListener`로 받아 복제본에 저장한다.
- [제안] 세부 (B1~B8)
  - B1 메시지: 대회 하나에 메시지 하나(key = `raceId`), 스냅샷으로 보낸다. 담는 값은 `raceId`, 종목 ID 목록, `individualOpenAt`, `applicationCloseAt`, `saleStatus`, `updatedAt`이다. 수정 때 종목이 빠져도 목록만 바꾸면 된다.
  - B2 보내는 시점: 등록 · 수정 · 취소 커밋 뒤 (`ProductFacade.publish()`, `ProductFacade.java:186`). 상태 배치(`ON_SALE`/`CLOSED`)와 매진(`SOLD_OUT`)은 보내지 않는다. 접수는 시각으로 판정하고, `SOLD_OUT`은 결제 대기가 풀리면 `OPEN`으로 돌아오는 일시적인 값이다.
  - B3 토픽: `queuing.race.changed` (상수는 common `KafkaTopics`, 이벤트 클래스는 common `shared/race/event/`). 취소는 스냅샷의 `saleStatus = CANCELED`로 받으므로 `order.race.cancel`은 구독하지 않는다.
  - B4 저장 위치: DB 테이블(`QUEUE_` 접두사). 복제본을 읽는 곳은 등록 API 한 곳이고, 등록은 회원마다 한 번이다. Redis Hash는 Redis가 비워지면 복제본도 사라진다.
  - B5 늦게 온 옛 메시지: 저장된 `updatedAt`보다 오래되면 건너뛴다(회원 동기화 방식, `ProductSyncMemberUseCase.java:21-25`).
    - `Race.updated_at`은 취소 · 상태 배치의 bulk UPDATE에서도 직접 채운다(`RaceRepository.java:61,71,78`). 수정 때는 엔티티 Auditing(`BaseIdAndTime.java:26`)이 갱신한다.
    - 한계: 대회 칸은 그대로 두고 종목만 바꾼 수정에서는 `updated_at`이 바뀌지 않는다(`RaceUpdateUseCase.java:56-85`). 그런 메시지끼리는 `updatedAt`으로 순서를 가릴 수 없다.
    - [미정] 순서 기준을 `updatedAt`으로 할지, 다른 값(발행 시각 등)으로 할지
  - B6 재시도 · DLT: 상품 컨슈머와 같게 `@RetryableTopic` 3번(1초 → 2초), `IllegalArgumentException`은 바로 DLT, DLT는 로그만 남긴다(`ProductKafkaConsumer.java:34-37,54-64`, DLT 로그는 `ProductKafkaDltConsumer.java:28-47`).
  - B7 대기열 전에 등록된 대회: 세미는 더미 대회를 새로 등록해 채운다. "전체 다시 보내기"는 필요하면 최종에서 만든다.
  - B8 복제본에 없는 종목으로 등록: 404로 막는다. 이때 상품 내부 API로 대신 조회하면 오픈 순간 요청이 상품으로 몰린다(결정 사항의 Cache-Aside 항목과 같은 문제).
- 알려진 한계: 상품의 발행 실패는 로그만 남긴다(10/13 아웃박스 없음 결정). 메시지를 놓치면 복제본이 계속 틀린다.
  - 다만 자리 확보 때 상품이 접수 기간을 다시 확인해 409로 막으므로(`RaceHoldUseCase.java:62-67`) 자리가 잘못 잡히지는 않는다.
- 확인: 상품에서 대회 일정을 바꾸면 대기열 복제본에 반영되는지 본다(Redpanda Console http://localhost:8091). 같은 메시지가 두 번 와도 결과가 같아야 한다(멱등). 보내는 시점 · 옛 메시지 처리 · 확인용 대회는 B2 · B5 · B7이 정해진 뒤 맞춘다.

### 3. 등록 API (회원 단위 NX)

- 회원은 `X-Member-Id`로 받고, ZSet 멤버는 회원 ID다. 여러 기기는 한 자리를 함께 쓴다.
  - 두 번째 기기를 막지는 않는다. 같은 회원의 중복 예매는 주문이 막는다: 대회당 살아 있는 신청 1건(`uk_participation_active`, `Participation.java:36`), [결제하기]의 신청 행 잠금(`OrderCreateUseCase.java:43-44`).
- [결정됨] waiting과 active를 **함께** 확인하는 Lua 하나로 등록한다.
  - waiting에 있으면 NX로 순번을 유지한다.
  - NX는 waiting만 본다. 승격된 회원은 waiting에서 빠지므로 NX만 쓰면 다시 줄에 들어간다. 그 뒤 다시 승격되면 active 끝 시각이 덮어써져 활성 시간이 늘어난다(ticketplay `RegistrationService.java:41`).
  - 확인과 추가를 명령 두 번으로 나누면 그 틈에 승격 Lua가 끼어들 수 있어서 Lua 하나로 묶는다.
- score는 등록 시각(epoch ms)이다. 같은 밀리초 동점은 멤버 문자열 사전순으로 정렬된다(`"10"`이 `"9"`보다 앞). 영향은 작다.
- 접수 기간 판정은 2단계 복제본으로 한다.
- 키는 `.claude/rules/redis-key-convention.md`(콜론 계층, 키는 한곳에서 만든다)를 따른다. Lua는 member의 `RefreshTokenRepository` 방식(`resources/redis/*.lua` + `DefaultRedisScript`)을 따른다.
- ticketplay에서 바꿀 것: 인증(`@AuthenticationPrincipal` 아이디 → `X-Member-Id` 숫자), 경로(`/api/reservation/...` → `/api/v1/queue/...`), 계층 없는 `queues` 키
- [미정] 경로 · 키 이름
- [미정] active인 회원이 다시 등록할 때 응답: 지금 상태를 그대로 돌려주기 / 거부
- [미정] Redis 장애 때: 막기(fail-closed, 예매 중단) / 통과(fail-open, 주문 · 상품에 트래픽이 몰림)
- 확인: Testcontainers Redis로 등록 · 다른 기기 재등록(순번 유지) · active 회원 재등록 · 접수 기간 밖 등록

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
  - [제안] 만료된 active는 한 곳(승격 스케줄러)에서만 지운다. ticketplay는 스케줄러와 순번 조회 두 곳에서 지워서 만료 후처리(좌석 해제)가 건너뛰어졌다(`QueueScheduler.java:48-55`, `RegistrationService.java:81-82`). 6단계의 자리 해제 주체와 이어서 정한다.
- `@EnableScheduling`은 구현 때 넣는다(뼈대 때 Claude 기본값, 보고함). [제안] 스케줄러는 `@Profile("!test")`로 테스트에서 끈다(order · product 관례).
- [제안] 대기열(종목)마다 예외를 잡아 다음 종목을 계속 처리한다. ticketplay 루프에는 try/catch가 없어 하나가 실패하면 그 주기의 승격 전체가 멈췄다.
- [미정] 승격 규칙: 주기마다 N명(ticketplay: 3초마다 10명, 동시 active 상한 없음) / 동시 active 상한(비어 있는 active 칸만큼)
  - 상한을 두면 결제 완료 · 이탈 때 active에서 빼는 처리가 함께 필요하다(6단계).
- [미정] 방식: `@Scheduled` + UseCase(order · product 관례) / Spring Batch(file 관례, 짧은 주기면 `BATCH_` 테이블에 기록이 계속 쌓인다)
- [미정] 주기와 한 번에 올릴 인원
- [미정] 시각: `Clock` 빈 주입(member 방식) / Facade · 스케줄러가 `now`를 만들어 UseCase에 넘기기(order · product 방식). epoch ms와 `LocalDateTime`(상품 `expiresAt`)을 바꿀 때 시간대를 정한다.
- [미정] 이탈 API: 없으면 창을 닫은 회원도 순번을 유지한 채 승격돼 10분을 차지한다. 상한 방식이면 그만큼 다른 회원의 입장이 늦어진다.
- 확인: 고정 시각으로 UseCase를 직접 불러 승격 · 만료 정리 확인 (Testcontainers Redis)

### 6. 주문 연동 API

- 주문이 [결제하기] 때 "이 회원이 이 종목의 active인가"와 active 끝 시각(`expiresAt`)을 확인할 수단이다.
  - ticketplay는 한 앱 안에서 `registrationService.isActive(...)`를 직접 불렀다(`WorkController.java:60-62`). 결제가 끝나면 `leaveActive(...)`로 active에서 뺐다(`PaymentService.java:44-45`). 우리는 서비스가 나뉘어 있어 서비스 간 통신이 필요하다.
- 필요한 이유
  - 대기열 건너뛰기를 막는다. 주문 API는 게이트웨이로 누구나 부를 수 있고, 브라우저가 폴링으로 받은 결과는 믿을 수 없다.
  - 상품 자리 확보 API가 `expiresAt`(대기열 활성 시간이 끝나는 시각)을 필수로 받는다(`HoldRequestDto.java:27-31`). 지금 주문의 `SeatHoldPort.hold(raceCourseId, participationId, memberId)`에는 이 값이 없다(`SeatHoldPort.java:8`).
- [미정] 입장 여부와 `expiresAt`을 주문에 주는 방법. 고르는 안에 따라 이 단계의 모양이 바뀐다.
  - 주문이 대기열 내부 API(`/internal/v1/queue/...`)를 부른다 → 이 단계에서 내부 API를 만든다
  - 브라우저가 대기열에서 받은 값을 주문 요청에 실어 보낸다 → 대기열이 서명 토큰 등 위조할 수 없는 값을 발급한다
  - 게이트웨이가 주문 경로에서 검사한다 → 게이트웨이가 Redis를 보게 된다
- [미정] 이다윗 님과 정할 것
  - 참가 신청서 작성을 대기열 앞에서 할지 뒤에서 할지 (지금 신청서 API는 대기열 검사 없이 만들어진다, `ParticipationApplyUseCase.java:45-50`)
  - 장바구니 결제(한 요청에 참가 신청 최대 10건, `CartCheckoutRequestDto.java:18,23`. 항목마다 [결제하기]를 부름, `CartCheckoutUseCase.java:97`)를 대기열에 넣을지
  - active가 끝났을 때 자리 해제 주체. 10/13 결정은 "주문이 `product.hold.cancel` 발행"이지만, 주문에는 아직 발행 코드가 없고 만료 스케줄러는 1분 주기다(`OrderExpiryScheduler.java:23`).
  - 결제가 끝났을 때 active 반납 방법 (반납하지 않고 10분 만료에 맡김 / 결제 완료 메시지 / 내부 API)
  - 10분을 세는 기준. active 10분은 승격 순간부터 센다. 반면 `docs/erd/cash.md:10`은 "결제 기한 개인 10분(임시 확보부터)"이다.
    - 프론트 `CheckoutPage.tsx:85,187-193`은 결제 화면에 들어온 때부터 센다. 시작값은 시안 예시 522초이고, API 연결 전이다(`:80` TODO). 상수 `:76-77`의 주석은 "자리를 잡아 두는 시간(10분)"이다.
- 주문 코드는 수정하지 않는다. 주문 쪽 변경(입장 확인 호출, `expiresAt` 전달 등)은 이다윗 님 전달 목록에 넣는다.
- 확인: 대기열 쪽은 MockMvc 단독 테스트. 주문과 이어서 확인하는 일은 주문 반영 뒤에 한다.

## 남은 일 (단계 밖)

- Linear: 부모 PRO-111, 1단계 뼈대 PRO-172(In Progress). 2~6단계 이슈는 shrimp-task-manager로 작업을 나눈 뒤 만든다.
- 브랜치: `feat/PRO-172-queue-skeleton`(`1bc5a73`, `fcded5e`). 새 이슈 작성과 PR은 대기열 작업을 더 쌓은 뒤에 한다(사용자 10/14).
- 문서 맞추기: `docs/policy/party-lottery.md:16`과 `backend/product-service/ROADMAP.md:15`에는 대기열이 "최종"으로 적혀 있다. 10/13 결정(세미 = 개인 예매 + 대기열)에 맞춘다. `CONTEXT.md`에는 대기열 · 활성 시간 용어가 없다.
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
