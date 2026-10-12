import type { MetadataRoute } from 'next'

// 홈 화면에 추가할 때 쓰는 앱 정보. 아이콘은 public/icons/
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: '크루런',
    short_name: '크루런',
    description: '러닝 대회 참가권 마켓플레이스',
    start_url: '/',
    display: 'standalone',
    background_color: '#FFFFFF',
    theme_color: '#0057FF',
    icons: [
      { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png' },
      { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png' },
      {
        src: '/icons/icon-maskable-512.png',
        sizes: '512x512',
        type: 'image/png',
        purpose: 'maskable',
      },
    ],
  }
}
