// eslint-disable-next-line import/no-extraneous-dependencies
import $ from 'jquery';

import {buildColumns} from './ganttConfig';
import {defaultColumns} from './ganttColumns';

import {preferenceService, storeService} from '../service/services';
import {getPluginBaseUrl} from '../common/ajs-helpers';
import {OptionsActionCreators} from '../service/gantt.reducer';


function matchesFilter(gantt, id, filter) {
    if (gantt.getTask(id).summary.toLocaleLowerCase().includes(filter)) {
        return true;
    }

    return gantt.getChildren(id).some(childId => matchesFilter(gantt, childId, filter));
}

export class GanttUpdater {
    constructor(gantt, store, loadGantt) {
        this.gantt = gantt;
        this.loadGantt = loadGantt;

        this._attachEventListeners();

        this._update();

        store.subscribe(this._update);
    }

    _attachEventListeners() {
        this.gantt.attachEvent(
            'onBeforeTaskDisplay',
            id => (this.filter && this.filter.length) ? matchesFilter(this.gantt, id, this.filter) : true
        );
    }

    _update = () => {
        const {startDate, endDate, groupBy, order, orderBy, columns, filter} = storeService.getOptions();
        const calendar = storeService.getCalendar();
        if (storeService.isGanttReady() && calendar) {
            if (
                this.startDate !== startDate ||
                this.endDate !== endDate ||
                this.groupBy !== groupBy ||
                this.orderBy !== orderBy ||
                this.order !== order ||
                this.columns !== columns ||
                calendar.id !== (this.calendar || {}).id
            ) {
                this.startDate = startDate;
                this.endDate = endDate;
                this.groupBy = groupBy;
                this.orderBy = orderBy;
                this.order = order;
                this.calendar = calendar;
                this.columns = columns;

                console.log('loading gantt');

                this.gantt.clearAll();
                this.gantt.addMarker({
                    start_date: new Date(),
                    css: 'today',
                    //text: 'Today'
                });

                this.gantt.config.columns = buildColumns([...defaultColumns, ...(columns || [])]);
                //no need to re-render here, gantt chart will be updated after data is parsed

                console.log(this.gantt.config.columns);

                const param = $.param({
                    start: startDate,
                    end: endDate,
                    order: order ? 'ASC' : 'DESC',
                    fields: this.gantt.config.columns.filter(col => col.isJiraField).map(col => col.name),
                    groupBy, orderBy
                });

                this.gantt.load(`${getPluginBaseUrl()}/gantt/${this.calendar.id}?${param}`);

                storeService.dispatch(OptionsActionCreators.updateOptions({ liveData: true }));
                preferenceService.saveOptions(storeService.getOptions());
            }

            if (filter !== this.filter) {
                this.filter = filter;

                this.gantt.refreshData();
            }
        } else {
            console.log('no calendar');
        }
    };
}
