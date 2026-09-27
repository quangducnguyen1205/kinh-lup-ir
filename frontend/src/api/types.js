/**
 * @typedef {Object} SearchResult
 * @property {string} id
 * @property {string | null | undefined} title
 * @property {string} url
 * @property {string | null | undefined} snippet
 * @property {number} score
 * @property {string | null | undefined} publishedAt
 */

/**
 * @typedef {Object} SearchResponse
 * @property {string} query
 * @property {number} total
 * @property {number} page
 * @property {number} size
 * @property {SearchResult[]} results
 */

/**
 * @typedef {Object} DocumentDetail
 * @property {string} id
 * @property {string} url
 * @property {string | null | undefined} title
 * @property {string} contentType
 * @property {string} text
 * @property {string | null | undefined} publishedAt
 * @property {string | null | undefined} lastCrawledAt
 */

export class ApiError extends Error {
  constructor(message, { status, cause } = {}) {
    super(message, { cause })
    this.name = 'ApiError'
    this.status = status
  }
}
