import Image from 'next/image'

import { cn } from '@/shared/lib'

// 피그마 Logo 페이지에서 받은 파일(public/brand). 글자색이 정해진 SVG라 바탕에 맞는 파일을 고른다.
const LOGO = {
  /** 밝은 바탕 헤더용 글자 로고 95.6×28 */
  wordmark: { src: '/brand/wordmark.svg', width: 95.6073, height: 28 },
  /** 네이비(Hero) 바탕 사이드바용 글자 로고 */
  wordmarkOnHero: { src: '/brand/wordmark-on-hero.svg', width: 95.6073, height: 28 },
  /** 하단 탭 가운데 홈 버튼 48 */
  navHome: { src: '/brand/nav-home.svg', width: 48, height: 48 },
  /** 접힌 사이드바용 정사각 로고 32 */
  symbol: { src: '/brand/symbol-on-hero.svg', width: 32, height: 32 },
} as const

export type LogoVariant = keyof typeof LOGO

export type LogoProps = { variant?: LogoVariant; className?: string }

export function Logo({ variant = 'wordmark', className }: LogoProps) {
  const { src, width, height } = LOGO[variant]
  return (
    <Image
      src={src}
      width={width}
      height={height}
      alt="크루런"
      unoptimized
      priority
      className={cn('shrink-0', className)}
    />
  )
}
