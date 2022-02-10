import React from 'react';
import ReactDOM from 'react-dom';
import { CacheProvider } from '@emotion/core';
import createCache from '@emotion/cache';
import ChatIssueCreationSettings from './components/ChatIssueCreationSettings';

const PANEL_CONTAINER_ID_SELECTOR = 'myteam-chat-settings-container';

export function init(): void {
  const emotionCache = createCache({
    key: 'myteam-styles',
  });

  const root = document.getElementById(PANEL_CONTAINER_ID_SELECTOR);

  const chatId = new URLSearchParams(location.search).get('chatId');

  ReactDOM.render(
    <CacheProvider value={emotionCache}>
      <ChatIssueCreationSettings chatId={chatId} />
    </CacheProvider>,
    root,
  );
}
