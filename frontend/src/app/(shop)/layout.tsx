import { BookingFlowProvider } from '@/features/booking'

import { MobileColumn } from '../_shell/MobileColumn'

// 회원 화면: PC에서도 가운데 480 기둥 안에 모바일 UI (헤더·하단 탭은 화면마다 views에서 붙인다)
// 예매 흐름 스토어는 참가 신청(/races/[id]/apply) · 장바구니 · 결제(/checkout)가 함께 쓰므로 (shop) 레이아웃에 둔다.
// 레이아웃은 하위 페이지를 오갈 때 다시 그려지지 않으므로 단계 사이에 값이 남고, (shop)을 벗어나면 사라진다.
export default function ShopLayout({ children }: LayoutProps<'/'>) {
  return (
    <MobileColumn>
      <BookingFlowProvider>{children}</BookingFlowProvider>
    </MobileColumn>
  )
}
