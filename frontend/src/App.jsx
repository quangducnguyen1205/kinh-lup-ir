const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export default function App() {
  return (
    <main className="app-shell">
      <section className="card">
        <p className="eyebrow">IT4863 · Kính Lúp</p>
        <h1>Frontend skeleton is running.</h1>
        <p>
          Đây chỉ là shell kỹ thuật để Chính bắt đầu T03. Search box, result list,
          pagination và document detail chưa được triển khai ở bước setup.
        </p>
        <dl>
          <div>
            <dt>Backend API</dt>
            <dd>{apiBaseUrl}</dd>
          </div>
          <div>
            <dt>Task tiếp theo</dt>
            <dd>T03 · Search UI baseline</dd>
          </div>
        </dl>
      </section>
    </main>
  )
}
