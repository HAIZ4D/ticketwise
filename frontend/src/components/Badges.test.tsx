import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ClassifierBadge, PriorityBadge, SlaBadge, StatusBadge } from './Badges.tsx'

describe('badges', () => {
  it('labels a Gemini result as AI', () => {
    render(<ClassifierBadge source="GEMINI" />)

    expect(screen.getByText('AI')).toBeInTheDocument()
  })

  it('labels a keyword result as Rules', () => {
    render(<ClassifierBadge source="RULES" />)

    expect(screen.getByText('Rules')).toBeInTheDocument()
  })

  it.each([
    ['ON_TRACK', 'On track'],
    ['DUE_SOON', 'Due soon'],
    ['OVERDUE', 'Overdue'],
    ['MET', 'Met'],
    ['MISSED', 'Missed'],
  ] as const)('labels the SLA state %s', (state, text) => {
    render(<SlaBadge state={state} />)

    expect(screen.getByText(text)).toBeInTheDocument()
  })

  it('shows no SLA badge when a ticket has no result', () => {
    const { container } = render(<SlaBadge state="NONE" />)

    expect(container).toBeEmptyDOMElement()
  })

  it('shows enum values in plain words', () => {
    render(
      <>
        <StatusBadge status="IN_PROGRESS" />
        <PriorityBadge priority="CRITICAL" />
      </>,
    )

    expect(screen.getByText('In progress')).toBeInTheDocument()
    expect(screen.getByText('Critical')).toBeInTheDocument()
  })
})
