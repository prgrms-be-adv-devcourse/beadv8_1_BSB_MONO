import { useMutation, useQueryClient } from '@tanstack/react-query'

import { FileType, uploadFile } from '@/features/file-upload'
import { productApi, productModel, unwrapData } from '@/shared/api'

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

/**
 * 대회 등록: 사진을 올린 뒤 대회를 등록한다.
 * 이미지는 종목마다 붙고 한 파일은 한 종목에만 연결되므로, 화면에서 고른 사진을 종목마다 따로 올린다.
 */
export function useRegisterRace() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ values, images }: { values: RaceFormValues; images: RaceImages }) => {
      const courseImages = await Promise.all(values.courses.map(() => uploadCourseImages(images)))
      return unwrapData(await productApi.register(toRaceSaveRequest(values, courseImages)))
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: productApi.getFindMineQueryKey() }),
  })
}
