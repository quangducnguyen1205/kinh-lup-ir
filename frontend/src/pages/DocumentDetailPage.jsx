import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { getDocument } from '../api/searchApi'

function formatDate(value) {
  if (!value) return 'Chưa cập nhật'

  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
  }).format(new Date(value))
}

export function DocumentDetailPage() {
  const { id } = useParams()
  const location = useLocation()
  const backTo = location.state?.from || '/'

  const [state, setState] = useState({
    status: 'loading',
    document: null,
    error: null,
  })

  useEffect(() => {
    const controller = new AbortController()

    setState({
      status: 'loading',
      document: null,
      error: null,
    })

    getDocument(id, {
      signal: controller.signal,
    })
      .then((document) => {
        setState({
          status: 'success',
          document,
          error: null,
        })
      })
      .catch((error) => {
        if (error.name === 'AbortError') return

        setState({
          status: 'error',
          document: null,
          error,
        })
      })

    return () => controller.abort()
  }, [id])

  const { status, document, error } = state

  if (status === 'loading') {
    return (
      <div className="detail-layout">
        <div className="loading-state">
          <i />
          Đang tải tài liệu...
        </div>
      </div>
    )
  }

  if (status === 'error') {
    return (
      <div className="detail-layout">
        <div className="message error">
          <strong>Không thể tải tài liệu.</strong>
          <span>{error?.message}</span>
          <Link to={backTo}>← Về kết quả tìm kiếm</Link>
        </div>
      </div>
    )
  }

  return (
    <div className="detail-layout">
      <Link className="back-link" to={backTo}>
        ← Về kết quả tìm kiếm
      </Link>

      <article className="detail-card">
        <div className="detail-header">
          <p className="eyebrow">
            Document / {document.contentType}
          </p>
          <span>● Indexed</span>
        </div>

        <h1 id="document-page-title">
          {document.title || 'Tài liệu chưa có tiêu đề'}
        </h1>

        <div className="detail-meta">
          Xuất bản: {formatDate(document.publishedAt)}
          {' · '}
          Crawl lần cuối: {formatDate(document.lastCrawledAt)}
        </div>

        <div className="detail-body">
          {document.text.split('\n').map((text, index) =>
            text ? <p key={index}>{text}</p> : <br key={index} />,
          )}
        </div>

        <a
          className="source-link"
          href={document.url}
          target="_blank"
          rel="noreferrer"
        >
          Mở tài liệu gốc ↗
        </a>
      </article>
    </div>
  )
}
