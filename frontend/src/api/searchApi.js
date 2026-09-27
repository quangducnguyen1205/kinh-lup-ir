import { env } from '../config/env'
import { getDocument as getBackendDocument, searchDocuments as searchBackendDocuments } from './client'
import { mockSearchApi } from './mockSearchApi'

const backendSearchApi = {
  searchDocuments: searchBackendDocuments,
  getDocument: getBackendDocument,
}

export function createSearchApi({ mode, mockApi = mockSearchApi, backendApi = backendSearchApi }) {
  return mode === 'mock' ? mockApi : backendApi
}

export const searchApi = createSearchApi({ mode: env.apiMode })
export const { searchDocuments, getDocument } = searchApi
