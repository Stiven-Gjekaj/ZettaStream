import { render, screen } from '@testing-library/react'
import { expect, test } from 'vitest'
import App from './App'

test('the app shows its name', () => {
  render(<App />)
  expect(screen.getByRole('heading', { name: 'ZettaStream' })).toBeInTheDocument()
})
