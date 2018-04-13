import React from 'react';
import ReactDOM from 'react-dom';

import {Provider} from 'react-redux';

import LayerManager from '@atlaskit/layer-manager';

// eslint-disable-next-line import/no-extraneous-dependencies
import AJS from 'AJS';
// eslint-disable-next-line import/no-extraneous-dependencies
import Backbone from 'backbone';

import {GanttTeams} from './GanttTeams';

import {collectTopMailCounterScript} from '../common/top-mail-ru';

import {calendarService, store, ganttTeamService} from '../service/services';
import {CalendarActionCreators, GanttTeamActionCreators} from '../service/gantt.reducer';

import './gantt-teams.less';


AJS.toInit(function() {
    try {
        collectTopMailCounterScript();

        /* Router */
        const ViewRouter = Backbone.Router.extend({
            routes: {
                'calendar=:calendar': 'setCalendar'
            },
            setCalendar: function (id) {
                calendarService
                    .getCalendar(id)
                    .then(calendar => {
                        store.dispatch(CalendarActionCreators.setCalendar({...calendar, id}, []));
                    });
                ganttTeamService
                    .getTeams(id)
                    .then(teams => {
                        store.dispatch(GanttTeamActionCreators.setTeams(teams));
                    });
            }
        });

        new ViewRouter();

        ReactDOM.render(
            <Provider store={store}>
                <LayerManager>
                    <GanttTeams/>
                </LayerManager>
            </Provider>,
            document.getElementById('gantt-teams')
        );

        Backbone.history.start();
    } catch (e) {
        console.error(e);
    }
});
