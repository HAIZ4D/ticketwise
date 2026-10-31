import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { SlaMeter } from './SlaMeter.tsx'

const CREATED = '2026-11-01T10:00:00Z'
const DUE = '2026-11-01T18:00:00Z'

function fillOf(container: HTMLElement) {
  return container.querySelector<HTMLElement>('.sla-fill')
}

describe('SlaMeter', () => {
  it('shows how much of the time has been used', () => {
    const { container } = render(
      <SlaMeter state="ON_TRACK" createdAt={CREATED} dueAt={DUE} now={Date.parse('2026-11-01T12:00:00Z')} />,
    )

    expect(screen.getByText('On track')).toBeInTheDocument()
    expect(screen.getByText('6h 0m left')).toBeInTheDocument()
    expect(fillOf(container)).toHaveStyle({ width: '25%' })
  })

  it('fills the bar and says how late a ticket is once overdue', () => {
    const { container } = render(
      <SlaMeter state="OVERDUE" createdAt={CREATED} dueAt={DUE} now={Date.parse('2026-11-01T20:30:00Z')} />,
    )

    expect(screen.getByText('Overdue')).toBeInTheDocument()
    expect(screen.getByText('2h 30m late')).toBeInTheDocument()
    expect(fillOf(container)).toHaveStyle({ width: '100%' })
  })

  it('shows only the result once a ticket is finished', () => {
    const { container } = render(
      <SlaMeter state="MET" createdAt={CREATED} dueAt={DUE} now={Date.parse('2026-11-05T12:00:00Z')} />,
    )

    expect(screen.getByText('Met')).toBeInTheDocument()
    expect(fillOf(container)).toBeNull()
  })

  it('shows nothing for a ticket closed without being resolved', () => {
    const { container } = render(
      <SlaMeter state="NONE" createdAt={CREATED} dueAt={DUE} now={Date.parse('2026-11-05T12:00:00Z')} />,
    )

    expect(container).toBeEmptyDOMElement()
  })
})
