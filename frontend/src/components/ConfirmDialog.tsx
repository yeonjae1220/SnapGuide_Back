'use client'

type ConfirmDialogProps = {
  open: boolean
  title: string
  description: string
  confirmLabel: string
  cancelLabel: string
  loading?: boolean
  danger?: boolean
  error?: string
  onCancel: () => void
  onConfirm: () => void
}

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  cancelLabel,
  loading = false,
  danger = false,
  error,
  onCancel,
  onConfirm,
}: ConfirmDialogProps) {
  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-[60] flex items-end justify-center bg-overlay/60 px-3 pb-3 sm:items-center sm:pb-0"
      onClick={onCancel}
    >
      <div
        role="alertdialog"
        aria-modal="true"
        className="w-full max-w-sm rounded-2xl border border-line bg-surface p-4 shadow-card"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 className={`text-base font-bold ${danger ? 'text-danger' : 'text-ink'}`}>{title}</h2>
        <p className="mt-2 text-sm text-muted">{description}</p>
        {error && <p className="mt-3 text-xs text-danger">{error}</p>}
        <div className="mt-4 grid grid-cols-2 gap-2">
          <button
            type="button"
            onClick={onCancel}
            disabled={loading}
            className="min-h-11 rounded-xl border border-line text-sm font-semibold text-muted transition hover:bg-surface-muted disabled:opacity-50"
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={loading}
            className={`min-h-11 rounded-xl text-sm font-semibold text-white transition hover:opacity-90 disabled:opacity-50 ${
              danger ? 'bg-danger' : 'bg-accent'
            }`}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
