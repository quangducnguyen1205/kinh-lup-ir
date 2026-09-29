import { describe, expect, it } from 'vitest'
import { getSafeExternalUrl } from '../utils/safeUrl'

describe('getSafeExternalUrl', () => {
  it('allows http and https URLs', () => {
    expect(getSafeExternalUrl('https://hust.edu.vn/a')).toBe('https://hust.edu.vn/a')
    expect(getSafeExternalUrl('http://hust.edu.vn/a')).toBe('http://hust.edu.vn/a')
  })

  it('rejects unsafe, malformed and blank URLs', () => {
    expect(getSafeExternalUrl('javascript:alert(1)')).toBeNull()
    expect(getSafeExternalUrl('data:text/html,unsafe')).toBeNull()
    expect(getSafeExternalUrl('not a url')).toBeNull()
    expect(getSafeExternalUrl('')).toBeNull()
  })
})
