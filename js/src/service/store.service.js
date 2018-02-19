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

    isGanttReady() {
        return this.store.getState().ganttReady;
    }
}
