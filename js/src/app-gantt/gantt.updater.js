// eslint-disable-next-line import/no-extraneous-dependencies
import $ from 'jquery';

import {buildColumns, defaultColumns} from './ganttConfig';

import {storeService} from '../service/services';
import {getPluginBaseUrl} from '../common/ajs-helpers';


export class GanttUpdater {
    constructor(gantt, store, loadGantt) {
        this.gantt = gantt;
        this.loadGantt = loadGantt;

        this._update();

        store.subscribe(this._update);
    }

    _update = () => {
        const {startDate, endDate, groupBy, order, orderBy} = storeService.getOptions();
        const calendar = storeService.getCalendar();
        if (storeService.isGanttReady() && calendar) {
            if (
                this.startDate !== startDate ||
                this.endDate !== endDate ||
                this.groupBy !== groupBy ||
                this.orderBy !== orderBy ||
                this.order !== order ||
                calendar.id !== (this.calendar || {}).id
            ) {
                this.startDate = startDate;
                this.endDate = endDate;
                this.groupBy = groupBy;
                this.orderBy = orderBy;
                this.order = order;
                this.calendar = calendar;

                console.log('loading gantt');

                this.gantt.clearAll();
                this.gantt.addMarker({
                    start_date: new Date(),
                    css: 'today',
                    //text: 'Today'
                });

                const isAssignee = groupBy === 'assignee';
                const hasAssigneeColumn = !!this.gantt.config.columns.find(col => col.name === 'assignee');

                if (isAssignee && hasAssigneeColumn) {
                    this.gantt.config.columns = buildColumns(defaultColumns.filter(col => col !== 'assignee'));
                    this.gantt.config.grid_width = 400;
                }

                if (!isAssignee && !hasAssigneeColumn) {
                    this.gantt.config.columns = buildColumns(defaultColumns);
                    this.gantt.config.grid_width = 600;
                }
                //no need to re-render here, gantt chart will be updated after data is parsed

                const param = $.param({
                    start: startDate,
                    end: endDate,
                    order: order ? 'ASC' : 'DESC',
                    groupBy, orderBy
                });

                this.gantt.load(`${getPluginBaseUrl()}/gantt/${this.calendar.id}?${param}`);
            }
        } else {
            console.log('no calendar');
        }
    };
}
