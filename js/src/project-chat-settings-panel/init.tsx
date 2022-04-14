import React from 'react';
import ReactDOM from 'react-dom';
import { CacheProvider } from '@emotion/core';
import createCache from '@emotion/cache';
import ProjectIssueCreationSettings from './components/ProjectIssueCreationSettings';

const PANEL_CONTAINER_ID_SELECTOR = 'myteam-project-chat-settings-container';

export default function init(): void {
  const emotionCache = createCache({
    key: 'myteam-styles',
  });

  const root = document.getElementById(PANEL_CONTAINER_ID_SELECTOR);

  ReactDOM.render(
    <CacheProvider value={emotionCache}>
      <ProjectIssueCreationSettings />
    </CacheProvider>,
    root,
  );
}
