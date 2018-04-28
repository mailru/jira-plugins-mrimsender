/* eslint-disable flowtype/require-valid-file-annotation */
export class StoreService {
    constructor(store) {
        this.store = store;
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

    dispatch(event) {
        return this.store.dispatch(event);
    }
}
