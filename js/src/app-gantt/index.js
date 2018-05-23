/* eslint-disable flowtype/require-valid-file-annotation */
import React from 'react';
import ReactDOM from 'react-dom';

import {Provider} from 'react-redux';

import LayerManager from '@atlaskit/layer-manager';
// eslint-disable-next-line import/no-extraneous-dependencies
import AJS from 'AJS';
// eslint-disable-next-line import/no-extraneous-dependencies
import Backbone from 'backbone';
// eslint-disable-next-line import/no-extraneous-dependencies
import JIRA from 'JIRA';
// eslint-disable-next-line import/no-extraneous-dependencies

import { collectTopMailCounterScript } from '../common/top-mail-ru';

import {calendarService, ganttService, preferenceService, store} from '../service/services';
import { CalendarActionCreators } from '../service/gantt.reducer';

import {App} from './App';

import './gantt.less';


AJS.toInit(() => {
    JIRA.Loading.showLoadingIndicator();
    AJS.dim();
    try {
        collectTopMailCounterScript();

        /* Router */
        const ViewRouter = Backbone.Router.extend({
            routes: {
                'calendar=:calendar': 'setCalendar'
            },
            setCalendar(idString) {
                const id = parseInt(idString, 10);
                if (!Number.isNaN(id)) {
                    if (id === -1) {
                        const lastGantt = preferenceService.get('ru.mail.jira.gantt.lastGantt');
                        if (lastGantt) {
                            this.navigate(`calendar=${lastGantt}`, {trigger: true});
                        } else {
                            store.dispatch(CalendarActionCreators.setCalendar(null, []));
                        }
                    } else {
                        JIRA.Loading.showLoadingIndicator();
                        AJS.dim();
                        Promise
                            .all([calendarService.getCalendar(id), ganttService.getCalendarSprints(id), ganttService.getErrors(id)])
                            .then(
                                ([calendar, sprints, errors]) => store.dispatch(CalendarActionCreators.setCalendar(
                                    {...calendar, errors, id}, sprints
                                ))
                            )
                            .finally(() => {
                                JIRA.Loading.hideLoadingIndicator();
                                AJS.undim();
                            });
                    }
                } else {
                    store.dispatch(CalendarActionCreators.setCalendar(null, []));
                }
            }
        });

        // eslint-disable-next-line no-unused-vars
        const viewRouter = new ViewRouter();

        ReactDOM.render(
            <Provider store={store}>
                <LayerManager>
                    <App/>
                </LayerManager>
            </Provider>,
            document.getElementById('gantt-actions')
        );

        Backbone.history.start();
    } finally {
        JIRA.Loading.hideLoadingIndicator();
        AJS.undim();
    }
});
