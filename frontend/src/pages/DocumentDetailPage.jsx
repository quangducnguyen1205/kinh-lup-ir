import { Link, useParams } from 'react-router-dom'

export function DocumentDetailPage() {
  const { id } = useParams()
  return <section className="card" aria-labelledby="document-page-title">
    <p className="eyebrow">Chi tiết tài liệu</p>
    <h1 id="document-page-title">Document detail placeholder</h1>
    <p>Document ID: {id}</p>
    <Link to="/">Quay lại tìm kiếm</Link>
  </section>
}
