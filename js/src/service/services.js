import {createStore} from 'redux';

import {GanttService} from './gantt.service';
import {ganttReducer} from './gantt.reducer';
import {StoreService} from './store.service';
import {CalendarService} from './calendar.service';


export const ganttService = new GanttService();
export const calendarService = new CalendarService();

export const store = createStore(ganttReducer, {});
export const storeService = new StoreService(store);
