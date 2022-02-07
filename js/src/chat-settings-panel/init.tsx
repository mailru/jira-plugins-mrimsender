import React from 'react';
import ReactDOM from 'react-dom';
import {CacheProvider, EmotionCache} from '@emotion/core';
import createCache from '@emotion/cache';

const PANEL_CONTAINER_ID_SELECTOR = '#myteam-chat-settings-container';


function renderMyteamChatPanel(reactDomRoot: HTMLElement, emotionCache: EmotionCache) {
    ReactDOM.render(
        <CacheProvider value={emotionCache}>
            <div>Тут будут настройки</div>
        </CacheProvider>,
        reactDomRoot,
    );
}

function renderAllContentInContext($context: JQuery<any>, emotionCache: EmotionCache) {
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
                renderMyteamChatPanel(container, emotionCache);
            });
    } catch (e) {
        console.error('Error occurred during renderAllContent function called', e);
    }
}

export function init() {
    const emotionCache = createCache({
        key: 'myteam-styles',
    });
    renderAllContentInContext($(document), emotionCache);
}
