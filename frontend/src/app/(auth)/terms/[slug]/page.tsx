import { notFound } from 'next/navigation'

import { TERMS_SLUGS, TermsPage, type TermsSlug } from '@/views/auth'

export function generateStaticParams() {
  return TERMS_SLUGS.map((slug) => ({ slug }))
}

export default async function Page({ params }: PageProps<'/terms/[slug]'>) {
  const { slug } = await params
  if (!TERMS_SLUGS.includes(slug as TermsSlug)) {
    notFound()
  }
  return <TermsPage slug={slug as TermsSlug} />
}
