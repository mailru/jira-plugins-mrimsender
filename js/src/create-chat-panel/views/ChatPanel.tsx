import React from 'react';
import { observer } from 'mobx-react';
import { ChatPanelStore } from '../stores/ChatPanelStore';
import { CreateChatDialog } from './CreateChatDialog';
import { ModalTransition } from '@atlaskit/modal-dialog';
import { I18n } from '@atlassian/wrm-react-i18n';
import styled from '@emotion/styled';
import { gridSize } from '@atlaskit/theme';
import { ChatInfo } from './ChatInfo';
import Button from '@atlaskit/button';
import Spinner from '@atlaskit/spinner';
import { ErrorView } from './ErrorView';
// eslint-disable-next-line @typescript-eslint/no-var-requires
const MyteamImage = require('../../assets/myteam.png');

/*const StyledPanelContainer = styled.div`
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
  :hover {
    border-color: ${B300};
    border-style: solid;
    opacity: 0.5;
  }
`;*/

const StyledSpinnerContainer = styled.div`
  text-align: center;
`;

const StyledContainer = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

const StyledLabel = styled.span`
  font-weight: 500;
  margin-right: 24px;
`;

type CreateChatPanelProps = {
  store: ChatPanelStore;
};

export const ChatPanel = observer((props: CreateChatPanelProps) => {
  const { store } = props;

  if (store.error) return <ErrorView error={store.error} />;

  if (store.isLoading)
    return (
      <StyledSpinnerContainer>
        <Spinner size={'medium'} />
      </StyledSpinnerContainer>
    );

  if (store.chatAlreadyExist && store.chatInfo != null) {
    return <ChatInfo chatInfo={store.chatInfo} />;
  }

  return (
    <>
      <StyledContainer>
        <StyledLabel>Myteam chat: </StyledLabel>
        <Button
          onClick={store.openCreateChatDialog}
          iconBefore={<img height={gridSize() * 2.5} src={MyteamImage.default} alt="Some image here" />}>
          {I18n.getText('ru.mail.jira.plugins.myteam.createChat.panel.title')}
        </Button>
      </StyledContainer>
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
