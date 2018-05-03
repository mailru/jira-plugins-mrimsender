/* eslint-disable flowtype/require-valid-file-annotation */
import {createStore} from 'redux';

import {GanttService} from './gantt.service';
import {ganttReducer} from './gantt.reducer';
import {GanttTeamService} from './gantt.team.service';
import {StoreService} from './store.service';
import {CalendarService} from './calendar.service';
import {JiraService} from './jira.service';
import {PreferenceService} from './PreferenceService';

import {defaultOptions} from '../app-gantt/staticOptions';


export const ganttService = GanttService;
export const ganttTeamService = GanttTeamService;
export const calendarService = CalendarService;
export const jiraService = JiraService;

export const preferenceService = PreferenceService;

export const store = createStore(
    ganttReducer,
    {
        options: {
            ...defaultOptions,
            ...preferenceService.getOptions()
        }
    }
);
export const storeService = new StoreService(store);
