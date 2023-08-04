import React from 'react'
import ReactDOM from 'react-dom'
import { CacheProvider } from '@emotion/core'
import createCache from '@emotion/cache'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import ProjectIssueCreationSettings from './components/ProjectIssueCreationSettings'

const PANEL_CONTAINER_ID_SELECTOR = 'myteam-project-chat-settings-container'

export default function init(): void {
  const emotionCache = createCache({
    key: 'myteam-styles',
  })

  const root = document.getElementById(PANEL_CONTAINER_ID_SELECTOR)

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        refetchOnReconnect: false,
        refetchOnWindowFocus: false,
        refetchIntervalInBackground: false,
        refetchInterval: false,
      },
    },
  })

  ReactDOM.render(
    <CacheProvider value={emotionCache}>
      <QueryClientProvider client={queryClient}>
        <ProjectIssueCreationSettings />
      </QueryClientProvider>
    </CacheProvider>,
    root
  )
}
