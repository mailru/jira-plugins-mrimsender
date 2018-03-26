import {combineReducers} from 'redux';

// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';

import {keyedConfigs} from '../app-gantt/scaleConfigs';
import {views} from '../app-gantt/views';
import {defaultOptions} from '../app-gantt/staticOptions';


export const ganttReducer = combineReducers({
    options: optionsReducer,
    calendar: calendarReducer,
    ganttReady: ganttReadyReducer
});


const UPDATE_OPTIONS = 'UPDATE_OPTIONS';
const SET_CALENDAR = 'SET_CALENDAR';
const GANTT_READY = 'GANTT_READY';
const UPDATE_ALL = 'UPDATE_ALL';

export function ganttReady() {
    return {
        type: GANTT_READY
    };
}

export const OptionsActionCreators = {
    updateOptions: (options) => {
        return {
            type: UPDATE_OPTIONS,
            options
        };
    }
};

export const CalendarActionCreators = {
    setCalendar: (calendar) => {
        return {
            type: SET_CALENDAR,
            calendar
        };
    },
    updateAll: (calendar, options) => {
        return {
            type: UPDATE_ALL,
            calendar, options
        };
    }
};

function optionsReducer(state, action) {
    if (state === undefined) {
        return defaultOptions;
    }

    if (action.type === UPDATE_OPTIONS || action.type === UPDATE_ALL) {
        return {
            ...state,
            ...action.options
        };
    }

    return state;
}

function calendarReducer(state, action) {
    if (state === undefined) {
        return null;
    }

    if (action.type === SET_CALENDAR || action.type === UPDATE_ALL) {
        return action.calendar;
    }

    return state;
}

function ganttReadyReducer(state, action) {
    if (state === undefined) {
        return false;
    }

    if (action.type === GANTT_READY) {
        return true;
    }

    return state;
}
