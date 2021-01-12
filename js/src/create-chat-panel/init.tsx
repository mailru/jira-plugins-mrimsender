import React from 'react';
import ReactDOM from 'react-dom';

import { configure } from 'mobx';
import { ChatPanel } from './views/ChatPanel';
import { ChatPanelStore } from './stores/ChatPanelStore';
import legacyIssueApi from 'jira/issues/search/legacyissue';
import { IntlProvider } from 'react-intl';

export function init() {
  configure({ enforceActions: 'observed', isolateGlobalState: true });
  const root = document.getElementById('myteam-chat-creation-panel-container');

  if (root !== null) {
    const issueKey: string = legacyIssueApi.getIssueKey();
    ReactDOM.render(
      <IntlProvider locale="en">
        <ChatPanel store={new ChatPanelStore(issueKey)} />
      </IntlProvider>,
      root,
    );
  } else {
    console.log('myteam-chat-creation-panel id not found !!!');
  }
}
