import FAQ from '@/components/shared/home/FAQ'
import FeatureSections from '@/components/shared/home/FeatureSections'
import Footer from '@/components/layout/Footer'
import Hero from '@/components/shared/home/Hero'
import Navbar from '@/components/layout/Navbar'
import StatsBar from '@/components/shared/home/StatsBar'
import Testimonials from '@/components/shared/home/Testimonials'

export default function Home() {
  return (
    <div className="flex flex-1 flex-col">
      <Navbar />
      <main className="flex-1">
        <Hero />
        <StatsBar />
        <FeatureSections />
        <Testimonials />
        <FAQ />
      </main>
      <Footer />
    </div>
  )
}
