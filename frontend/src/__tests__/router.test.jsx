import { render, screen, waitFor } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { router } from '../app/router'

function renderAt(path) {
  const memoryRouter = createMemoryRouter(router.routes, { initialEntries: [path] })
  return render(<RouterProvider router={memoryRouter} />)
}

describe('application routes', () => {
  it('renders the search experience at the root route', () => {
    renderAt('/')
    expect(screen.getByRole('heading', { name: /tìm đúng tài liệu/i })).toBeTruthy()
    expect(screen.getByRole('search')).toBeTruthy()
  })

  it('loads the document detail route', async () => {
    renderAt('/documents/hoc-bong-ky-1-2026')
    expect(screen.getByText('Đang tải tài liệu...')).toBeTruthy()
    await waitFor(() => expect(screen.getByRole('heading', { name: /thông báo học bổng/i })).toBeTruthy())
  })
})
