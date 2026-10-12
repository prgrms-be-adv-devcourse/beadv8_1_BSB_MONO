'use client'

import { useRouter } from 'next/navigation'
import { useState } from 'react'

import { ConsoleCard, ConsolePage } from '@/widgets/console-page'
import { cn } from '@/shared/lib'
import {
  Alert,
  Button,
  DataTable,
  FOCUS_RING,
  ImageCropDialog,
  ImageListDialog,
  ImageUpload,
  Radio,
  RadioGroup,
  TextField,
  useImageList,
  type ImageUploadValue,
  type UploadImage,
} from '@/shared/ui'

// TODO(api): 파일 서비스(ApiV1FileController) 업로드로 바꾼다. 지금은 브라우저 안 주소만 만든다.
const localUpload: UploadImage = async (file, onProgress) => {
  onProgress(1)
  return URL.createObjectURL(file)
}

const COURSES = [
  {
    course: '10K',
    price: '₩45,000',
    capacity: '2,000명',
    age: '만 16세 ~ 만 80세',
    tax: '과세',
    kit: '배번 · 기념 티셔츠',
  },
  {
    course: '5K',
    price: '₩35,000',
    capacity: '1,000명',
    age: '만 14세 ~ 만 80세',
    tax: '과세',
    kit: '배번 · 기념 티셔츠',
  },
]

const KITS = [
  {
    option: '기념 티셔츠',
    size: 'S · M · L · XL · 2XL',
    courses: '10K · 5K',
    method: '배송',
    note: '사이즈표는 소개 이미지에',
  },
  {
    option: '배번',
    size: '없음',
    courses: '10K · 5K',
    method: '배송',
    note: '환불 기한 뒤 함께 배송',
  },
]

const LINK = cn('rounded-sm text-label-m text-text-brand', FOCUS_RING)

// 피그마 V2 · S02 대회 등록 (+ S02-1~3 이미지 올리기 · 자르기 창, S02-4 등록 완료)
// TODO(api): 대회 등록 API에 연결하고 react-hook-form + zod로 검사한다. 값은 시안 예시.
export function SellerRaceNewPage() {
  const router = useRouter()
  const [cover, setCover] = useState<ImageUploadValue[]>([])
  const [cropOpen, setCropOpen] = useState(false)
  const [dropped, setDropped] = useState<File | null>(null)
  const [intro, setIntro] = useState<ImageUploadValue[]>([])
  const [listOpen, setListOpen] = useState(false)
  const list = useImageList({ upload: localUpload, rules: { minWidth: 1080 } })

  function openList(files: File[] = []) {
    // 창을 열 때는 저장된 목록에서 시작한다
    list.setItems(
      intro.map((v) => ({
        id: v.id,
        name: v.name,
        previewUrl: v.url,
        url: v.url,
        status: 'done',
        progress: 1,
      })),
    )
    setListOpen(true)
    if (files.length) void list.add(files)
  }

  return (
    <ConsolePage
      title="대회 등록"
      description="판매가 시작되면 가격과 취소 · 환불 정책은 바꿀 수 없어요. 구매자에게 유리하게 바꾸는 것만 돼요."
      actions={
        <>
          <Button variant="secondary">미리보기</Button>
          <Button onClick={() => router.push('/seller/races?registered=1')}>등록하기</Button>
        </>
      }
    >
      <ConsoleCard title="기본 정보">
        <div className="flex gap-4">
          <TextField
            label="대회 이름"
            defaultValue="2026 용감한 쿠키RUN in 서울"
            className="flex-1"
          />
          <TextField label="대회 일시" defaultValue="2026-11-01 (일) 08:00" className="flex-1" />
        </div>
        <div className="flex gap-4">
          <TextField label="장소" defaultValue="서울숲 일대" className="flex-1" />
          <TextField
            label="접수 기간"
            defaultValue="10/14 10:00 ~ 10/31 18:00"
            className="flex-1"
          />
          <TextField
            label="참가 가능 나이"
            defaultValue="종목마다 최소 · 최대 나이를 정해요"
            helper="대회 당일 만 나이 · 최소는 만 14세 이상"
            readOnly
            className="flex-1"
          />
        </div>
      </ConsoleCard>

      <ConsoleCard title="이미지">
        <div className="flex gap-4">
          <ImageUpload
            label="대표 이미지 (필수 · 1장)"
            helper="정사각(1:1) 1125×1125px 권장 · 대회 목록과 대회 상세 맨 위에 보여요"
            value={cover}
            onOpen={() => {
              setDropped(null)
              setCropOpen(true)
            }}
            onFilesDrop={(files) => {
              setDropped(files[0] ?? null)
              setCropOpen(true)
            }}
            onRemove={() => setCover([])}
            className="flex-1"
          />
          <ImageUpload
            type="multiple"
            label="소개 이미지 (여러 장)"
            helper="코스 · 타임테이블 · 기념품 사이즈표를 넣어 주세요"
            value={intro}
            onOpen={() => openList()}
            onFilesDrop={openList}
            onRemove={(id) => setIntro((prev) => prev.filter((v) => v.id !== id))}
            className="flex-1"
          />
        </div>
      </ConsoleCard>

      <ConsoleCard
        title="종목 · 참가권"
        aside={
          <button type="button" className={LINK}>
            종목 추가
          </button>
        }
      >
        <DataTable
          label="종목 · 참가권"
          rows={COURSES}
          rowKey={(r) => r.course}
          columns={[
            { key: 'course', header: '종목', render: (r) => r.course },
            { key: 'price', header: '참가비', align: 'right', render: (r) => r.price },
            { key: 'capacity', header: '모집 정원', align: 'right', render: (r) => r.capacity },
            { key: 'age', header: '참가 나이 (최소 ~ 최대)', render: (r) => r.age },
            { key: 'tax', header: '과세 구분', render: (r) => r.tax },
            { key: 'kit', header: '키트 (참가권 포함)', render: (r) => r.kit },
          ]}
        />
        <Alert
          title="다른 곳에서도 함께 팔 수 있어요"
          description="정원에는 크루런에 배정할 수량만 적어 주세요. 한 사람 한 대회 1건 규칙은 크루런 안에서만 지켜져요."
        />
      </ConsoleCard>

      <ConsoleCard title="취소 · 환불 정책">
        <TextField
          label="환불 기한"
          defaultValue="2026-10-31 (토) 23:59"
          helper="이 날짜까지 취소하면 전액 환불, 그 뒤 개인 사정 환불은 안 돼요. 결제 전에 구매자에게 보여 줘요."
        />
      </ConsoleCard>

      <ConsoleCard
        title="키트 배송"
        aside={
          <button type="button" className={LINK}>
            옵션 추가
          </button>
        }
      >
        <p className="text-caption text-text-tertiary">
          키트는 배송으로만 보내요. 환불 기한이 지난 뒤, 참가 신청 때 받은 주소로 보내요. 사이즈는
          옵션마다 신청 때 받아요.
        </p>
        <DataTable
          label="키트 옵션"
          rows={KITS}
          rowKey={(r) => r.option}
          columns={[
            { key: 'option', header: '키트 옵션', render: (r) => r.option },
            { key: 'size', header: '사이즈', render: (r) => r.size },
            { key: 'courses', header: '포함 종목', render: (r) => r.courses },
            { key: 'method', header: '받는 방법', render: (r) => r.method },
            { key: 'note', header: '안내', render: (r) => r.note },
          ]}
        />
      </ConsoleCard>

      <ConsoleCard title="공개 범위">
        <RadioGroup defaultValue="public" className="flex-row gap-6">
          <Radio value="public" label="전체 공개 (기본)" />
          <Radio value="link" label="링크로만 공개" />
        </RadioGroup>
        <p className="text-caption text-text-tertiary">
          링크로만 공개하면 대회 목록 · 검색에 나오지 않아요. 링크로 들어온 회원은 신청할 수 있고,
          링크가 퍼져도 막지 않아요.
        </p>
      </ConsoleCard>

      <ImageCropDialog
        open={cropOpen}
        onOpenChange={setCropOpen}
        file={dropped}
        onApply={(blob, name) => {
          setCover([{ id: 'cover', url: URL.createObjectURL(blob), name, meta: '1125×1125' }])
          setCropOpen(false)
        }}
      />
      <ImageListDialog
        open={listOpen}
        onOpenChange={setListOpen}
        items={list.items}
        onFilesAdd={(files) => void list.add(files)}
        onRemove={list.remove}
        onMove={list.move}
        onSave={() => {
          setIntro(
            list.items
              .filter((item) => item.status === 'done')
              .map((item) => ({ id: item.id, url: item.url ?? item.previewUrl, name: item.name })),
          )
          setListOpen(false)
        }}
      />
    </ConsolePage>
  )
}
