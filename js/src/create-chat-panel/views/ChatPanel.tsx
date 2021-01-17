import React from 'react';
import { observer } from 'mobx-react';
import { ChatPanelStore } from '../stores/ChatPanelStore';
import { CreateChatDialog } from './CreateChatDialog';
import { ModalTransition } from '@atlaskit/modal-dialog';
import { I18n } from '@atlassian/wrm-react-i18n';
import styled from '@emotion/styled';
import { B300, N200, N40 } from '@atlaskit/theme/colors';
import { gridSize } from '@atlaskit/theme';

const StyledPanelContainerStatic = styled.div`
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

const StyledPanelContainer = styled.div`
  height: ${gridSize() * gridSize()}px;
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
  :hover {
    border-color: ${B300};
    border-style: solid;
    opacity: 0.5;
  }
`;

const StyledChatLink = styled.a`
  padding: 10px;'
`;

type CreateChatPanelProps = {
  store: ChatPanelStore;
};

export const ChatPanel = observer((props: CreateChatPanelProps) => {
  const { store } = props;
  // TODO maybe should show those errors somehow
  if (store.hasErrors) return null;

  if (store.chatAlreadyExist) {
    return (
      <StyledPanelContainerStatic>
        <StyledChatLink href={store.chatLink} target="_blank" rel="noreferrer">
          {store.chatName}
        </StyledChatLink>
      </StyledPanelContainerStatic>
    );
  }
  return (
    <>
      <StyledPanelContainer onClick={store.openCreateChatDialog}>
        <div>{I18n.getText('ru.mail.jira.plugins.myteam.createChat.panel.title')}</div>
      </StyledPanelContainer>
      <ModalTransition>
        {store.isCreateChatDialogOpen && store.dialogData && (
          <CreateChatDialog
            chatCreationData={store.dialogData}
            closeDialog={store.closeCreateChatDialog}
            createChat={store.createChat}
          />
        )}
      </ModalTransition>
    </>
  );
});
