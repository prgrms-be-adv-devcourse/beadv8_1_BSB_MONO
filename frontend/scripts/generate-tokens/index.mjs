// 피그마 변수 JSON으로 웹 토큰 CSS를 만든다.
// 사용법 (frontend/ 폴더에서):
//   pnpm tokens                      # src/app/tokens.css를 다시 만든다
//   pnpm tokens --check              # 다시 만들었을 때 바뀌는 곳이 있으면 실패한다
//   pnpm tokens --check-hash <hash>  # JSON이 피그마에서 받은 값과 같은지 확인한다 (design-tokens/figma-export.js의 hash)
import { readFileSync, writeFileSync } from 'node:fs'

import { hashOf, renderTokensCss } from './render.mjs'

const SOURCE = 'design-tokens/figma-variables.json'
const OUTPUT = 'src/app/tokens.css'

const figma = JSON.parse(readFileSync(SOURCE, 'utf8'))
const args = process.argv.slice(2)

const hashIndex = args.indexOf('--check-hash')
if (hashIndex !== -1) {
  const expected = args[hashIndex + 1]
  const actual = hashOf(figma)
  if (actual !== expected) {
    console.error(`${SOURCE}가 피그마에서 받은 값과 다릅니다 (hash ${actual}, 기대 ${expected}).`)
    process.exit(1)
  }
  console.log(`ok   ${SOURCE} hash ${actual}`)
}

const css = renderTokensCss(figma)
if (args.includes('--check')) {
  if (readFileSync(OUTPUT, 'utf8') !== css) {
    console.error(`${OUTPUT}가 ${SOURCE}와 다릅니다. pnpm tokens로 다시 만드세요.`)
    process.exit(1)
  }
  console.log(`ok   ${OUTPUT}는 ${SOURCE}와 같습니다`)
} else {
  writeFileSync(OUTPUT, css)
  console.log(`ok   ${SOURCE} → ${OUTPUT}`)
}
