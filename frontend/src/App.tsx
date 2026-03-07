import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from 'next-themes'
import { Toaster } from 'sonner'
import { Layout } from '@/components/shared/Layout'
import { SubscriptionsPage } from '@/pages/subscriptions/SubscriptionsPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, staleTime: 30_000 },
  },
})

export default function App() {
  return (
    <ThemeProvider attribute="class" defaultTheme="system" storageKey="admin-theme">
      <QueryClientProvider client={queryClient}>
        <BrowserRouter basename="/admin">
          <Routes>
            <Route element={<Layout />}>
              <Route index element={<Navigate to="/subscriptions" replace />} />
              <Route path="/subscriptions" element={<SubscriptionsPage />} />
            </Route>
          </Routes>
        </BrowserRouter>
        <Toaster richColors position="top-right" />
      </QueryClientProvider>
    </ThemeProvider>
  )
}
