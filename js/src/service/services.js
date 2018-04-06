import {createStore} from 'redux';

import {GanttService} from './gantt.service';
import {ganttReducer} from './gantt.reducer';
import {GanttTeamService} from './gantt.team.service';
import {StoreService} from './store.service';
import {CalendarService} from './calendar.service';
import {JiraService} from './jira.service';
import {PreferenceService} from './PreferenceService';

import {defaultOptions} from '../app-gantt/staticOptions';


export const ganttService = new GanttService();
export const ganttTeamService = new GanttTeamService();
export const calendarService = new CalendarService();
export const jiraService = new JiraService();

export const preferenceService = new PreferenceService();
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
