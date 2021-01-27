import React from 'react';
import ReactDOM from 'react-dom';

import { configure } from 'mobx';
import { ChatPanel } from './views/ChatPanel';
import { ChatPanelStore } from './stores/ChatPanelStore';
import legacyIssueApi from 'jira/issues/search/legacyissue';
import { IntlProvider } from 'react-intl';
import EventTypes from 'jira/util/events/types';
import Events from 'jira/util/events';
import { ErrorView, makeFaultTolerantComponent } from './views/ErrorView';

const PANEL_CONTAINER_ID_SELECTOR = '#myteam-chat-creation-panel-container';

function memoizeStoreCreation(createStoreFunc: (issueKey: string) => ChatPanelStore) {
  const cache: { [key: string]: ChatPanelStore } = {};
  return (issueKey: string) => {
    if (issueKey in cache) return cache[issueKey];
    else {
      cache[issueKey] = createStoreFunc(issueKey);
      return cache[issueKey];
    }
  };
}

const createStore = memoizeStoreCreation((issueKey: string): ChatPanelStore => new ChatPanelStore(issueKey));

const ErrorBoundary = makeFaultTolerantComponent(ErrorView);

function renderMyteamChatPanel(reactDomRoot: HTMLElement) {
  const issueKey: string = legacyIssueApi.getIssueKey();
  const memoizedStore = createStore(issueKey);
  ReactDOM.render(
    <ErrorBoundary>
      <IntlProvider locale="en">
        <ChatPanel store={memoizedStore} />
      </IntlProvider>
    </ErrorBoundary>,
    reactDomRoot,
  );
}

function renderAllContentInContext($context: JQuery<any>) {
  if ($context.length === 0) return;
  try {
    // for each found custom table cfId root container on viewing page
    $context
      .find(PANEL_CONTAINER_ID_SELECTOR)
      .get()
      .forEach((container) => {
        const affected = $.contains($context[0], container);
        if (!affected) {
          console.debug('Found out that panel container is not affected => no render occurred');
          return;
        }
        if (container == null) {
          console.error('Myteam chat panel container not found');
          return;
        }
        renderMyteamChatPanel(container);
      });
  } catch (e) {
    console.error('Error occurred during renderAllContent function called', e);
  }
}

export function init() {
  configure({ enforceActions: 'observed', isolateGlobalState: true });

  const listener = (event: any, context: any) => {
    try {
      let $context;
      if (event.type === EventTypes.ISSUE_REFRESHED) {
        $context = $(document);
      } else {
        $context = context;
      }
      renderAllContentInContext($context);
    } catch (e) {
      console.error('Myteam chat panel an error occured in events listener', e);
    }
  };
  Events.bind(EventTypes.NEW_CONTENT_ADDED, listener);
  Events.bind(EventTypes.ISSUE_REFRESHED, listener);
  renderAllContentInContext($(document));
}
