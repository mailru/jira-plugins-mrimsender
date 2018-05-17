/* eslint-disable flowtype/require-valid-file-annotation */
import {combineReducers} from 'redux';

// eslint-disable-next-line import/no-extraneous-dependencies
import {defaultOptions} from '../app-gantt/staticOptions';


export const ganttReducer = combineReducers({
    options: optionsReducer,
    calendar: calendarReducer,
    sprints: sprintsReducer,
    ganttReady: ganttReadyReducer,
});


const UPDATE_OPTIONS = 'UPDATE_OPTIONS';
const SET_CALENDAR = 'SET_CALENDAR';
const GANTT_READY = 'GANTT_READY';
const UPDATE_ALL = 'UPDATE_ALL';
const SELECT_FILTER = 'SELECT_FILTER';

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
    setCalendar: (calendar, sprints) => {
        return {
            type: SET_CALENDAR,
            calendar, sprints
        };
    },
    updateAll: (calendar, options) => {
        return {
            type: UPDATE_ALL,
            calendar, options
        };
    },
    selectFilter: (id, selected) => {
        return {
            type: SELECT_FILTER,
            id, selected
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

    if (action.type === SET_CALENDAR) {
        return {
            ...state,
            sprint: undefined
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

    if (action.type === SELECT_FILTER) {
        return {
            ...state,
            favouriteQuickFilters: state
                .favouriteQuickFilters
                .map(filter => {
                    if (filter.id === action.id) {
                        return {
                            ...filter,
                            selected: action.selected
                        };
                    }
                    return filter;
                })
        }
    }

    return state;
}

function sprintsReducer(state, action) {
    if (state === undefined) {
        return [];
    }

    if (action.type === SET_CALENDAR) {
        return action.sprints;
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
