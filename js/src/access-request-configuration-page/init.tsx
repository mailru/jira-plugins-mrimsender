import React from 'react'
import ReactDOM from 'react-dom'
import { CacheProvider } from '@emotion/core'
import createCache from '@emotion/cache'
import { QueryClient, QueryClientProvider } from 'react-query'
import AccessRequestConfiguration from './components/AccessRequestConfiguration'

const PANEL_CONTAINER_ID_SELECTOR =
  'myteam-access-request-configuration-container'

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

export default function init(): void {
  const emotionCache = createCache({
    key: 'myteam-access-request-configuration-styles',
  })

  const root = document.getElementById(PANEL_CONTAINER_ID_SELECTOR)
  const projectKey = new URLSearchParams(window.location.search).get('project')

  ReactDOM.render(
    <CacheProvider value={emotionCache}>
      <QueryClientProvider client={queryClient}>
        <AccessRequestConfiguration projectKey={projectKey} />
      </QueryClientProvider>
    </CacheProvider>,
    root
  )
}
