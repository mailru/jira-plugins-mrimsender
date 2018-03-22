import {createStore} from 'redux';

import {GanttService} from './gantt.service';
import {ganttReducer} from './gantt.reducer';
import {StoreService} from './store.service';
import {CalendarService} from './calendar.service';
import {JiraService} from './jira.service';


export const ganttService = new GanttService();
export const calendarService = new CalendarService();
export const jiraService = new JiraService();

export const store = createStore(ganttReducer, {});
export const storeService = new StoreService(store);
