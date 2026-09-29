import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { SearchPage } from '../pages/SearchPage'
import { DocumentDetailPage } from '../pages/DocumentDetailPage'

const { searchDocuments, getDocument } = vi.hoisted(() => ({ searchDocuments: vi.fn(), getDocument: vi.fn() }))
vi.mock('../api/searchApi', () => ({ searchDocuments, getDocument }))

afterEach(() => { cleanup(); vi.clearAllMocks() })

function renderSearch(initialEntry = '/') {
  return render(<MemoryRouter initialEntries={[initialEntry]}><SearchPage /></MemoryRouter>)
}

describe('SearchPage', () => {
  it('shows idle state without requesting blank queries', () => {
    renderSearch()
    expect(screen.getByRole('heading', { name: /bắt đầu với một từ khóa/i })).toBeTruthy()
    expect(searchDocuments).not.toHaveBeenCalled()
  })

  it('submits a query and renders successful results', async () => {
    searchDocuments.mockResolvedValue({ query: 'học bổng', total: 1, page: 0, size: 10, results: [{ id: 'doc-1', title: 'Thông tin học bổng', url: 'https://hust.edu.vn/doc-1', snippet: 'Hồ sơ học bổng.', score: 1.2, publishedAt: null }] })
    renderSearch()
    fireEvent.change(screen.getByRole('textbox'), { target: { value: '  học bổng  ' } })
    fireEvent.submit(screen.getByRole('search'))
    await waitFor(() => expect(screen.getByRole('heading', { name: /thông tin học bổng/i })).toBeTruthy())
    expect(searchDocuments).toHaveBeenCalledWith(expect.objectContaining({ query: 'học bổng', page: 0, size: 10 }))
    expect(screen.getByText('Chưa cập nhật')).toBeTruthy()
  })

  it('clamps an out-of-range page to the last available page', async () => {
    searchDocuments.mockResolvedValue({
      query: 'hust',
      total: 21,
      page: 100,
      size: 10,
      results: [],
    })

    renderSearch('/?q=hust&page=100')

    await waitFor(() => {
      expect(searchDocuments).toHaveBeenCalledWith(
        expect.objectContaining({ query: 'hust', page: 100, size: 10 }),
      )
      expect(searchDocuments).toHaveBeenCalledWith(
        expect.objectContaining({ query: 'hust', page: 2, size: 10 }),
      )
    })
  })

  it('retries a failed search request', async () => {
    searchDocuments
      .mockRejectedValueOnce(new Error('Tạm thời không kết nối.'))
      .mockResolvedValueOnce({
        query: 'hust',
        total: 1,
        page: 0,
        size: 10,
        results: [{
          id: 'doc-1',
          title: 'Kết quả sau khi thử lại',
          url: 'https://hust.edu.vn/doc-1',
          snippet: 'Đã tải lại.',
          score: 1,
          publishedAt: null,
        }],
      })

    renderSearch('/?q=hust')
    await waitFor(() => expect(screen.getByText('Tạm thời không kết nối.')).toBeTruthy())

    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /kết quả sau khi thử lại/i })).toBeTruthy()
    })
    expect(searchDocuments).toHaveBeenCalledTimes(2)
  })

  it('renders API errors without losing the search form', async () => {
    searchDocuments.mockRejectedValue(new Error('Máy chủ đang gặp sự cố.'))
    renderSearch('/?q=hust')
    await waitFor(() => expect(screen.getByText('Máy chủ đang gặp sự cố.')).toBeTruthy())
    expect(screen.getByRole('textbox').value).toBe('hust')
  })
})

describe('DocumentDetailPage', () => {
  it('renders a not-found error from the API', async () => {
    getDocument.mockRejectedValue(Object.assign(new Error('Không tìm thấy tài liệu.'), { status: 404 }))
    render(<MemoryRouter initialEntries={['/documents/missing']}><Routes><Route path="/documents/:id" element={<DocumentDetailPage />} /></Routes></MemoryRouter>)
    await waitFor(() => expect(screen.getByText('Không tìm thấy tài liệu.')).toBeTruthy())
    expect(screen.getByRole('link', { name: /về kết quả tìm kiếm/i })).toBeTruthy()
  })
})
