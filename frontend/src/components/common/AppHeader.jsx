import { Link } from 'react-router-dom'
export function AppHeader() {
  return <header className="app-header"><div className="app-header__inner"><Link className="app-header__brand" to="/" aria-label="Kính Lúp, trang tìm kiếm"><span className="brand-mark" aria-hidden="true">⌕</span>Kính Lúp</Link><span className="app-header__context">HUST knowledge search</span></div></header>
}
