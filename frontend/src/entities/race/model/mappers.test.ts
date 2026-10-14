import { describe, expect, it } from 'vitest'

import { fileMock, productMock } from '@/shared/testing'

import { raceBadge, toClosingSoonCards, toRaceDetail, toRaceListItem } from './mappers'

const summary = productMock.getSearchResponseMock().data!.races![0]
const detail = productMock.getFindRaceResponseMock().data!

describe('대회 응답 → 화면 모양', () => {
  it('목록 한 줄: 대회일 · 장소, 최저가, 남은 자리', () => {
    expect(toRaceListItem(summary)).toMatchObject({
      id: '1',
      title: '2026 용감한 쿠키RUN in 서울',
      posterLabel: '10K',
      meta: '11/1 (일) · 서울숲 일대',
      price: '₩45,000~',
      remaining: '580자리 남음',
    })
  })

  it('곧 마감 카드는 종목마다 한 장이고 종목을 골라 둔다', () => {
    expect(toClosingSoonCards(summary)).toEqual([
      expect.objectContaining({ id: '1', option: '1', title: '2026 용감한 쿠키RUN in 서울 (10K)' }),
    ])
  })

  it('판매 예정 · 정원 마감 · 판매 중 배지', () => {
    expect(raceBadge('SCHEDULED', [], undefined)).toEqual({ label: '접수 예정', tone: 'neutral' })
    expect(raceBadge('ON_SALE', [{ status: 'SOLD_OUT' }], '2099-01-01T00:00:00').label).toBe(
      '정원 마감',
    )
    expect(raceBadge('ON_SALE', [{ status: 'OPEN' }], '2099-01-01T00:00:00')).toEqual({
      label: '접수중',
      tone: 'brand',
    })
  })

  it('상세: 종목 ID를 종목 선택 값으로 쓰고, 종목 대표 이미지를 붙인다', () => {
    const files = fileMock.getFindByRefResponseMock().data!
    const race = toRaceDetail(detail, { 1: files })

    expect(race.options).toEqual([
      expect.objectContaining({ id: '1', name: '10K', price: 45000, image: files[0].url }),
    ])
    expect(race.images).toEqual([files[0].url, files[1].url])
    expect(race.startsAt).toBe('11/1 (일) 08:00 출발')
    expect(race.schedule).toContainEqual({ label: '전액 환불 기한', value: '10/31 (토) 23:59' })
    expect(race.seller.company).toBe('(주)쿠키런')
  })

  it('상세: 이미지가 없으면 빈 목록(색 포스터)', () => {
    expect(toRaceDetail(detail).images).toEqual([])
  })
})
