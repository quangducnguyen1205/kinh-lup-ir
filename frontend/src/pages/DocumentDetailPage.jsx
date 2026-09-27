import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getDocument } from '../api/searchApi'
const formatDate = (value) => value ? new Intl.DateTimeFormat('vi-VN',{dateStyle:'medium'}).format(new Date(value)) : 'Chưa cập nhật'
export function DocumentDetailPage() {
  const {id} = useParams(), [state,setState] = useState({status:'loading',document:null,error:null})
  useEffect(() => { const controller = new AbortController(); setState({status:'loading',document:null,error:null}); getDocument(id,{signal:controller.signal}).then(document => setState({status:'success',document,error:null})).catch(error => { if (error.name !== 'AbortError') setState({status:'error',document:null,error}) }); return () => controller.abort() }, [id])
  const {status,document,error} = state
  if (status === 'loading') return <div className="detail-layout"><div className="loading-state"><i/>Đang tải tài liệu...</div></div>
  if (status === 'error') return <div className="detail-layout"><div className="message error"><strong>Không thể tải tài liệu.</strong><span>{error?.message}</span><Link to="/">← Về trang tìm kiếm</Link></div></div>
  return <div className="detail-layout"><Link className="back-link" to="/">← Về kết quả tìm kiếm</Link><article className="detail-card"><div className="detail-header"><p className="eyebrow">Document / {document.contentType}</p><span>● Indexed</span></div><h1 id="document-page-title">{document.title || 'Tài liệu chưa có tiêu đề'}</h1><div className="detail-meta">Xuất bản: {formatDate(document.publishedAt)} · Crawl lần cuối: {formatDate(document.lastCrawledAt)}</div><div className="detail-body">{document.text.split('\n').map((text,index) => text ? <p key={index}>{text}</p> : <br key={index}/>)}</div><a className="source-link" href={document.url} target="_blank" rel="noreferrer">Mở tài liệu gốc ↗</a></article></div>
}
