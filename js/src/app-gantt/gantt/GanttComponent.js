//@flow
import React from 'react';

// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';
// eslint-disable-next-line import/no-extraneous-dependencies
import 'dhtmlx-gantt';

import queryString from 'query-string';


import type {DhtmlxGantt, GanttTask, GanttTaskData} from './types';
import {bindEvents} from './events';
import {setupShortcuts} from './shortcuts';
import type {CurrentCalendarType, OptionsType} from '../types';
import {matchesFilter} from './util';
import {defaultColumns} from '../ganttColumns';
import {OptionsActionCreators} from '../../service/gantt.reducer';
import {getPluginBaseUrl} from '../../common/ajs-helpers';
import {preferenceService, storeService} from '../../service/services';
import {buildColumns} from '../ganttConfig';


const {Gantt} = window;

type Props = {
    ganttReady: boolean,
    calendar: ?CurrentCalendarType,
    options: OptionsType,

}

const urlParams = ['startDate', 'endDate', 'groupBy', 'orderBy', 'order', 'columns', 'sprint', 'withUnscheduled'];

export class GanttComponent extends React.PureComponent<Props> {
    //$FlowFixMe
    ganttElRef = React.createRef();
    gantt: ?DhtmlxGantt = null;

    //attach listeners that depend on props
    _attachListeners(gantt: DhtmlxGantt) {
        gantt.attachEvent(
            'onBeforeTaskDisplay',
            id => (this.filter && this.filter.length) ? matchesFilter(gantt, id, this.props.options.filter || '') : true
        );
    }

    _updateTask = (task: GanttTask, data: GanttTaskData) => {
        const {gantt} = this;
        if (!gantt) {
            return;
        }

        // eslint-disable-next-line camelcase
        const {start_date, id, duration, overdueSeconds, ...etc} = data;
        const start = moment(start_date).toDate();

        Object.assign(
            task,
            {
                ...etc, duration,
                start_date: start,
                end_date: gantt.calculateEndDate(start, duration),
                overdueSeconds: overdueSeconds || undefined
            }
        );
        gantt.refreshTask(task.id);
    };

    componentDidMount() {
        const gantt = Gantt.getGanttInstance();

        this._attachListeners(gantt);
        bindEvents(gantt);
        setupShortcuts(gantt);

        this.gantt = gantt;
    }

    componentWillUnmount() {
        if (this.gantt) {
            this.gantt.destructor();
        }
    }

    componentDidUpdate(prevProps: *) {
        const {gantt} = this;
        if (!gantt) {
            return;
        }

        let refreshData = false;

        const {calendar, options} = this.props;

        if (calendar) {
            const didUrlParamsChange = urlParams.some(param => prevProps.options[param] !== options[param]);
            const didCalendarChange = (prevProps.calendar || {}).id !== calendar.id;
            const didFilersChange = calendar.favouriteQuickFilters !== prevProps.calendar.favouriteQuickFilters;

            if (didUrlParamsChange || didCalendarChange || didFilersChange) {
                gantt.clearAll();
                gantt.addMarker({
                    start_date: new Date(),
                    css: 'today',
                    //text: 'Today'
                });

                gantt.config.columns = buildColumns([...defaultColumns, ...(options.columns || [])]);
                //no need to re-render here, gantt chart will be updated after data is parsed

                const {startDate, endDate, order, groupBy, orderBy, sprint, withUnscheduled} = options;

                const param = queryString.stringify({
                    start: startDate,
                    end: endDate,
                    order: order ? 'ASC' : 'DESC',
                    fields: gantt.config.columns.filter(col => col.isJiraField).map(col => col.name),
                    groupBy, orderBy, sprint, withUnscheduled
                });

                gantt.load(`${getPluginBaseUrl()}/gantt/${calendar.id}?${param}`);

                storeService.dispatch(OptionsActionCreators.updateOptions({ liveData: true }));
                preferenceService.saveOptions(storeService.getOptions());
                preferenceService.put('ru.mail.jira.gantt.lastGantt', calendar.id);
            }
        }

        if (prevProps.options.filter !== options.filter) {
            refreshData = true;
        }

        if (refreshData) {
            gantt.refreshData();
        }
    }

    render() {
        const {ganttReady, calendar, options} = this.props;

        if (!ganttReady || !calendar) {
            return <div/>
        }

        console.log(options);

        return (<div ref={this.ganttElRef}/>)
    }
}