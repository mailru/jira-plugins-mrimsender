/* eslint-disable flowtype/require-valid-file-annotation */
import React from 'react';
import ReactDOM from 'react-dom';

import {Provider} from 'react-redux';

import LayerManager from '@atlaskit/layer-manager';

// eslint-disable-next-line import/no-extraneous-dependencies
import AJS from 'AJS';
// eslint-disable-next-line import/no-extraneous-dependencies
import Backbone from 'backbone';

import {GanttTeams} from './GanttTeams';

import {calendarService, teamsStore, ganttTeamService} from '../service/gantt.team.store';
import {CalendarActionCreators, GanttTeamActionCreators} from '../service/gantt.teams.reducer';

import './gantt-teams.less';


AJS.toInit(() => {
    try {
        /* Router */
        const ViewRouter = Backbone.Router.extend({
            routes: {
                'calendar=:calendar': 'setCalendar'
            },
            setCalendar (id) {
                calendarService
                    .getCalendar(id)
                    .then(calendar => {
                        teamsStore.dispatch(CalendarActionCreators.setCalendar({...calendar, id}, []));
                    });
                ganttTeamService
                    .getTeams(id)
                    .then(teams => {
                        teamsStore.dispatch(GanttTeamActionCreators.setTeams(teams));
                    });
            }
        });

        // eslint-disable-next-line no-unused-vars
        const viewRouter = new ViewRouter();

        ReactDOM.render(
            <Provider store={teamsStore}>
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
