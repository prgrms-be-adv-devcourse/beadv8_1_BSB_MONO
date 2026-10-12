import Image from 'next/image'
import Link from 'next/link'

import { AppHeader } from '@/widgets/app-header'
import { BottomNav } from '@/widgets/bottom-nav'
import { CLOSING_SOON_RACES, OPEN_RACES, RACE_BANNERS, RaceCard } from '@/entities/race'
import { cn } from '@/shared/lib'
import { buttonClassName, Carousel, CarouselSlide, FOCUS_RING } from '@/shared/ui'

// 피그마 V2 · M01 홈: ① 배너 → ② 곧 마감(가로 넘김) → ③ 지금 접수 중(2열, 4개만)
export function HomePage() {
  return (
    <>
      <AppHeader />
      <main className="flex-1">
        <section className="pt-4 pb-2">
          <Carousel label="접수 중인 대회">
            {RACE_BANNERS.map((banner) => (
              <CarouselSlide
                key={banner.id}
                image={
                  <Image
                    src={banner.image}
                    alt={banner.title ?? ''}
                    fill
                    sizes="420px"
                    className="object-cover"
                  />
                }
                title={banner.title}
                tags={banner.tags}
              />
            ))}
          </Carousel>
        </section>

        <section className="flex flex-col gap-4 pt-8 pb-2">
          <SectionHead title="곧 마감돼요" />
          <div className="flex [scrollbar-width:none] gap-4 overflow-x-auto px-(--layout-page-margin)">
            {CLOSING_SOON_RACES.map((race) => (
              <RaceCard key={race.id} race={race} className="w-54 shrink-0" />
            ))}
          </div>
        </section>

        <section className="flex flex-col gap-4 py-8">
          <SectionHead title="지금 접수 중인 대회" />
          <div className="grid grid-cols-2 gap-x-4 gap-y-6 px-(--layout-page-margin)">
            {OPEN_RACES.map((race) => (
              <RaceCard key={race.id} race={race} />
            ))}
          </div>
          <div className="px-(--layout-page-margin)">
            <Link
              href="/races"
              className={buttonClassName({ variant: 'secondary', className: 'w-full' })}
            >
              대회 전체 보기
            </Link>
          </div>
        </section>
      </main>
      <BottomNav />
    </>
  )
}

function SectionHead({ title }: { title: string }) {
  return (
    <div className="flex items-end justify-between px-(--layout-page-margin)">
      <h2 className="text-heading-l">{title}</h2>
      <Link href="/races" className={cn('rounded-sm text-label-m text-text-secondary', FOCUS_RING)}>
        전체 보기
      </Link>
    </div>
  )
}
