import React from 'react'
import ReactDOM from 'react-dom'
import { CacheProvider } from '@emotion/core'
import createCache from '@emotion/cache'
import { QueryClient, QueryClientProvider } from 'react-query'
import { I18n } from '@atlassian/wrm-react-i18n'
import EmptyPageContainer from '@shared/components/styled/EmptyPageContainer'
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
        {projectKey === null ? (
          <EmptyPageContainer h4={16} p={24}>
            <h2>
              {I18n.getText(
                'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.error.project'
              )}
            </h2>
          </EmptyPageContainer>
        ) : (
          <AccessRequestConfiguration projectKey={projectKey} />
        )}
      </QueryClientProvider>
    </CacheProvider>,
    root
  )
}
