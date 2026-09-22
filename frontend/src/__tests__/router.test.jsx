import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { router } from '../app/router'

function renderAt(path) {
  const memoryRouter = createMemoryRouter(router.routes, { initialEntries: [path] })
  return render(<RouterProvider router={memoryRouter} />)
}

describe('application routes', () => {
  it('renders the search placeholder at the root route', () => {
    renderAt('/')
    expect(screen.getByRole('heading', { name: /search ui đang được xây dựng/i })).toBeTruthy()
  })

  it('renders the document detail placeholder with the route id', () => {
    renderAt('/documents/test-id')
    expect(screen.getByText('Document ID: test-id')).toBeTruthy()
  })
})
