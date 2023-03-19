import styled from '@emotion/styled'
import React, { ReactElement } from 'react'

type Props = {
  className?: string
  chatTitle: string
  href?: string
  disabled?: boolean
  target?: string
  rel?: string
}

const TitleLink = styled.span`
  font-weight: 700;
  font-size: 16px;

  a.disabled {
    cursor: not-allowed;
    opacity: 0.6;
    text-decoration: none;
  }
`

export default function ChatName({
  className,
  chatTitle,
  href,
  target,
  rel,
  disabled,
}: Props): ReactElement {
  return (
    <TitleLink className={className}>
      <a
        className={disabled ? 'disabled' : ''}
        target={target || '_blank'}
        href={disabled ? '/#' : href}
        rel={rel || 'noreferrer'}
      >
        {chatTitle}
      </a>
    </TitleLink>
  )
}

ChatName.defaultProps = {
  className: undefined,
  href: undefined,
  disabled: undefined,
  target: undefined,
  rel: undefined,
}
