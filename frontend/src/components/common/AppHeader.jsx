import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

function getInitialTheme() {
  const savedTheme = window.localStorage.getItem('kinh-lup-theme')
  if (savedTheme === 'light' || savedTheme === 'dark') return savedTheme
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function AppHeader() {
  const [theme, setTheme] = useState(getInitialTheme)

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    window.localStorage.setItem('kinh-lup-theme', theme)
  }, [theme])

  const nextTheme = theme === 'light' ? 'dark' : 'light'
  return <header className="app-header"><div className="app-header__inner"><Link className="app-header__brand" to="/" aria-label="Kính Lúp, trang tìm kiếm"><span className="brand-mark" aria-hidden="true">⌕</span>Kính Lúp</Link><div className="app-header__actions"><span className="app-header__context">HUST knowledge search</span><button className="theme-toggle" type="button" onClick={() => setTheme(nextTheme)} aria-label={`Chuyển sang giao diện ${nextTheme === 'dark' ? 'tối' : 'sáng'}`} title={`Giao diện ${nextTheme === 'dark' ? 'tối' : 'sáng'}`}><span aria-hidden="true">{theme === 'light' ? '☾' : '☀'}</span><span>{theme === 'light' ? 'Tối' : 'Sáng'}</span></button></div></div></header>
}
