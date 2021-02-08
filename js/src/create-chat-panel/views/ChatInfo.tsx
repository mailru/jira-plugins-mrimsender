import { observer } from 'mobx-react';
import React from 'react';
import styled from '@emotion/styled';
import { gridSize } from '@atlaskit/theme';
import { ChatInfoType } from '../stores/LoadingService';

type ChatInfoProps = {
  chatInfo: ChatInfoType;
};

export const StyledContainer = styled.div`
  display: flex;
  justify-content: space-between;
  line-height: 1.5;
  margin-bottom: ${gridSize() / 2}px;
`;

export const StyledLabel = styled.span`
  font-weight: 500;
  margin-right: 24px;
`;

const StyledValue = styled.span`
  text-align: right;
`;

export const ChatInfo = observer((props: ChatInfoProps) => {
  const { chatInfo } = props;
  return (
    <div>
      <StyledContainer>
        <StyledLabel>Myteam chat:</StyledLabel>
        <StyledValue>{chatInfo.name}</StyledValue>
      </StyledContainer>
      <StyledContainer>
        <StyledLabel>Chat link:</StyledLabel>
        <StyledValue>
          <a href={chatInfo.link} target="_blank" rel="noreferrer">
            {chatInfo.link}
          </a>
        </StyledValue>
      </StyledContainer>
    </div>
  );
});
