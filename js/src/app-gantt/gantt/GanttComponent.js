//@flow
import React from 'react';

// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';
// eslint-disable-next-line import/no-extraneous-dependencies
import 'dhtmlx-gantt';
// eslint-disable-next-line import/no-extraneous-dependencies
import 'dhtmlx-gantt/codebase/ext/dhtmlxgantt_marker';
// eslint-disable-next-line import/no-extraneous-dependencies
import 'dhtmlx-gantt/codebase/ext/dhtmlxgantt_smart_rendering';
// eslint-disable-next-line import/no-extraneous-dependencies
import 'dhtmlx-gantt/codebase/ext/dhtmlxgantt_auto_scheduling';
// eslint-disable-next-line import/no-extraneous-dependencies
import 'dhtmlx-gantt/codebase/ext/dhtmlxgantt_keyboard_navigation';

import queryString from 'query-string';


import type {DhtmlxGantt} from './types';
import {bindEvents} from './events';
import {setupShortcuts} from './shortcuts';
import type {CurrentCalendarType, OptionsType} from '../types';
import {matchesFilter} from './util';
import {defaultColumns} from './columns';
import {OptionsActionCreators} from '../../service/gantt.reducer';
import {getPluginBaseUrl} from '../../common/ajs-helpers';
import {calendarService, preferenceService, storeService} from '../../service/services';
import {buildColumns, configure} from './config';
import {attachPopover} from './popover';
import {updateScales} from './scales';
import {getRowsForView} from './views';


const {Gantt} = window;

type Props = {
    calendar: ?CurrentCalendarType,
    options: OptionsType,
    onGanttInit?: (gantt: DhtmlxGantt) => void
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

        const resourcesStore = gantt.createDatastore({
            name: gantt.config.resource_store,
            type: 'treeDatastore',
            initItem (item) {
                const newItem = item;
                newItem.parent = item.parent || gantt.config.root_id;
                newItem[gantt.config.resource_property] = item.parent;
                newItem.open = true;
                return newItem;
            }
        });

        gantt.attachEvent('onBeforeParse', () => {
            gantt.serverList('resources');
        });

        gantt.attachEvent('onParse', () => {
            updateScales(gantt, this.props.options.scale);

            resourcesStore.parse(gantt.serverList('resources'));
        });
    }

    componentDidMount() {
        const gantt = Gantt.getGanttInstance();
        this.gantt = gantt;
        console.log('created gantt');

        if (this.props.onGanttInit) {
            this.props.onGanttInit(gantt);
        }

        calendarService
            .getUserPreference()
            .then(preference => {
                //$FlowFixMe no moment-tz flow types
                moment.tz.setDefault(preference.timezone);
                const {workingDays, workingTime} = preference;
                const workingHours = [
                    moment(workingTime.startTime, 'HH:mm').hours(),
                    moment(workingTime.endTime, 'HH:mm').hours()
                ];
                for (let i = 0; i <= 6; i++) {
                    gantt.setWorkTime({day: i, hours: workingDays.includes(i) ? workingHours : false});
                }
                for (const nonWorkingDay of preference.nonWorkingDays) {
                    gantt.setWorkTime({date: new Date(nonWorkingDay), hours: false});
                }

                this._init();
            });
    }

    _init() {
        const {gantt} = this;

        if (!gantt) {
            alert('gantt istance is null');
            return;
        }

        configure(gantt);

        this._attachListeners(gantt);
        bindEvents(gantt);
        setupShortcuts(gantt);
        attachPopover(gantt);

        gantt.init(this.ganttElRef.current);
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
        let render = false;
        let init = false;

        const {calendar, options} = this.props;

        if (calendar) {
            const didUrlParamsChange = urlParams.some(param => prevProps.options[param] !== options[param]);

            const prevCalendar = (prevProps.calendar || {});
            const didCalendarChange = prevCalendar.id !== calendar.id;
            const didFilersChange = calendar.favouriteQuickFilters !== prevCalendar.favouriteQuickFilters;

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

        if (options.scale !== prevProps.options.scale) {
            updateScales(gantt, options.scale);
            render = true;
        }

        if (prevProps.options.filter !== options.filter) {
            refreshData = true;
        }

        if (prevProps.options.view !== options.view) {
            gantt.config.layout.rows = getRowsForView(gantt, options.view);
            init = true;
        }

        if (init) {
            gantt.init(this.ganttElRef.current);
            return;
        }

        if (render) {
            gantt.render();
            return;
        }

        if (refreshData) {
            gantt.refreshData();
        }
    }

    render() {
        return (<div className="gantt-diagram-calendar" ref={this.ganttElRef}/>)
    }
}