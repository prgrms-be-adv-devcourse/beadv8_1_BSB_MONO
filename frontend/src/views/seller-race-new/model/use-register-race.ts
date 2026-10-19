import { useMutation, useQueryClient } from '@tanstack/react-query'

import { FileType, uploadFile } from '@/features/file-upload'
import { productApi, productModel, unwrapData, useIdempotencyKey } from '@/shared/api'

import { toRaceSaveRequest, type RaceFormValues } from './race-form'

export type RaceImages = {
  /** 대표 이미지 1장 (자른 결과) */
  cover: File
  /** 소개 이미지 (보이는 순서대로) */
  intro: File[]
}

/** 한 종목에 붙일 이미지를 올리고 파일 ID 목록을 만든다. */
async function uploadCourseImages(images: RaceImages): Promise<productModel.RaceImageRequestDto[]> {
  const [cover, ...intro] = await Promise.all([
    uploadFile(images.cover, FileType.THUMBNAIL),
    ...images.intro.map((file) => uploadFile(file, FileType.DETAIL)),
  ])
  return [
    { fileId: cover.fileId, fileType: FileType.THUMBNAIL, sortNo: 0 },
    ...intro.map((file, i) => ({ fileId: file.fileId, fileType: FileType.DETAIL, sortNo: i })),
  ]
}

/** 같은 파일인지 알아볼 값. File은 JSON으로 바꾸면 내용이 비어서 이름 · 크기 · 수정 시각으로 대신한다. */
function fileSignature(file: File) {
  return [file.name, file.size, file.lastModified]
}

/**
 * 대회 등록: 사진을 올린 뒤 대회를 등록한다.
 * 이미지는 종목마다 붙고 한 파일은 한 종목에만 연결되므로, 화면에서 고른 사진을 종목마다 따로 올린다.
 *
 * 같은 입력으로 다시 누르면(두 번 클릭 · 실패 뒤 재시도) 같은 Idempotency-Key를 보내 대회가 한 번만 등록된다.
 * 입력이나 사진을 바꾸면 새 키를 쓴다.
 */
export function useRegisterRace() {
  const queryClient = useQueryClient()
  const registerKey = useIdempotencyKey()
  return useMutation({
    mutationFn: async ({ values, images }: { values: RaceFormValues; images: RaceImages }) => {
      const idempotencyKey = registerKey({
        values,
        cover: fileSignature(images.cover),
        intro: images.intro.map(fileSignature),
      })
      const courseImages = await Promise.all(values.courses.map(() => uploadCourseImages(images)))
      return unwrapData(
        await productApi.register(toRaceSaveRequest(values, courseImages), { idempotencyKey }),
      )
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: productApi.getFindMineQueryKey() }),
  })
}
