import { asRsData } from './rs-data'

export class ApiError extends Error {
  /** 서버가 RsData로 보낸 문구. 화면에 그대로 보여 줄 수 있다. RsData가 아니면 undefined */
  readonly serverMessage?: string
  /** 서버 오류 코드 (PRO-124 이후) */
  readonly code?: string

  constructor(
    readonly status: number,
    readonly body: unknown,
    readonly url: string,
  ) {
    super(`API ${status}: ${url}`)
    this.name = 'ApiError'
    const rsData = asRsData(body)
    this.serverMessage = rsData?.message
    this.code = rsData?.code
  }
}
