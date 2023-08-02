import React from 'react'
import ReactDOM from 'react-dom'
import {CacheProvider, css, Global} from '@emotion/core'
import createCache from '@emotion/cache'
import ChatIssueCreationSettings from './components/ChatIssueCreationSettings'
import {QueryClient, QueryClientProvider} from "@tanstack/react-query";

const PANEL_CONTAINER_ID_SELECTOR = 'myteam-chat-settings-container'

export default function init(): void {
    const emotionCache = createCache({
        key: 'myteam-styles',
    })

    const root = document.getElementById(PANEL_CONTAINER_ID_SELECTOR)

    const chatId = new URLSearchParams(window.location.search).get('chatId')

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
            <QueryClientProvider client={queryClient}>
                <ChatIssueCreationSettings chatId={chatId}/>
            </QueryClientProvider>

        </CacheProvider>,
        root
    )
}
