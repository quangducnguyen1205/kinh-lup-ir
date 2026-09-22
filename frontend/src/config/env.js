const DEFAULT_API_BASE_URL = 'http://localhost:8080'
const VALID_API_MODES = new Set(['mock', 'api'])

function readApiMode(value) {
  const mode = value?.trim().toLowerCase() || 'mock'
  if (!VALID_API_MODES.has(mode)) throw new Error(`VITE_API_MODE không hợp lệ: ${mode}`)
  return mode
}

const rawBaseUrl = import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL

export const env = Object.freeze({
  apiBaseUrl: rawBaseUrl.trim().replace(/\/+$/, ''),
  apiMode: readApiMode(import.meta.env.VITE_API_MODE),
})
