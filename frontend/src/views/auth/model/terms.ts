// 약관 문구. 서버의 generateStaticParams도 쓰므로 'use client' 파일과 분리한다.
// 근거는 레포에 있는 것만 쓴다: 회원 · 약관 코드(backend/member-service Member · TermsDataInit · MemberWithdrawUseCase),
// 가입 요청(MemberJoinRequestDto), 참가 신청(ParticipationApplyRequestDto), docs/policy/listing-gaps.md.
// 정책 문서에 없는 법률 조항(보유 기간 법령 근거, 마케팅 수신 방법 · 철회, 약관 변경 고지 기간 등)은 넣지 않는다.
// TODO(content): 위 항목은 정책이 정해지면 채운다.
export const TERMS = {
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
      ['제3조 (참가권 신청과 취소)', '참가권 신청, 환불, 취소는 대회마다 정한 환불 기한을 따라요.'],
      [
        '제4조 (회원 탈퇴)',
        '회원은 마이 > 설정에서 탈퇴할 수 있어요. 남은 예치금이 있거나 끝나지 않은 참가권이 있으면 탈퇴할 수 없어요. 예치금을 모두 쓰거나 출금하고, 대회가 끝나거나 참가권을 취소한 뒤에 탈퇴해 주세요.',
      ],
    ],
  },
  privacy: {
    title: '개인정보 수집 · 이용',
    heading: '[필수] 개인정보 수집 · 이용',
    articles: [
      [
        '수집 항목',
        '이메일, 비밀번호, 이름, 생년월일, 휴대폰 번호, 닉네임을 받아요. 성별은 선택으로 받을 수 있어요.',
      ],
      ['이용 목적', '회원 식별과 로그인, 대회 참가 신청, 예치금 · 결제 등 거래 안내에 써요.'],
      [
        '보유 기간',
        '회원 탈퇴 시까지 보관해요. 탈퇴하면 이메일, 이름, 닉네임, 휴대폰 번호, 생년월일을 지워요. 주문 · 결제 기록은 남기되 회원의 닉네임은 "탈퇴한 회원"으로 보여요.',
      ],
      [
        '제3자 제공',
        '대회에 참가 신청할 때 대회를 연 판매자(주최자)에게 참가자 정보를 넘기는 것은 신청 단계에서 따로 동의를 받아요. 동의하지 않으면 신청할 수 없어요.',
      ],
    ],
  },
  marketing: {
    title: '마케팅 정보 받기',
    heading: '[선택] 마케팅 정보 받기',
    articles: [
      ['동의 여부', '선택 항목이에요. 동의하지 않아도 가입하고 서비스를 이용할 수 있어요.'],
    ],
  },
} as const

/** 약관 데이터(member_terms)의 기본 시행일과 버전. TermsDataInit의 값이다. 최종 개정일은 정해진 값이 없어 보여 주지 않는다. */
export const TERMS_EFFECTIVE_DATE = '2026.10.01'
export const TERMS_VERSION = '1.0'

export type TermsSlug = keyof typeof TERMS

export const TERMS_SLUGS = Object.keys(TERMS) as TermsSlug[]
