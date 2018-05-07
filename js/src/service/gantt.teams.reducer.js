/* eslint-disable flowtype/require-valid-file-annotation */
import {combineReducers} from 'redux';

export const ganttTeamsReducer = combineReducers({
    calendar: calendarReducer,
    teams: ganttTeamReducer
});

const SET_CALENDAR = 'SET_CALENDAR';
const GANTT_SET_TEAMS = 'GANTT_SET_TEAMS';

export const CalendarActionCreators = {
    setCalendar: (calendar) => {
        return {
            type: SET_CALENDAR,
            calendar
        };
    }
};

function calendarReducer(state, action) {
    if (state === undefined) {
        return null;
    }

    switch (action.type) {
        case SET_CALENDAR:
            return action.calendar;
        default:
            return state;
    }
}

export const GanttTeamActionCreators = {
    setTeams: (teams) => {
        return {
            type: GANTT_SET_TEAMS,
            teams
        };
    }
};


function ganttTeamReducer(state, action) {
    if (state === undefined) {
        return [];
    }

    switch (action.type) {
        case GANTT_SET_TEAMS:
            return action.teams;
        default:
            return state;
    }
}