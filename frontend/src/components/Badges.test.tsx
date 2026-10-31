import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ClassifierBadge, PriorityBadge, StatusBadge } from './Badges.tsx'

describe('badges', () => {
  it('labels a Gemini result as AI', () => {
    render(<ClassifierBadge source="GEMINI" />)

    expect(screen.getByText('AI')).toBeInTheDocument()
  })

  it('labels a keyword result as Rules', () => {
    render(<ClassifierBadge source="RULES" />)

    expect(screen.getByText('Rules')).toBeInTheDocument()
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
