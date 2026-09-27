import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createSearchApi } from '../api/searchApi'
import { getDocument, searchDocuments } from '../api/client'

function jsonResponse(body, options = {}) {
  return {
    ok: options.ok ?? true,
    status: options.status ?? 200,
    json: vi.fn().mockResolvedValue(body),
  }
}

const validSearchResponse = {
  query: 'học bổng & tuyển sinh?',
  total: 1,
  page: 0,
  size: 10,
  results: [],
}

describe('backend API client', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('encodes search query and sends zero-based pagination', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse(validSearchResponse))

    await searchDocuments({ query: '  học bổng & tuyển sinh?  ', page: 2, size: 10 })

    const requestUrl = new URL(fetchMock.mock.calls[0][0])
    expect(requestUrl.pathname).toBe('/api/search')
    expect(Object.fromEntries(requestUrl.searchParams)).toEqual({
      q: 'học bổng & tuyển sinh?',
      page: '2',
      size: '10',
    })
  })

  it('rejects blank queries without calling fetch', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')

    await expect(searchDocuments({ query: '   ' })).rejects.toMatchObject({ status: 400 })
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('converts network failures into ApiError without a fake HTTP status', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(searchDocuments({ query: 'hust' })).rejects.toMatchObject({
      name: 'ApiError',
      message: 'Không thể kết nối đến máy chủ.',
      status: undefined,
    })
  })

  it('converts HTTP failures into ApiError with status', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse({}, { ok: false, status: 500 }))

    await expect(searchDocuments({ query: 'hust' })).rejects.toMatchObject({
      name: 'ApiError',
      status: 500,
    })
  })

  it('preserves 404 for missing documents', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse({}, { ok: false, status: 404 }))

    await expect(getDocument('missing/id')).rejects.toMatchObject({
      name: 'ApiError',
      status: 404,
      message: 'Không tìm thấy tài liệu.',
    })

    const requestUrl = new URL(globalThis.fetch.mock.calls[0][0])
    expect(requestUrl.pathname).toBe('/api/documents/missing%2Fid')
  })

  it('keeps abort errors distinguishable from network errors', async () => {
    const abortError = new DOMException('Aborted', 'AbortError')
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(abortError)

    await expect(searchDocuments({ query: 'hust' })).rejects.toBe(abortError)
  })

  it('rejects malformed successful responses', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse({ total: 1, results: [] }))

    await expect(searchDocuments({ query: 'hust' })).rejects.toMatchObject({
      name: 'ApiError',
      message: 'Phản hồi tìm kiếm không đúng định dạng.',
    })
  })

  it('rejects malformed result items', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse({
      ...validSearchResponse,
      results: [{ title: 'Thiếu id và url' }],
    }))

    await expect(searchDocuments({ query: 'hust' })).rejects.toMatchObject({
      name: 'ApiError',
      message: 'Phản hồi tìm kiếm không đúng định dạng.',
    })
  })

  it('selects mock and backend implementations by mode', () => {
    const mockApi = { searchDocuments: vi.fn(), getDocument: vi.fn() }
    const backendApi = { searchDocuments: vi.fn(), getDocument: vi.fn() }

    expect(createSearchApi({ mode: 'mock', mockApi, backendApi })).toBe(mockApi)
    expect(createSearchApi({ mode: 'api', mockApi, backendApi })).toBe(backendApi)
  })
})
