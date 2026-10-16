import { RaceQueuePage } from '@/views/race-queue'

// 대기열 · 차례 기다리기(M05-Q1). /races/1/queue?option=1 처럼 줄을 선 종목을 붙여 온다.
// ?preview=admitted 로 M05-Q2(내 차례)를 시안의 남은 시간(09:43)으로 볼 수 있다.
// TODO(api): 입장 여부는 다음 단계에서 화면(클라이언트)이 대기열 API로 묻는다.
const PREVIEW_REMAINING_SECONDS = 583

export default async function Page({ params, searchParams }: PageProps<'/races/[raceId]/queue'>) {
  const [{ raceId }, { option, preview }] = await Promise.all([params, searchParams])
  return (
    <RaceQueuePage
      raceId={raceId}
      optionId={typeof option === 'string' ? option : undefined}
      admission={
        preview === 'admitted'
          ? { admitted: true, admissionEndsAt: null, remainingSeconds: PREVIEW_REMAINING_SECONDS }
          : undefined
      }
    />
  )
}
