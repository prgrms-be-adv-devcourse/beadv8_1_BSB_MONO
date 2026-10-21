// 판매자 승인 결과를 이 브라우저에서 이미 봤는지. 서버에 "봤음" 칸이 없어서 회원별로 localStorage에 남긴다.
// 키에는 회원 ID(내 정보 조회의 id)를 넣는다. 판매자 신청서 ID(SellerDto.id)와는 다른 값이다.
// 저장소를 못 쓰면(사생활 보호 모드 등) "아직 안 봄"으로 두어 결과 화면을 한 번 더 보여 주는 쪽으로 기운다.

const sellerApprovalSeenKey = (memberId: number) => `members:${memberId}:seller-approval-seen`

export function hasSeenSellerApproval(memberId: number): boolean {
  try {
    return window.localStorage.getItem(sellerApprovalSeenKey(memberId)) === 'true'
  } catch {
    return false
  }
}

export function markSellerApprovalSeen(memberId: number): void {
  try {
    window.localStorage.setItem(sellerApprovalSeenKey(memberId), 'true')
  } catch {
    // 못 남기면 다음에 결과 화면을 한 번 더 보여 줄 뿐이다
  }
}
