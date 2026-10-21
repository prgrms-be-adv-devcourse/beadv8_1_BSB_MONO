// 운영(또는 다른) 서버에 데모 대회 8개를 판매자 대회 등록 API로 넣는다.
// DB에 직접 넣지 않고 API를 부르는 이유: 이미지 연결(Kafka 파일 연결 메시지)과 대기열의 대회 복제본 같은
// 서비스 사이 흐름이 실제 등록과 똑같이 돌아야 하기 때문이다.
//
// 필요한 계정: 관리자가 승인한 판매자 계정 (승인 전이면 대회 등록이 403으로 막힌다)
// 필요한 것: Node 20 이상 (내장 fetch만 쓰고, 설치할 패키지는 없다)
//
// 사용법 (frontend/ 폴더에서):
//   # 1) 보낼 순서와 본문만 확인 (네트워크 요청 없음, 계정 불필요)
//   node scripts/seed-demo-races/index.mjs --dry-run
//
//   # 2) 실제로 넣기. 비밀번호가 셸 기록에 남지 않게 read -s로 받는 것을 권한다
//   read -s SELLER_PASSWORD && export SELLER_PASSWORD
//   SELLER_EMAIL=seller@example.com node scripts/seed-demo-races/index.mjs
//
//   # 다른 서버에 넣을 때
//   BASE_URL=http://localhost:8000 SELLER_EMAIL=... node scripts/seed-demo-races/index.mjs
//
// 환경 변수:
//   BASE_URL         게이트웨이 주소 (기본 https://crewrun.duckdns.org)
//   SELLER_EMAIL     판매자 계정 이메일
//   SELLER_PASSWORD  판매자 계정 비밀번호 (로그·파일에 남기지 않는다)
//
// 대회마다: 대표 이미지 올리기(발급 → S3 PUT → 완료 확인)를 종목 수만큼 → 대회 등록
// 다시 돌려도 중복 등록되지 않는다: Idempotency-Key를 대회마다 seed-demo-<slug>로 고정해,
// 서버가 같은 판매자의 같은 키를 보면 새로 만들지 않고 처음 대회를 200으로 돌려준다.
// (다시 돌리면 이미지는 새로 올라가지만 어디에도 연결되지 않아, 파일 정리 배치가 지운다)
//
// 날짜: 접수 시작 · 접수 마감 · 대회 일시를 모두 races.mjs에 2027년 3~6월로 고정한다.
// 서버는 접수 시작이 실행일 + 2일 이후여야 받는다. 실행이 그보다 늦어 어긋나면 등록 전에 이 스크립트가 막는다.

import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

import { buildRaceRequest, idempotencyKeyOf, validateRaceRequest } from './build-request.mjs'
import { DEMO_RACES } from './races.mjs'

const IMAGE_DIR = new URL('../../public/mock/races/', import.meta.url)

const dryRun = process.argv.includes('--dry-run')
const baseUrl = (process.env.BASE_URL || 'https://crewrun.duckdns.org').replace(/\/+$/, '')
const now = Date.now()

/** 서버 응답(RsData)을 읽는다. JSON이 아니면 본문 앞부분을 메시지로 쓴다 */
async function readBody(response) {
  const text = await response.text()
  try {
    return JSON.parse(text)
  } catch {
    return { message: text.slice(0, 200) }
  }
}

async function api(path, { token, method = 'POST', body, headers = {} }) {
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const json = await readBody(response)
  if (!response.ok) {
    throw new Error(`${method} ${path} → ${response.status}: ${json.message ?? '(메시지 없음)'}`)
  }
  return { status: response.status, data: json.data }
}

async function login(email, password) {
  const { data } = await api('/api/v1/auth/login', { body: { email, password } })
  if (!data?.accessToken) throw new Error('로그인 응답에 accessToken이 없습니다')
  return data.accessToken
}

// 브라우저와 Node가 직접 정하는 헤더는 빼고, 나머지 서명된 헤더는 그대로 붙인다 (upload-file.ts와 같다)
const FORBIDDEN_HEADERS = new Set(['content-length', 'host'])

/** 이미지 하나를 올리고 파일 ID를 돌려준다: 발급 → S3 PUT → 완료 확인 */
async function uploadImage(token, imageName) {
  const bytes = readFileSync(fileURLToPath(new URL(imageName, IMAGE_DIR)))
  const { data: issued } = await api('/api/v1/file/files', {
    token,
    body: {
      fileType: 'THUMBNAIL',
      fileName: imageName,
      contentType: 'image/webp',
      fileSize: bytes.length,
    },
  })
  if (!issued?.fileId || !issued.uploadUrl) throw new Error('업로드 주소를 받지 못했습니다')

  const headers = Object.fromEntries(
    Object.entries(issued.headers ?? {}).filter(
      ([name]) => !FORBIDDEN_HEADERS.has(name.toLowerCase()),
    ),
  )
  const put = await fetch(issued.uploadUrl, {
    method: issued.method ?? 'PUT',
    headers,
    body: bytes,
  })
  if (!put.ok) throw new Error(`S3 업로드 실패 (${put.status})`)

  await api(`/api/v1/file/files/${issued.fileId}/complete`, { token })
  return issued.fileId
}

async function seedRace(token, race) {
  const fileIds = []
  for (const [i, course] of race.courses.entries()) {
    const fileId = await uploadImage(token, race.image)
    console.log(
      `  이미지 올림: ${course.name} 종목 → fileId ${fileId} (${i + 1}/${race.courses.length})`,
    )
    fileIds.push(fileId)
  }
  const request = buildRaceRequest(race, { now, fileIds })
  const errors = validateRaceRequest(request, { now })
  if (errors.length > 0)
    throw new Error(`요청 값이 서버 규칙에 맞지 않습니다: ${errors.join(', ')}`)

  const { status, data } = await api('/api/v1/product/races', {
    token,
    body: request,
    headers: { 'Idempotency-Key': idempotencyKeyOf(race) },
  })
  return { raceId: data?.raceId, created: status === 201 }
}

function printDryRun() {
  console.log(`[드라이런] 요청을 보내지 않습니다. 대상 서버: ${baseUrl}\n`)
  console.log('1. POST /api/v1/auth/login  { email: $SELLER_EMAIL, password: (숨김) }\n')
  for (const [n, race] of DEMO_RACES.entries()) {
    const request = buildRaceRequest(race, { now })
    const errors = validateRaceRequest(request, { now, requireFiles: false })
    console.log(`── ${n + 1}/${DEMO_RACES.length} ${race.title}`)
    for (const course of race.courses) {
      console.log(
        `  POST /api/v1/file/files  { fileType: THUMBNAIL, fileName: ${race.image}, contentType: image/webp }`,
      )
      console.log('  PUT  <발급받은 S3 주소>  (발급 응답의 서명된 헤더 그대로)')
      console.log(`  POST /api/v1/file/files/<fileId>/complete   ← ${course.name} 종목 대표 이미지`)
    }
    console.log(`  POST /api/v1/product/races  Idempotency-Key: ${idempotencyKeyOf(race)}`)
    console.log('  (images[].fileId 0은 실제 실행 때 위에서 받은 파일 ID로 채운다)')
    console.log(JSON.stringify(request, null, 2).replace(/^/gm, '  '))
    console.log(
      errors.length === 0 ? '  규칙 검사: 통과\n' : `  규칙 검사: 실패 — ${errors.join(', ')}\n`,
    )
  }
}

async function main() {
  if (dryRun) {
    printDryRun()
    return
  }

  const email = process.env.SELLER_EMAIL
  const password = process.env.SELLER_PASSWORD
  if (!email || !password) {
    console.error(
      'SELLER_EMAIL과 SELLER_PASSWORD 환경 변수가 필요합니다. (--dry-run은 없어도 됩니다)',
    )
    process.exitCode = 1
    return
  }

  // 로그인 · 이미지 업로드 전에 날짜 · 값이 서버 규칙에 맞는지 먼저 본다
  const problems = DEMO_RACES.flatMap((race) =>
    validateRaceRequest(buildRaceRequest(race, { now }), { now, requireFiles: false }).map(
      (error) => `${race.title}: ${error}`,
    ),
  )
  if (problems.length > 0) {
    console.error(`서버 규칙에 맞지 않아 등록하지 않습니다:\n${problems.join('\n')}`)
    process.exitCode = 1
    return
  }

  console.log(`대상 서버: ${baseUrl}`)
  let token
  try {
    token = await login(email, password)
    console.log(`로그인 성공: ${email}\n`)
  } catch (error) {
    console.error(`로그인 실패: ${error.message}`)
    process.exitCode = 1
    return
  }

  const results = []
  for (const race of DEMO_RACES) {
    console.log(`▶ ${race.title}`)
    try {
      const { raceId, created } = await seedRace(token, race)
      const result = created
        ? `새로 등록 (raceId ${raceId})`
        : `이미 등록됨 (raceId ${raceId}, 새로 만들지 않음)`
      results.push({ title: race.title, ok: true, result })
    } catch (error) {
      // 한 대회가 실패해도 나머지는 계속한다
      results.push({ title: race.title, ok: false, result: error.message })
    }
    console.log(`  ${results.at(-1).ok ? '성공' : '실패'}: ${results.at(-1).result}\n`)
  }

  console.log('── 결과 요약')
  for (const { title, ok, result } of results) {
    console.log(`${ok ? '성공' : '실패'}  ${title}  ${result}`)
  }
  const failed = results.filter((result) => !result.ok).length
  console.log(`\n성공 ${results.length - failed}개, 실패 ${failed}개`)
  if (failed > 0) process.exitCode = 1
}

await main()
