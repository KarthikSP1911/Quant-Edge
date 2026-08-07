import CTABand from '@/components/shared/home/CTABand'
import FeatureGrid from '@/components/shared/home/FeatureGrid'
import Footer from '@/components/layout/Footer'
import Hero from '@/components/shared/home/Hero'
import HowItWorks from '@/components/shared/home/HowItWorks'
import Navbar from '@/components/layout/Navbar'
import ProductPreview from '@/components/shared/home/ProductPreview'
import StatsBar from '@/components/shared/home/StatsBar'

export default function Home() {
  return (
    <div className="flex flex-1 flex-col">
      <Navbar />
      <main className="flex-1">
        <Hero />
        <StatsBar />
        <FeatureGrid />
        <ProductPreview />
        <HowItWorks />
        <CTABand />
      </main>
      <Footer />
    </div>
  )
}
