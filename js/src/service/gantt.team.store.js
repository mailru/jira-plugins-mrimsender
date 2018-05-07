/* eslint-disable flowtype/require-valid-file-annotation */
import {createStore} from 'redux';

import {ganttTeamsReducer} from './gantt.teams.reducer';
import {GanttTeamService} from './gantt.team.service';
import {CalendarService} from './calendar.service';

export const ganttTeamService = GanttTeamService;
export const calendarService = CalendarService;

export const teamsStore = createStore(ganttTeamsReducer);