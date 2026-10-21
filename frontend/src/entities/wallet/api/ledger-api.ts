import { infiniteQueryOptions } from '@tanstack/react-query'

import { cashApi, cashModel, unwrapData } from '@/shared/api'

export type LedgerCategory = cashModel.GetLedgersCategory
export const LedgerCategory = cashModel.GetLedgersCategory
export type WalletLedger = cashModel.WalletLedgerItemDto

export const LEDGER_PAGE_SIZE = 20

function ledgerParams(
  category: LedgerCategory | undefined,
  page: number,
): cashModel.GetLedgersParams {
  // 전체 탭은 category를 보내지 않는다 (서버가 모든 분류를 돌려준다)
  return { ...(category ? { category } : {}), page, size: LEDGER_PAGE_SIZE }
}

/**
 * 내 예치금 내역(GET /api/v1/cash/wallets/ledgers, 최신순). 더 보기로 다음 페이지를 붙인다.
 * 로그인 상태가 확인된 뒤에만 부르도록 enabled는 쓰는 쪽에서 정한다.
 */
export function walletLedgersQueryOptions(category?: LedgerCategory) {
  return infiniteQueryOptions({
    queryKey: [...cashApi.getGetLedgersQueryKey(ledgerParams(category, 0)), 'infinite'],
    queryFn: async ({ pageParam, signal }) =>
      unwrapData(await cashApi.getLedgers(ledgerParams(category, pageParam), { signal })),
    initialPageParam: 0,
    getNextPageParam: (last) => {
      const next = last.page + 1
      return next < last.totalPages ? next : undefined
    },
  })
}
