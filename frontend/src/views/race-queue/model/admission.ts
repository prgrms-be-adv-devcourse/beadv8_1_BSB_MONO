/** GET /api/v1/order/queue/admission?raceId&raceCourseId 의 data */
export type QueueAdmission = {
  admitted: boolean
  /** 입장 끝 시각(= 결제 기한). 입장 전이면 null */
  admissionEndsAt: string | null
  /** 서버가 센 남은 초 */
  remainingSeconds: number
}
