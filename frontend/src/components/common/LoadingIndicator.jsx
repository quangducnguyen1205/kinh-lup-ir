export function LoadingIndicator({ label = 'Đang tải…' }) {
  return <p role="status" aria-live="polite">{label}</p>
}
