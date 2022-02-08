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

  const match = location.search.match(/chatId=([^&]+)/);

  const chatId = match ? match[1] : null;

  ReactDOM.render(
    <CacheProvider value={emotionCache}>
      <div>Тут будут настройки11</div>
      <ChatIssueCreationSettings chatId={chatId} />
    </CacheProvider>,
    root,
  );
}
