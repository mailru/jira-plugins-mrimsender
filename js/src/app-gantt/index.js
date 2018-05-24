/* eslint-disable flowtype/require-valid-file-annotation */
import React from 'react';
import ReactDOM from 'react-dom';

import {Provider} from 'react-redux';

import LayerManager from '@atlaskit/layer-manager';
// eslint-disable-next-line import/no-extraneous-dependencies
import AJS from 'AJS';

import { collectTopMailCounterScript } from '../common/top-mail-ru';

import {storeService} from '../service/services';

import {App} from './App';

import './gantt.less';


AJS.toInit(() => {
    collectTopMailCounterScript();

    ReactDOM.render(
        <Provider store={storeService.store}>
            <LayerManager>
                <App/>
            </LayerManager>
        </Provider>,
        document.getElementById('gantt-actions')
    );
});
