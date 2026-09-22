import { createBrowserRouter, Link, Outlet } from 'react-router-dom'
import { AppHeader } from '../components/common/AppHeader'
import { DocumentDetailPage } from '../pages/DocumentDetailPage'
import { SearchPage } from '../pages/SearchPage'

function AppLayout() {
  return <><AppHeader /><main className="app-shell"><Outlet /></main></>
}

function NotFoundPage() {
  return <section className="card" aria-labelledby="not-found-title">
    <p className="eyebrow">Kính Lúp</p>
    <h1 id="not-found-title">Không tìm thấy trang</h1>
    <p>Đường dẫn này không tồn tại.</p>
    <Link to="/">Về trang tìm kiếm</Link>
  </section>
}

export const router = createBrowserRouter([{
  element: <AppLayout />,
  children: [
    { path: '/', element: <SearchPage /> },
    { path: '/documents/:id', element: <DocumentDetailPage /> },
    { path: '*', element: <NotFoundPage /> },
  ],
}])
