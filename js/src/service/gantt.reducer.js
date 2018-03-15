import {combineReducers} from 'redux';

import {keyedConfigs} from '../app-gantt/scaleConfigs';
import {views} from '../app-gantt/views';


export const ganttReducer = combineReducers({
    options: optionsReducer,
    calendar: calendarReducer,
    ganttReady: ganttReadyReducer
});


const UPDATE_OPTIONS = 'UPDATE_OPTIONS';
const SET_CALENDAR = 'SET_CALENDAR';
const GANTT_READY = 'GANTT_READY';

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
    }
};

function optionsReducer(state, action) {
    if (state === undefined) {
        return {
            scale: keyedConfigs[1].i,
            startDate: '',
            endDate: '',
            groupBy: null,
            orderBy: null,
            order: true,
            view: views.basic.key,
            columns: [
                {
                    key: 'timeoriginalestimate',
                    name: 'Оценка',
                    isJiraField: true,
                    colParams: {
                        width: '53px'
                    }
                },
                {
                    key: 'assignee',
                    name: 'Исполнитель',
                    isJiraField: true,
                    colParams: {
                        width: '200px'
                    }
                }
            ]
        };
    }

    if (action.type === UPDATE_OPTIONS) {
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

    if (action.type === SET_CALENDAR) {
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
