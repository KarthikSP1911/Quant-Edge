import AppTopbar from '@/components/layout/AppTopbar'
import OrderStreamListener from '@/components/layout/OrderStreamListener'
import ChatWidget from '@/components/layout/ChatWidget'
import MarketStatusBanner from '@/components/layout/MarketStatusBanner'

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex flex-1 flex-col">
      <OrderStreamListener />
      <div className="sticky top-0 z-50 flex flex-col">
        <MarketStatusBanner />
        <AppTopbar />
      </div>
      <main className="mx-auto w-full max-w-7xl flex-1 px-8 py-8 sm:px-12 lg:px-16">
        {children}
      </main>
      <ChatWidget />
    </div>
  )
}
