import { observer } from 'mobx-react';
import React from 'react';
import styled from '@emotion/styled';
import { gridSize } from '@atlaskit/theme';
import { N200, N40 } from '@atlaskit/theme/colors';
import { ChatInfoType } from '../stores/LoadingService';

const StyledPanelContainerStatic = styled.div`
  text-align: center;
  min-height: ${gridSize() * gridSize()}px;
  border-width: 3px;
  border-color: ${N40};
  border-style: dashed;
  border-radius: ${gridSize()}px;
  font-weight: 500;
  font-size: ${gridSize() * 2}px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: ${N200};
`;

const StyledChatLink = styled.a`
  padding: 10px;'
`;

type ChatInfoProps = {
  chatInfo: ChatInfoType;
};

export const ChatInfo = observer((props: ChatInfoProps) => {
  const { chatInfo } = props;
  return (
    <StyledPanelContainerStatic>
      <StyledChatLink href={chatInfo.link} target="_blank" rel="noreferrer">
        {chatInfo.name}
      </StyledChatLink>
    </StyledPanelContainerStatic>
  );
});
