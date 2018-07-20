//@flow
import {GanttService} from './gantt.service';
import {StoreService} from './store.service';
import {CalendarService} from './calendar.service';
import {JiraService} from './jira.service';
import {PreferenceService} from './PreferenceService';

export const ganttService = GanttService;
export const calendarService = CalendarService;
export const jiraService = JiraService;

export const preferenceService = PreferenceService;

export const storeService = new StoreService();


storeService.store.subscribe(preferenceService.handleStateChange);
