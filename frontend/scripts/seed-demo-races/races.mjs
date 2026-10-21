// 운영 서버에 넣을 데모 대회 8개.
// 이름 · 장소 · 종목 · 참가비 · 정원 · 이미지는 프론트 목(src/shared/mocks/races.ts)과 같다.
// 날짜는 목과 달리 달력 날짜로 고정한다: 접수 시작 · 접수 마감 · 대회 일시 모두 2027년 3~6월 안에 있고,
// 접수 시작 < 접수 마감 < 대회 일시(주말 오전) 순서다. 접수 시작은 서버 규칙(실행일 + 2일 이후)을 지나야 등록된다.
//
// slug는 Idempotency-Key(seed-demo-<slug>)에 쓰므로 한 번 운영에 넣은 뒤에는 바꾸지 않는다.

/** 종목 구분별 거리(km). 서버는 소수 둘째 자리까지만 받아 하프 21.0975 → 21.1, 풀 42.195 → 42.2로 반올림했다 */
export const DISTANCE_KM = {
  KM_5: 5,
  KM_10: 10,
  HALF: 21.1,
  FULL: 42.2,
  ULTRA: 50,
}

/** 종목 구분별 제한 시간(분). 목과 같다 */
export const TIME_LIMIT_MINUTES = {
  KM_5: 60,
  KM_10: 90,
  HALF: 180,
  FULL: 360,
  ULTRA: 600,
}

export const DEMO_RACES = [
  {
    slug: 'cookierun-seoul',
    title: '2026 용감한 쿠키RUN in 서울',
    description: '서울숲에서 달리는 가을 러닝 대회',
    location: '서울숲 일대',
    regionSido: '서울특별시',
    regionSigungu: '성동구',
    applicationOpenAt: '2027-03-02T00:00:00',
    applicationCloseAt: '2027-03-19T18:00:00',
    raceDate: '2027-03-27T08:00:00',
    image: 'cookierun.webp',
    courses: [
      { name: '10K', courseType: 'KM_10', price: 45000, quota: 600 },
      { name: '5K', courseType: 'KM_5', price: 35000, quota: 500 },
    ],
  },
  {
    slug: 'youth-10k',
    title: '긍정 하프 마라톤 YOUTH 10K',
    description: '올림픽공원을 도는 10K 대회. 접수가 곧 끝나요',
    location: '올림픽공원 평화의 광장',
    regionSido: '서울특별시',
    regionSigungu: '송파구',
    applicationOpenAt: '2027-03-08T00:00:00',
    applicationCloseAt: '2027-04-02T18:00:00',
    raceDate: '2027-04-10T08:00:00',
    image: 'youth10k.webp',
    courses: [{ name: '10K', courseType: 'KM_10', price: 40000, quota: 1000 }],
  },
  {
    slug: 'seoul-marathon',
    title: '2026 서울마라톤',
    description: '광화문에서 출발해 잠실로 들어오는 국내 최고 역사의 마라톤',
    location: '광화문광장',
    regionSido: '서울특별시',
    regionSigungu: '종로구',
    applicationOpenAt: '2027-03-01T00:00:00',
    applicationCloseAt: '2027-03-12T18:00:00',
    raceDate: '2027-03-21T08:00:00',
    image: 'seoul-international-marathon.webp',
    courses: [
      { name: '10K', courseType: 'KM_10', price: 50000, quota: 2000 },
      { name: '풀', courseType: 'FULL', price: 90000, quota: 1500 },
    ],
  },
  {
    slug: 'jtbc-seoul-marathon',
    title: 'JTBC 서울 마라톤',
    description: '잠실종합운동장에서 출발해 서울 도심을 달리는 풀코스 대회',
    location: '잠실종합운동장',
    regionSido: '서울특별시',
    regionSigungu: '송파구',
    applicationOpenAt: '2027-04-05T00:00:00',
    applicationCloseAt: '2027-05-07T18:00:00',
    raceDate: '2027-05-16T08:00:00',
    image: 'jtbc.webp',
    courses: [
      { name: '풀', courseType: 'FULL', price: 90000, quota: 3000 },
      { name: '하프', courseType: 'HALF', price: 70000, quota: 3000 },
      { name: '10K', courseType: 'KM_10', price: 55000, quota: 2000 },
    ],
  },
  {
    slug: 'busan-beach',
    title: '부산바다마라톤',
    description: '광안대교를 바라보며 달리는 부산의 해변 마라톤',
    location: '광안리 해수욕장',
    regionSido: '부산광역시',
    regionSigungu: '수영구',
    applicationOpenAt: '2027-04-12T00:00:00',
    applicationCloseAt: '2027-05-14T18:00:00',
    raceDate: '2027-05-23T08:00:00',
    image: 'busan-beach-run.webp',
    courses: [
      { name: '5K', courseType: 'KM_5', price: 30000, quota: 800 },
      { name: '10K', courseType: 'KM_10', price: 38000, quota: 800 },
    ],
  },
  {
    slug: 'jeju-tangerine',
    title: '제주감귤국제마라톤',
    description: '감귤 밭 사이를 달리는 제주 대표 마라톤',
    location: '조천운동장 일대',
    regionSido: '제주특별자치도',
    regionSigungu: '제주시',
    applicationOpenAt: '2027-05-03T00:00:00',
    applicationCloseAt: '2027-05-28T18:00:00',
    raceDate: '2027-06-06T08:00:00',
    image: 'jeju-trail-run.webp',
    courses: [
      { name: '10K', courseType: 'KM_10', price: 48000, quota: 400 },
      { name: '하프', courseType: 'HALF', price: 62000, quota: 300 },
    ],
  },
  {
    slug: 'daegu-marathon',
    title: '대구마라톤',
    // 목은 "곧 접수를 시작해요"(오픈 예정)였지만, 데모에서는 문구를 뺐다
    description: '대구스타디움을 출발해 도심을 달리는 마라톤',
    location: '대구스타디움',
    regionSido: '대구광역시',
    regionSigungu: '수성구',
    applicationOpenAt: '2027-05-10T00:00:00',
    applicationCloseAt: '2027-06-04T18:00:00',
    raceDate: '2027-06-12T08:00:00',
    image: 'daegu-marathon.webp',
    courses: [
      { name: '풀', courseType: 'FULL', price: 90000, quota: 700 },
      { name: '10K', courseType: 'KM_10', price: 38000, quota: 700 },
    ],
  },
  {
    slug: 'chuncheon-marathon',
    title: '춘천마라톤',
    // 목은 "접수가 끝났어요"(마감)였지만, 데모에서는 문구를 뺐다
    description: '의암호를 따라 달리는 가을 마라톤',
    location: '공지천 일대',
    regionSido: '강원특별자치도',
    regionSigungu: '춘천시',
    applicationOpenAt: '2027-05-24T00:00:00',
    applicationCloseAt: '2027-06-17T18:00:00',
    raceDate: '2027-06-26T08:00:00',
    image: 'chuncheon-lake-run.webp',
    courses: [
      { name: '풀', courseType: 'FULL', price: 80000, quota: 500 },
      { name: '10K', courseType: 'KM_10', price: 32000, quota: 500 },
    ],
  },
]
