import styled from '@emotion/styled';
import React, { ReactElement } from 'react';

type Props = {
  className?: string;
  chatId: string;
  chatTitle: string;
  href?: string;
  disabled?: boolean;
  target?: string;
  rel?: string;
};

const TitleLink = styled.span`
  font-weight: 700;
  font-size: 16px;

  a.disabled {
    cursor: not-allowed;
    opacity: 0.6;
    text-decoration: none;
  }

  span {
    font-size: 10px;
    opacity: 0.5;
    vertical-align: top;
    margin-left: 5px;
  }
`;

export const ChatName = ({ className, chatId, chatTitle, href, target, rel, disabled }: Props): ReactElement => {
  return (
    <TitleLink className={className}>
      <a
        className={disabled ? 'disabled' : ''}
        target={target || '_blank'}
        href={disabled ? undefined : href}
        rel={rel || 'noreferrer'}>
        {chatTitle}
      </a>
      <span>{chatId}</span>
    </TitleLink>
  );
};
