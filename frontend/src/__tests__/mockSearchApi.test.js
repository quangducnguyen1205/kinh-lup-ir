import { describe, expect, it } from 'vitest'
import { createMockSearchApi } from '../api/mockSearchApi'

const api = createMockSearchApi({ delayMs: 0 })

describe('mock search API', () => {
  it('returns contract-shaped, paginated search results', async () => {
    const firstPage = await api.searchDocuments({ query: 'hust', page: 0, size: 10 })
    const secondPage = await api.searchDocuments({ query: 'HUST', page: 1, size: 10 })
    const thirdPage = await api.searchDocuments({ query: 'hust', page: 2, size: 10 })

    expect(firstPage).toMatchObject({ query: 'hust', total: 21, page: 0, size: 10 })
    expect(firstPage.results).toHaveLength(10)
    expect(secondPage.results).toHaveLength(10)
    expect(thirdPage.results).toHaveLength(1)
    expect(Object.keys(firstPage.results[0]).sort()).toEqual(['id', 'publishedAt', 'score', 'snippet', 'title', 'url'])
  })

  it('returns an empty result set for an unmatched query', async () => {
    await expect(api.searchDocuments({ query: 'khong-co-ket-qua', page: 0, size: 10 }))
      .resolves.toMatchObject({ total: 0, results: [] })
  })

  it('returns a 404 ApiError when a document does not exist', async () => {
    await expect(api.getDocument('unknown-id')).rejects.toMatchObject({
      name: 'ApiError',
      status: 404,
    })
  })

  it('can deterministically simulate a network failure', async () => {
    const failingApi = createMockSearchApi({
      delayMs: 0,
      error: { message: 'Mất kết nối mạng.', status: 0 },
    })

    await expect(failingApi.searchDocuments({ query: 'hust' })).rejects.toMatchObject({
      name: 'ApiError',
      message: 'Mất kết nối mạng.',
      status: 0,
    })
  })
})
