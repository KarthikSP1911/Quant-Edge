import CTABand from '@/components/home/CTABand'
import FeatureGrid from '@/components/home/FeatureGrid'
import Footer from '@/components/home/Footer'
import Hero from '@/components/home/Hero'
import HowItWorks from '@/components/home/HowItWorks'
import Navbar from '@/components/home/Navbar'
import ProductPreview from '@/components/home/ProductPreview'
import StatsBar from '@/components/home/StatsBar'

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
