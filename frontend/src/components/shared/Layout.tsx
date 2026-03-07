import { Outlet } from 'react-router-dom'
import { Navbar } from './Navbar'

export function Layout() {
  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="container px-6 py-8">
        <Outlet />
      </main>
    </div>
  )
}
