import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

import { renderTokensCss } from './render.mjs'

const figma = JSON.parse(readFileSync('design-tokens/figma-variables.json', 'utf8'))
const css = renderTokensCss(figma)

describe('renderTokensCss', () => {
  it('커밋된 tokens.css는 피그마 변수 JSON으로 다시 만든 결과와 같다 (손으로 고치지 않았다)', () => {
    expect(readFileSync('src/app/tokens.css', 'utf8')).toBe(css)
  })

  it('원색 · 의미 색 · 그림자 · 글자를 피그마 이름 그대로 옮긴다', () => {
    expect(css).toContain('  --color-brand-500: #0057ff;')
    expect(css).toContain('  --color-bg-surface: var(--color-neutral-0);')
    expect(css).toContain('    --color-bg-surface: var(--color-navy-900);')
    expect(css).toContain('  --shadow-sm: 0 1px 2px 0 #14213d0f, 0 1px 3px 0 #14213d14;')
    expect(css).toContain('  --text-heading-xl--letter-spacing: -0.025em;')
  })

  it('화면 폭에 따라 바뀌는 값은 앞 단계와 다를 때만 적는다', () => {
    const tablet = css.slice(
      css.indexOf('@media (width >= 48rem)'),
      css.indexOf('@media (width >= 80rem)'),
    )
    expect(tablet).toContain('--text-heading-xl: 32px;')
    // heading-m은 Mobile과 Tablet이 20px로 같다
    expect(tablet).not.toContain('--text-heading-m:')
  })

  it('max-width 9999는 제한 없음(none)으로 쓴다', () => {
    expect(css).toContain('    --layout-form-max-width: none;')
  })
})
