import { http, HttpResponse } from 'msw'

import { freshMemberOf } from './mock-member'

/**
 * 알림 목. 알림 API는 백엔드에 아직 없어서 명세(openapi)도 생성 코드도 없다. 가짜 응답 모드에서만 이 응답이 있다.
 * 시연용 기존 회원(runner1)은 시안의 알림 4개를, 방금 가입한 회원은 알림이 없다.
 */
export const MOCK_NOTIFICATIONS = [
  {
    id: 'n1',
    icon: 'Calendar',
    title: '예치금을 돌려받았어요',
    body: '₩5,000을 국민은행 계좌로 보냈어요.\n예치금 내역에서 확인할 수 있어요.',
    at: '방금',
    group: 'today',
    href: '/me/deposit',
    unread: true,
  },
  {
    id: 'n2',
    icon: 'Calendar',
    title: '대회 일정이 바뀌었어요',
    body: '춘천 나이트런 · 12/6 → 12/13.\n10/12까지 전액 환불할 수 있어요.',
    at: '5시간 전',
    group: 'today',
    href: '/tickets/R-20261002-0388/notice',
    unread: true,
  },
  {
    id: 'n3',
    icon: 'CircleCheck',
    title: '대회가 취소됐어요',
    body: '한강 나이트런 10K · 참가비 전액\n₩45,000을 예치금으로 돌려드렸어요.',
    at: '10/5',
    group: 'earlier',
    href: '/tickets',
    unread: false,
  },
  {
    id: 'n4',
    icon: 'CircleCheck',
    title: '환불이 끝났어요',
    body: '가을 트레일 21K ·\n₩60,000이 예치금으로 들어왔어요.',
    at: '9/30',
    group: 'earlier',
    href: '/me/deposit',
    unread: false,
  },
]

export const notificationHandlers = [
  http.get('*/api/v1/notification/notifications', ({ cookies }) =>
    HttpResponse.json({
      status: 200,
      message: '알림을 조회했습니다.',
      data: freshMemberOf(cookies) ? [] : MOCK_NOTIFICATIONS,
    }),
  ),
]
