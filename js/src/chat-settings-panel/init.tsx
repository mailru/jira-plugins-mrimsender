import React from 'react';
import ReactDOM from 'react-dom';
import { CacheProvider, css, Global } from '@emotion/core';
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
      <Global
        styles={css`
          #${PANEL_CONTAINER_ID_SELECTOR} {
            padding: 40px;
            background: white;
            display: flex;
            justify-content: center;
          }
        `}
      />
      <ChatIssueCreationSettings chatId={chatId} />
    </CacheProvider>,
    root,
  );
}
