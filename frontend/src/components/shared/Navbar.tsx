import { Bell } from 'lucide-react'
import { ThemeToggle } from './ThemeToggle'

export function Navbar() {
  return (
    <header className="sticky top-0 z-40 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container flex h-14 items-center px-6">
        <div className="flex items-center gap-2 font-semibold text-foreground">
          <Bell className="h-5 w-5 text-primary" />
          <span>Закупки Monitor</span>
        </div>
        <div className="ml-auto">
          <ThemeToggle />
        </div>
      </div>
    </header>
  )
}
