import { MobileColumn } from '../_shell/MobileColumn'

// 회원 화면: PC에서도 가운데 480 기둥 안에 모바일 UI (헤더·하단 탭은 화면마다 views에서 붙인다)
export default function ShopLayout({ children }: LayoutProps<'/'>) {
  return <MobileColumn>{children}</MobileColumn>
}
