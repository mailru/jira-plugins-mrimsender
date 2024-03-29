import React from 'react'
import ReactDOM from 'react-dom'
import AJS from 'AJS'

import { configure } from 'mobx'
import JIRA from 'JIRA'
import EventTypes from 'jira/util/events/types'
import Events from 'jira/util/events'
import { CacheProvider, EmotionCache } from '@emotion/core'
import createCache from '@emotion/cache'
import { IntlProvider } from 'react-intl-next'
import { ErrorView, makeFaultTolerantComponent } from './views/ErrorView'
import ChatPanelStore from './stores/ChatPanelStore'
import { ChatPanel, StyledChatPanelContainer } from './views/ChatPanel'

const PANEL_CONTAINER_ID_SELECTOR = '#myteam-chat-creation-panel-container'

function memoizeStoreCreation(
  createStoreFunc: (issueKey: string) => ChatPanelStore
) {
  const cache: { [key: string]: ChatPanelStore } = {}
  return (issueKey: string) => {
    if (issueKey in cache) return cache[issueKey]

    cache[issueKey] = createStoreFunc(issueKey)
    return cache[issueKey]
  }
}

const createStore = memoizeStoreCreation(
  (issueKey: string): ChatPanelStore => new ChatPanelStore(issueKey)
)

const ErrorBoundary = makeFaultTolerantComponent(ErrorView)

function renderMyteamChatPanel(
  reactDomRoot: HTMLElement,
  emotionCache: EmotionCache
) {
  let issueKey: string = JIRA.Issue.getIssueKey()
  if (!issueKey) {
    issueKey = AJS.$("meta[name='ajs-issue-key']").attr('content')
    if (!issueKey) {
      issueKey = AJS.$('#key-val').text()
    }
  }
  const memoizedStore = createStore(issueKey)
  ReactDOM.render(
    <CacheProvider value={emotionCache}>
      <ErrorBoundary>
        <IntlProvider locale="en">
          <StyledChatPanelContainer>
            <ChatPanel store={memoizedStore} />
          </StyledChatPanelContainer>
        </IntlProvider>
      </ErrorBoundary>
    </CacheProvider>,
    reactDomRoot
  )
}

function renderAllContentInContext(
  $context: JQuery<any>,
  emotionCache: EmotionCache
) {
  if ($context.length === 0) return
  try {
    // for each found custom table cfId root container on viewing page
    $context
      .find(PANEL_CONTAINER_ID_SELECTOR)
      .get()
      .forEach((container) => {
        const isAffected = $.contains($context[0], container)
        if (!isAffected) {
          console.debug(
            'Found out that panel container is not affected => no render occurred'
          )
          return
        }
        if (container == null) {
          console.error('Myteam chat panel container not found')
          return
        }
        renderMyteamChatPanel(container, emotionCache)
      })
  } catch (e) {
    console.error('Error occurred during renderAllContent function called', e)
  }
}

export default function init(): void {
  configure({ enforceActions: 'observed', isolateGlobalState: true })
  const emotionCache = createCache({
    key: 'myteam-styles',
  })

  const listener = (event: any, context: any) => {
    try {
      let $context
      if (event.type === EventTypes.ISSUE_REFRESHED) {
        $context = $(document)
      } else {
        $context = context
      }
      renderAllContentInContext($context, emotionCache)
    } catch (e) {
      console.error('Myteam chat panel an error occured in events listener', e)
    }
  }
  Events.bind(EventTypes.NEW_CONTENT_ADDED, listener)
  Events.bind(EventTypes.ISSUE_REFRESHED, listener)
  renderAllContentInContext($(document), emotionCache)
}
