import React from 'react';
import { observer } from 'mobx-react';
import { ChatPanelStore } from '../stores/ChatPanelStore';
import { CreateChatDialog } from './CreateChatDialog';
import { ModalTransition } from '@atlaskit/modal-dialog';
import { I18n } from '@atlassian/wrm-react-i18n';
import styled from '@emotion/styled';
import { gridSize } from '@atlaskit/theme';
import { ChatInfo } from './ChatInfo';
import Spinner from '@atlaskit/spinner';
import { ErrorView } from './ErrorView';
import LoadingButton from '@atlaskit/button/loading-button';
// eslint-disable-next-line @typescript-eslint/no-var-requires
const MyteamImage = require('../../assets/myteam.png');

export const StyledChatPanelContainer = styled.div`
  max-width: ${gridSize() * 50}px;
`;

const StyledSpinnerContainer = styled.div`
  text-align: center;
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
      <div>
        <LoadingButton
          isLoading={store.isDialogDataLoading}
          onClick={store.openCreateChatDialog}
          iconBefore={<img height={gridSize() * 2.5} src={MyteamImage.default} alt="Some image here" />}>
          {I18n.getText('ru.mail.jira.plugins.myteam.createChat.panel.title')}
        </LoadingButton>
      </div>
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
