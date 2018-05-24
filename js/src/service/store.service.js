//@flow
import {createStore, combineReducers, applyMiddleware, compose} from 'redux';
import {connectRoutes} from 'redux-first-router';
import createHistory from 'history/createBrowserHistory'
import queryString from 'query-string';

import {calendarReducer, isLoadingReducer, sprintsReducer, optionsReducer, calendarRouteThunk} from './gantt.reducer';
import {preferenceService} from './services';
import {defaultOptions} from '../app-gantt/staticOptions';
import {getBaseUrl} from '../common/ajs-helpers';

export class StoreService {
    store: * = null;

    constructor() {
        const history = createHistory();
        const routesMap = {
            CALENDAR_ROUTE: {
                path: `${getBaseUrl()}/secure/MailRuGanttDiagram.jspa`,
                thunk: calendarRouteThunk
            }
        };

        const {reducer: routerReducer, middleware: routerMiddleware, enhancer: routerEnhancer} = connectRoutes(
            history, routesMap, { querySerializer: queryString }
        );

        this.store = createStore(
            combineReducers({
                options: optionsReducer,
                calendar: calendarReducer,
                sprints: sprintsReducer,
                isLoading: isLoadingReducer,
                location: routerReducer
            }),
            {
                options: {
                    ...defaultOptions,
                    ...preferenceService.getOptions()
                }
            },
            compose(
                routerEnhancer,
                applyMiddleware(routerMiddleware)
            )
        );
    }

    getOptions() {
        return this.store.getState().options;
    }

    getCalendar() {
        return this.store.getState().calendar;
    }

    getSprints() {
        return this.store.getState().sprints;
    }

    isGanttReady() {
        return this.store.getState().ganttReady;
    }

    dispatch(event: *) {
        return this.store.dispatch(event);
    }
}
