import { env } from '../config/env'
import { ApiError } from './types'

function isAbortError(error) {
  return error?.name === 'AbortError'
}

function errorMessageForStatus(status) {
  if (status === 400) return 'Yêu cầu tìm kiếm không hợp lệ.'
  if (status === 404) return 'Không tìm thấy tài liệu.'
  if (status >= 500) return 'Máy chủ đang gặp sự cố. Vui lòng thử lại.'
  return 'Không thể tải dữ liệu từ máy chủ.'
}

async function readJson(response) {
  try {
    return await response.json()
  } catch (error) {
    throw new ApiError('Phản hồi từ máy chủ không hợp lệ.', {
      status: response.status,
      cause: error,
    })
  }
}

export async function requestJson(path, { signal } = {}) {
  const url = new URL(path, `${env.apiBaseUrl}/`)

  let response
  try {
    response = await fetch(url, { signal })
  } catch (error) {
    if (isAbortError(error)) throw error
    throw new ApiError('Không thể kết nối đến máy chủ.', { cause: error })
  }

  if (!response.ok) {
    throw new ApiError(errorMessageForStatus(response.status), { status: response.status })
  }

  return readJson(response)
}

function isValidSearchResult(result) {
  return result && typeof result === "object" && typeof result.id === "string" && typeof result.url === "string"
}

function validateSearchResponse(data) {
  if (
    !data ||
    typeof data !== 'object' ||
    typeof data.query !== 'string' ||
    !Number.isInteger(data.total) ||
    data.total < 0 ||
    !Number.isInteger(data.page) ||
    data.page < 0 ||
    !Number.isInteger(data.size) ||
    data.size <= 0 ||
    !Array.isArray(data.results) ||
    data.results.some((result) => !isValidSearchResult(result))
  ) {
    throw new ApiError('Phản hồi tìm kiếm không đúng định dạng.')
  }

  return data
}

function validateDocument(data) {
  if (
    !data ||
    typeof data !== 'object' ||
    typeof data.id !== 'string' ||
    typeof data.url !== 'string' ||
    typeof data.contentType !== 'string' ||
    typeof data.text !== 'string'
  ) {
    throw new ApiError('Phản hồi tài liệu không đúng định dạng.')
  }

  return data
}

export async function searchDocuments({ query, page = 0, size = 10, signal } = {}) {
  const trimmedQuery = String(query || '').trim()
  if (!trimmedQuery) {
    throw new ApiError('Vui lòng nhập từ khóa tìm kiếm.', { status: 400 })
  }
  if (!Number.isInteger(page) || page < 0 || !Number.isInteger(size) || size <= 0) {
    throw new ApiError('Tham số phân trang không hợp lệ.', { status: 400 })
  }

  const params = new URLSearchParams({
    q: trimmedQuery,
    page: String(page),
    size: String(size),
  })
  const data = await requestJson(`/api/search?${params.toString()}`, { signal })
  return validateSearchResponse(data)
}

export async function getDocument(id, { signal } = {}) {
  const documentId = String(id || '').trim()
  if (!documentId) {
    throw new ApiError('Mã tài liệu không hợp lệ.', { status: 400 })
  }

  const data = await requestJson(`/api/documents/${encodeURIComponent(documentId)}`, { signal })
  return validateDocument(data)
}
