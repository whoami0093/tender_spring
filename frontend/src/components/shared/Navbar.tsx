import { Bell } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { ThemeToggle } from './ThemeToggle'

export function Navbar() {
  return (
    <header className="sticky top-0 z-40 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container flex h-14 items-center px-6">
        <div className="flex items-center gap-2 font-semibold text-foreground mr-6">
          <Bell className="h-5 w-5 text-primary" />
          <span>Закупки Monitor</span>
        </div>
        <nav className="flex items-center gap-4 text-sm">
          <NavLink
            to="/subscriptions"
            className={({ isActive }) =>
              isActive
                ? 'font-medium text-foreground'
                : 'text-muted-foreground hover:text-foreground transition-colors'
            }
          >
            Подписки
          </NavLink>
          <NavLink
            to="/tenders"
            className={({ isActive }) =>
              isActive
                ? 'font-medium text-foreground'
                : 'text-muted-foreground hover:text-foreground transition-colors'
            }
          >
            Тендеры
          </NavLink>
        </nav>
        <div className="ml-auto">
          <ThemeToggle />
        </div>
      </div>
    </header>
  )
}
