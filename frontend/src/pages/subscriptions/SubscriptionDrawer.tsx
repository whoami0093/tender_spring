import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { SubscriptionForm } from './SubscriptionForm'
import { createSubscription, updateSubscription } from '@/api/subscriptions'
import type { SubscriptionResponse, SubscriptionRequest, SubscriptionUpdateRequest } from '@/api/types'

interface Props {
  open: boolean
  onClose: () => void
  existing?: SubscriptionResponse
}

export function SubscriptionDrawer({ open, onClose, existing }: Props) {
  const qc = useQueryClient()

  const createMutation = useMutation({
    mutationFn: (data: SubscriptionRequest) => createSubscription(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['subscriptions'] })
      toast.success('Подписка создана')
      onClose()
    },
    onError: (e: Error) => toast.error(e.message),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: SubscriptionUpdateRequest }) =>
      updateSubscription(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['subscriptions'] })
      toast.success('Подписка обновлена')
      onClose()
    },
    onError: (e: Error) => toast.error(e.message),
  })

  const isLoading = createMutation.isPending || updateMutation.isPending

  function handleSubmit(data: SubscriptionRequest) {
    if (existing) {
      const { source: _source, ...updateData } = data
      updateMutation.mutate({ id: existing.id, data: updateData })
    } else {
      createMutation.mutate(data)
    }
  }

  return (
    // key сбрасывает всё внутреннее состояние формы при смене подписки или режима
    <Sheet open={open} onOpenChange={(o) => !o && onClose()}>
      <SheetContent>
        <SheetHeader>
          <SheetTitle>{existing ? 'Редактировать подписку' : 'Новая подписка'}</SheetTitle>
        </SheetHeader>
        <SubscriptionForm
          key={existing?.id ?? 'new'}
          existing={existing}
          onSubmit={handleSubmit}
          isLoading={isLoading}
          onCancel={onClose}
        />
      </SheetContent>
    </Sheet>
  )
}
