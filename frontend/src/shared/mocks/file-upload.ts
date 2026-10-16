import { http, HttpResponse } from 'msw'

import { getCreateUploadUrlResponseMock } from '@/shared/api/generated/file/file.msw'

/** 목 S3 주소. 브라우저에서도 /api 라우트(MSW)로 들어오게 같은 출처의 상대 경로로 둔다. */
const MOCK_S3_PATH = '/api/mock-s3/files'

let lastFileId = 100

/**
 * 파일 업로드 목. 생성된 핸들러보다 앞에 둬서 먼저 걸리게 한다.
 * 명세 예시의 업로드 URL은 실제 S3 주소라 목에서는 받을 수 없으므로, 목 S3 주소를 내려 주고 그 PUT도 목으로 받는다.
 * 파일마다 다른 fileId를 줘서 여러 장을 올려도 겹치지 않게 한다.
 */
export const fileUploadHandlers = [
  http.post('*/api/v1/file/files', () => {
    const example = getCreateUploadUrlResponseMock()
    lastFileId += 1
    return HttpResponse.json(
      {
        ...example,
        data: {
          ...example.data,
          fileId: lastFileId,
          uploadUrl: `${MOCK_S3_PATH}/${lastFileId}?X-Amz-Signature=mock`,
        },
      },
      { status: 201 },
    )
  }),
  http.put(`*${MOCK_S3_PATH}/:fileId`, () => new HttpResponse(null, { status: 200 })),
]
