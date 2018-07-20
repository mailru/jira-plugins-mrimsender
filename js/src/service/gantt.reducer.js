//@flow
import { type Dispatch } from 'redux';
import type { CurrentCalendarType, OptionsType, SprintType } from '../app-gantt/types';
import { defaultOptions } from '../app-gantt/staticOptions';
import { calendarService, ganttService, preferenceService } from './services';


const UPDATE_OPTIONS = 'UPDATE_OPTIONS';
const SET_CALENDAR = 'SET_CALENDAR';
const SELECT_FILTER = 'SELECT_FILTER';
const FETCH_CALENDAR = 'FETCH_CALENDAR';


export const OptionsActionCreators = {
    updateOptions: (options: $Shape<OptionsType>) => {
        return {
            type: UPDATE_OPTIONS,
            options
        };
    }
};

export const CalendarActionCreators = {
    setCalendar: (calendar: ?CurrentCalendarType, sprints: $ReadOnlyArray<SprintType>, sprint: ?number) => {
        return {
            type: SET_CALENDAR,
            calendar, sprints, sprint
        };
    },
    selectFilter: (id: number, selected: boolean) => {
        return {
            type: SELECT_FILTER,
            id, selected
        };
    },
    navigate: (calendarId: number, sprintId?: number) => ({
        type: 'CALENDAR_ROUTE',
        query: {calendarId, sprintId}
    })
};

export function optionsReducer(state: OptionsType, action: *) {
    if (state === undefined) {
        return defaultOptions;
    }

    if (action.type === UPDATE_OPTIONS) {
        const newState = {
            ...state,
            ...action.options
        };
        return newState;
    }

    if (action.type === SET_CALENDAR) {
        return {
            ...state,
            ...preferenceService.getOptions(action.calendar.id) || defaultOptions,
            sprint: action.sprint
        };
    }

    return state;
}

export function calendarReducer(state: ?CurrentCalendarType, action: *) {
    if (state === undefined) {
        return null;
    }

    if (action.type === SET_CALENDAR) {
        return action.calendar;
    }

    if (action.type === SELECT_FILTER && state) {
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

export function sprintsReducer(state: $ReadOnlyArray<SprintType>, action: *) {
    if (state === undefined) {
        return [];
    }

    if (action.type === SET_CALENDAR) {
        return action.sprints;
    }

    return state;
}

export function isLoadingReducer(state: boolean = false, action: *) {
    console.log(action);

    switch (action.type) {
        case FETCH_CALENDAR:
            return true;
        case SET_CALENDAR:
            return false;
        default:
            return state;
    }
}

export const calendarRouteThunk = (dispatch: Dispatch, getState: *) => {
    const {calendarId, sprintId} = (getState().location.query || {});

    const id = calendarId ? parseInt(calendarId, 10) : -1;
    const sprint = parseInt(sprintId, 10) || null;

    let navigateToFirstAvailable = false;

    if (!Number.isNaN(id)) {
        if (getState().calendar && id === getState().calendar.id) {
            dispatch(OptionsActionCreators.updateOptions({ sprint }));
            return;
        }

        if (id === -1) {
            const lastGantt = preferenceService.get('ru.mail.jira.gantt.lastGantt');
            if (lastGantt) {
                dispatch(CalendarActionCreators.navigate(lastGantt))
            } else {
                navigateToFirstAvailable = true;
            }
        } else {
            dispatch({ type: 'FETCH_CALENDAR' });
            Promise
                .all([calendarService.getCalendar(id), ganttService.getCalendarSprints(id), ganttService.getErrors(id)])
                .then(
                    ([calendar, sprints, errors]) => dispatch(CalendarActionCreators.setCalendar(
                        {...calendar, errors, id}, sprints, sprint
                    ))
                )
                .catch(e => {
                    console.error(e);
                    if (e && e.response && e.response.responseText) {
                        alert(e.response.responseText);
                    }
                    dispatch(CalendarActionCreators.setCalendar(null, [], null));
                })
        }
    } else {
        navigateToFirstAvailable = true;
    }

    if (navigateToFirstAvailable) {
        calendarService
            .getUserCalendars()
            .then(calendars => {
                const ganttCalendars = calendars.filter(cal => cal.ganttEnabled);

                if (ganttCalendars.length > 0) {
                    dispatch(CalendarActionCreators.navigate(ganttCalendars[0].id));
                } else {
                    dispatch(CalendarActionCreators.setCalendar(null, [], null));
                }
            })
            .catch(e => {
                console.error(e);
                if (e && e.response && e.response.responseText) {
                    alert(e.response.responseText);
                }
                dispatch(CalendarActionCreators.setCalendar(null, [], null));
            })
    }
};
