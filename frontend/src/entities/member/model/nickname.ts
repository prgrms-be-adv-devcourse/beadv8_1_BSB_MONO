// 닉네임 규칙. 백엔드 가입·수정 요청 DTO(@Pattern, @NicknameWidth)와 Member.nicknameWidth를 그대로 옮겼다.
// 칸 수 규칙은 커스텀 검증이라 OpenAPI 명세에 나오지 않아 orval zod에 없다.

/** 한글 · 영문 · 숫자만 (백엔드 @Pattern) */
export const NICKNAME_PATTERN = /^[가-힣A-Za-z0-9]+$/

/** 칸 수 범위. 한글 2자 · 영문 4자 ~ 한글 8자 · 영문 16자 (백엔드 Member.NICKNAME_MIN_WIDTH/MAX_WIDTH) */
export const NICKNAME_MIN_WIDTH = 4
export const NICKNAME_MAX_WIDTH = 16

/** 한글 완성형(가~힣)은 2칸, 그 밖의 글자는 1칸으로 센다 ("러너abc" = 7칸) */
export function nicknameWidth(nickname: string): number {
  return [...nickname].reduce((width, char) => width + (char >= '가' && char <= '힣' ? 2 : 1), 0)
}

export function isNicknameWidthValid(nickname: string): boolean {
  const width = nicknameWidth(nickname)
  return width >= NICKNAME_MIN_WIDTH && width <= NICKNAME_MAX_WIDTH
}
