import { mockDocuments } from '../data/mockDocuments'
import { ApiError } from './types'

function createAbortError() {
  return new DOMException('The operation was aborted.', 'AbortError')
}

function wait(delayMs, signal) {
  if (!delayMs) return Promise.resolve()

  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(createAbortError())
      return
    }

    const timeoutId = setTimeout(resolve, delayMs)
    signal?.addEventListener('abort', () => {
      clearTimeout(timeoutId)
      reject(createAbortError())
    }, { once: true })
  })
}

function toPositiveInteger(value, fallback) {
  const number = Number(value)
  return Number.isInteger(number) && number > 0 ? number : fallback
}

function toNonNegativeInteger(value, fallback) {
  const number = Number(value)
  return Number.isInteger(number) && number >= 0 ? number : fallback
}

function matchesQuery(document, normalizedQuery) {
  return [document.title, document.snippet, document.text, document.url]
    .filter(Boolean)
    .some((value) => value.toLocaleLowerCase('vi-VN').includes(normalizedQuery))
}

function asSearchResult(document) {
  const { id, title, url, snippet, score, publishedAt } = document
  return { id, title, url, snippet, score, publishedAt }
}

function asDocumentDetail(document) {
  const { id, url, title, contentType, text, publishedAt, lastCrawledAt } = document
  return { id, url, title, contentType, text, publishedAt, lastCrawledAt }
}

function toApiError(error) {
  if (error instanceof Error) return error
  return new ApiError(error?.message || 'Không thể tải dữ liệu mô phỏng.', {
    status: error?.status,
    cause: error,
  })
}

/**
 * Tạo API mock cùng interface với API adapter ở Phase 3.
 * @param {{ documents?: typeof mockDocuments, delayMs?: number, error?: Error | { message?: string, status?: number } }} options
 */
export function createMockSearchApi({ documents = mockDocuments, delayMs = 80, error } = {}) {
  async function respond(signal) {
    await wait(delayMs, signal)
    if (error) throw toApiError(error)
  }

  return {
    /** @returns {Promise<import('./types').SearchResponse>} */
    async searchDocuments({ query, page = 0, size = 10, signal } = {}) {
      await respond(signal)

      const normalizedQuery = String(query || '').trim().toLocaleLowerCase('vi-VN')
      const validPage = toNonNegativeInteger(page, 0)
      const validSize = toPositiveInteger(size, 10)
      const matchingDocuments = normalizedQuery
        ? documents.filter((document) => matchesQuery(document, normalizedQuery))
        : []

      const start = validPage * validSize
      return {
        query: String(query || '').trim(),
        total: matchingDocuments.length,
        page: validPage,
        size: validSize,
        results: matchingDocuments.slice(start, start + validSize).map(asSearchResult),
      }
    },

    /** @returns {Promise<import('./types').DocumentDetail>} */
    async getDocument(id, { signal } = {}) {
      await respond(signal)

      const document = documents.find((item) => item.id === id)
      if (!document) {
        throw new ApiError('Không tìm thấy tài liệu.', { status: 404 })
      }

      return asDocumentDetail(document)
    },
  }
}

export const mockSearchApi = createMockSearchApi()
