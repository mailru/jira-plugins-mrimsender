import React from 'react'
import ReactDOM from 'react-dom'
import { CacheProvider } from '@emotion/core'
import createCache from '@emotion/cache'
import { QueryClient, QueryClientProvider } from 'react-query'
import ManageFilterSubscriptions from './components/ManageFilterSubscriptions'

const CONTAINER_ID_SELECTOR = 'myteam-filter-subscriptions-container'

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
    key: 'myteam-styles',
  })

  const root = document.getElementById(CONTAINER_ID_SELECTOR)

  ReactDOM.render(
    <CacheProvider value={emotionCache}>
      <QueryClientProvider client={queryClient}>
        <ManageFilterSubscriptions />
      </QueryClientProvider>
    </CacheProvider>,
    root
  )
}
