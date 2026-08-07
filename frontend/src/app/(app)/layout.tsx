import AppTopbar from '@/components/layout/AppTopbar'

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex flex-1 flex-col">
      <AppTopbar />
      <main className="mx-auto w-full max-w-7xl flex-1 px-6 py-8 sm:px-10">{children}</main>
    </div>
  )
}
