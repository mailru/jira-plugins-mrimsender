import React from 'react';
import ReactDOM from 'react-dom';
import { CacheProvider } from '@emotion/core';
import createCache from '@emotion/cache';
import ManageFilterSubscriptions from './components/ManageFilterSubscriptions';

const CONTAINER_ID_SELECTOR = 'myteam-filter-subscriptions-container';

export default function init(): void {
  const emotionCache = createCache({
    key: 'myteam-styles',
  });

  const root = document.getElementById(CONTAINER_ID_SELECTOR);

  ReactDOM.render(
    <CacheProvider value={emotionCache}>
      <ManageFilterSubscriptions />
    </CacheProvider>,
    root,
  );
}
