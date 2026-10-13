'use client'

import { useRouter } from 'next/navigation'

import { AppHeader } from '@/widgets/app-header'
import { BottomCta, Button } from '@/shared/ui'

import { TERMS, type TermsSlug } from '../model/terms'

// 피그마 V2 · M04-T 약관 전문
export function TermsPage({ slug }: { slug: TermsSlug }) {
  const router = useRouter()
  const terms = TERMS[slug]
  return (
    <>
      <AppHeader type="sub" title={terms.title} />
      <main className="flex flex-1 flex-col gap-5 px-4 py-6">
        <div className="flex flex-col gap-1">
          <h2 className="text-heading-l">{terms.heading}</h2>
          <p className="text-caption text-text-tertiary">
            시행일 2026.11.01 · 최종 개정 2026.10.10
          </p>
        </div>
        {terms.articles.map(([title, body]) => (
          <section key={title} className="flex flex-col gap-1.5">
            <h3 className="text-heading-s">{title}</h3>
            <p className="text-body-m text-text-secondary">{body}</p>
          </section>
        ))}
        <p className="text-caption text-text-tertiary">
          ※ 시안용 예시 문구예요. 실제 약관은 법률 검토 뒤 넣어요.
        </p>
      </main>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Button size="l" onClick={() => router.back()}>
            동의하기
          </Button>
        }
      />
    </>
  )
}
