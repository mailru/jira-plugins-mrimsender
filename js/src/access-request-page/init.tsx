import React from 'react'
import ReactDOM from 'react-dom'
import { CacheProvider } from '@emotion/core'
import createCache from '@emotion/cache'
import { QueryClient, QueryClientProvider } from 'react-query'
import AccessRequest from './components/AccessRequest'

const PANEL_CONTAINER_ID_SELECTOR = 'myteam-access-request-container'

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
    key: 'myteam-access-request-styles',
  })

  const root = document.getElementById(PANEL_CONTAINER_ID_SELECTOR)
  const issueKey = new URLSearchParams(window.location.search).get('issueKey')

  ReactDOM.render(
    <CacheProvider value={emotionCache}>
      <QueryClientProvider client={queryClient}>
        <AccessRequest issueKey={issueKey} />
      </QueryClientProvider>
    </CacheProvider>,
    root
  )
}
