'use client'

import { useRouter } from 'next/navigation'

import { AppHeader } from '@/widgets/app-header'
import { BottomCta, Button } from '@/shared/ui'

// TODO(content): 실제 약관은 법률 검토 뒤 넣는다. 지금은 시안 예시 문구.
const TERMS = {
  service: {
    title: '서비스 이용약관',
    heading: '[필수] 크루런 서비스 이용약관',
    articles: [
      [
        '제1조 (목적)',
        '이 약관은 크루런(이하 "회사")이 제공하는 러닝 대회 참가권 · 굿즈 거래 서비스를 이용할 때 회사와 회원의 권리, 의무, 책임을 정해요.',
      ],
      [
        '제2조 (용어)',
        '"회원"은 이 약관에 동의하고 가입한 사람이에요. "판매자"는 회사의 승인을 받아 대회와 참가권을 등록하는 회원이에요. "참가권"은 대회에 참가할 수 있는 권리예요.',
      ],
      [
        '제3조 (약관의 효력과 변경)',
        '회사는 약관을 바꿀 때 시행일 7일 전부터 서비스 안에 알려요. 회원에게 불리한 변경은 30일 전부터 알려요.',
      ],
      [
        '제4조 (참가권 신청과 취소)',
        '참가권 신청, 환불, 취소는 대회별 환불 기한과 정책 3장 · 5장을 따라요.',
      ],
      [
        '제5조 (회원 탈퇴)',
        '회원은 언제든 마이 > 설정에서 탈퇴할 수 있어요. 진행 중인 거래가 있으면 끝난 뒤에 탈퇴할 수 있어요.',
      ],
    ],
  },
  privacy: {
    title: '개인정보 수집 · 이용',
    heading: '[필수] 개인정보 수집 · 이용',
    articles: [
      ['수집 항목', '이메일, 비밀번호, 이름, 생년월일, 휴대폰 번호, 닉네임'],
      ['이용 목적', '회원 식별, 대회 참가 신청, 거래 안내'],
      [
        '보유 기간',
        '회원 탈퇴 시까지. 법령에 따라 보관해야 하는 정보는 정해진 기간 동안 보관해요.',
      ],
    ],
  },
  marketing: {
    title: '마케팅 정보 받기',
    heading: '[선택] 마케팅 정보 받기',
    articles: [
      ['받는 정보', '새 대회 소식, 할인 · 이벤트 안내'],
      ['동의 기간', '2년마다 다시 여쭤봐요. 마이에서 언제든 끌 수 있어요.'],
    ],
  },
} as const

export type TermsSlug = keyof typeof TERMS

export const TERMS_SLUGS = Object.keys(TERMS) as TermsSlug[]

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
