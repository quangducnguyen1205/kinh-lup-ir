import { useEffect, useState } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router-dom'
import { searchDocuments } from '../api/searchApi'
import { getSafeExternalUrl } from '../utils/safeUrl'

const PAGE_SIZE = 10

function normalizePage(value) {
  const page = Number(value)
  return Number.isInteger(page) && page >= 0 ? page : 0
}

function formatDate(value) {
  if (!value) return 'Chưa cập nhật'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Chưa cập nhật'

  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
  }).format(date)
}

function Result({ result, backTo }) {
  const title = result.title || 'Tài liệu chưa có tiêu đề'
  const snippet = result.snippet || 'Chưa có phần mô tả cho tài liệu này.'
  const sourceUrl = getSafeExternalUrl(result.url)

  return (
    <article className="result-card">
      <div className="result-top">
        <span>Tài liệu HUST</span>
        <span>Điểm {Number(result.score || 0).toFixed(2)}</span>
      </div>

      <h2>
        <Link
          to={`/documents/${encodeURIComponent(result.id)}`}
          state={{ from: backTo }}
        >
          {title}
        </Link>
      </h2>

      <p>{snippet}</p>

      <div className="result-meta">
        <span>{formatDate(result.publishedAt)}</span>
        {sourceUrl ? (
          <a href={sourceUrl} target="_blank" rel="noreferrer">
            Mở nguồn ↗
          </a>
        ) : (
          <span>URL nguồn không hợp lệ</span>
        )}
      </div>
    </article>
  )
}

function Pagination({ page, total, onChange }) {
  const totalPages = Math.ceil(total / PAGE_SIZE)

  if (totalPages < 2) return null

  return (
    <nav className="pagination" aria-label="Phân trang kết quả">
      <button
        type="button"
        onClick={() => onChange(page - 1)}
        disabled={page === 0}
      >
        ← Trước
      </button>

      <span>
        Trang <strong>{page + 1}</strong> / {totalPages}
      </span>

      <button
        type="button"
        onClick={() => onChange(page + 1)}
        disabled={page + 1 >= totalPages}
      >
        Sau →
      </button>
    </nav>
  )
}

export function SearchPage() {
  const location = useLocation()
  const [params, setParams] = useSearchParams()
  const queryParam = params.get('q') || ''
  const pageParam = normalizePage(params.get('page'))

  const [input, setInput] = useState(queryParam)
  const [query, setQuery] = useState(queryParam)
  const [page, setPage] = useState(pageParam)
  const [retryKey, setRetryKey] = useState(0)
  const [state, setState] = useState({
    status: queryParam ? 'loading' : 'idle',
    data: null,
    error: null,
  })

  useEffect(() => {
    setInput(queryParam)
    setQuery(queryParam)
    setPage(pageParam)
  }, [pageParam, queryParam])

  useEffect(() => {
    if (!query.trim()) {
      setState({
        status: 'idle',
        data: null,
        error: null,
      })
      return undefined
    }

    const controller = new AbortController()

    setState((current) => ({
      ...current,
      status: 'loading',
      error: null,
    }))

    searchDocuments({
      query,
      page,
      size: PAGE_SIZE,
      signal: controller.signal,
    })
      .then((data) => {
        const lastPage = data.total > 0
          ? Math.ceil(data.total / PAGE_SIZE) - 1
          : 0

        if (page > lastPage) {
          setPage(lastPage)
          setParams({
            q: query,
            page: String(lastPage),
          })
          return
        }

        setState({
          status: 'success',
          data,
          error: null,
        })
      })
      .catch((error) => {
        if (error.name === 'AbortError') return

        setState({
          status: 'error',
          data: null,
          error,
        })
      })

    return () => controller.abort()
  }, [page, query, retryKey])

  function submit(event) {
    event.preventDefault()

    const nextQuery = input.trim()

    setQuery(nextQuery)
    setPage(0)
    setParams(nextQuery ? { q: nextQuery } : {})
  }

  function changePage(nextPage) {
    setPage(nextPage)
    setParams({
      q: query,
      page: String(nextPage),
    })
    window.scrollTo({
      top: 0,
      behavior: 'smooth',
    })
  }

  const { status, data, error } = state
  const currentSearch = `${location.pathname}${location.search}`

  return (
    <div className="search-layout">
      <section className="search-hero">
        <div>
          <p className="eyebrow">Kính Lúp / Search</p>
          <h1 id="search-page-title">
            Tìm đúng tài liệu,
            <br />
            <em>nhanh hơn.</em>
          </h1>
          <p className="hero-copy">
            Tra cứu thông báo, hướng dẫn và tài liệu công khai từ HUST.
          </p>
        </div>

        <form className="search-form" onSubmit={submit} role="search">
          <label htmlFor="search-input">Từ khóa tìm kiếm</label>

          <div className="search-row">
            <input
              id="search-input"
              value={input}
              onChange={(event) => setInput(event.target.value)}
              placeholder="Ví dụ: học bổng, tuyển sinh..."
              autoComplete="off"
            />
            <button type="submit">Tìm kiếm ↗</button>
          </div>

          <small>Enter để tìm kiếm · hỗ trợ tìm kiếm không dấu</small>
        </form>
      </section>

      <section className="results-panel" aria-live="polite">
        {!query && (
          <div className="empty-state">
            <span>✦</span>
            <h2>Bắt đầu với một từ khóa</h2>
            <p>Nhập nội dung bạn muốn tìm để khám phá kho tài liệu HUST.</p>
          </div>
        )}

        {status === 'loading' && (
          <div className="loading-state">
            <i />
            Đang tìm kiếm tài liệu...
          </div>
        )}

        {status === 'error' && (
          <div className="message error">
            <strong>Không thể tải kết quả.</strong>
            <span>{error?.message}</span>
            <button type="button" onClick={() => setRetryKey((current) => current + 1)}>
              Thử lại
            </button>
          </div>
        )}

        {status === 'success' && data && (
          <>
            <header className="results-header">
              <div>
                <p className="eyebrow">Search results</p>
                <h2>Kết quả cho “{data.query}”</h2>
              </div>
              <span>{data.total} tài liệu</span>
            </header>

            {data.results.length ? (
              data.results.map((result) => (
                <Result
                  key={result.id}
                  result={result}
                  backTo={currentSearch}
                />
              ))
            ) : (
              <div className="empty-state">
                <span>⌕</span>
                <h2>Không tìm thấy kết quả</h2>
                <p>Thử một từ khóa khác hoặc kiểm tra lại chính tả.</p>
              </div>
            )}

            <Pagination
              page={data.page}
              total={data.total}
              onChange={changePage}
            />
          </>
        )}
      </section>
    </div>
  )
}
