// PR 본문 양식 검사 (docs/LINEAR.md "작업 흐름")
// 본문에 .github/pull_request_template.md의 섹션 제목이 모두 있는지 본다. 내용이 채워졌는지는 보지 않는다.

const TEMPLATE_PATH = '.github/pull_request_template.md'
const SECTIONS = ['배경', '의도', '구현', '중점 리뷰 사항', '확인 방법', '기타']

// 줄 시작의 '## 제목'만 섹션으로 친다. 뒤 공백은 허용한다.
function findMissingSections(body) {
  const headings = new Set(
    (body ?? '')
      .split(/\r?\n/)
      .map((line) => line.match(/^##\s+(.+?)\s*$/))
      .filter(Boolean)
      .map((m) => m[1]),
  )
  return SECTIONS.filter((name) => !headings.has(name))
}

// 봇 PR, dev-main-sync 워크플로가 만드는 dev→main 동기화 PR은 검사하지 않는다
function skipReason(pr) {
  if (pr.user?.type === 'Bot') {
    return `봇(${pr.user.login})이 만든 PR`
  }
  if (pr.head.ref === 'dev' && pr.base.ref === 'main') {
    return 'dev→main 동기화 PR'
  }
  return null
}

module.exports = async ({ context, core }) => {
  // 본문은 사용자 입력이라 스크립트에 끼워 넣지 않고 이벤트 payload에서 읽는다
  const pr = context.payload.pull_request
  const reason = skipReason(pr)
  if (reason) {
    core.notice(`${reason}이라 본문 검사를 건너뜁니다.`)
    return
  }
  if (!pr.body || pr.body.trim() === '') {
    core.setFailed(`PR 본문이 비어 있습니다. ${TEMPLATE_PATH} 양식으로 채워 주세요.`)
    return
  }
  const missing = findMissingSections(pr.body)
  if (missing.length > 0) {
    core.setFailed(
      `PR 본문에 빠진 섹션이 있습니다: ${missing.map((s) => `'## ${s}'`).join(', ')}. ` +
        `${TEMPLATE_PATH} 양식대로 섹션을 모두 넣어 주세요.`,
    )
  }
}

module.exports.SECTIONS = SECTIONS
module.exports.findMissingSections = findMissingSections
module.exports.skipReason = skipReason
