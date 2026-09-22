import { Link } from 'react-router-dom'

export function AppHeader() {
  return <header className="app-header"><Link className="app-header__brand" to="/" aria-label="Kính Lúp, trang tìm kiếm">Kính Lúp</Link></header>
}
