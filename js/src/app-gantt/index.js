import React from 'react';
import ReactDOM from 'react-dom';

import {Provider} from 'react-redux';

import LayerManager from '@atlaskit/layer-manager';

// eslint-disable-next-line import/no-extraneous-dependencies
import AJS from 'AJS';
// eslint-disable-next-line import/no-extraneous-dependencies
import $ from 'jquery';
// eslint-disable-next-line import/no-extraneous-dependencies
import _ from 'underscore';
// eslint-disable-next-line import/no-extraneous-dependencies
import Backbone from 'backbone';
// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';
// eslint-disable-next-line import/no-extraneous-dependencies
import JIRA from 'JIRA';

// eslint-disable-next-line import/no-extraneous-dependencies
import 'dhtmlx-gantt';
// eslint-disable-next-line import/no-extraneous-dependencies
import 'dhtmlx-gantt/codebase/ext/dhtmlxgantt_marker';
// eslint-disable-next-line import/no-extraneous-dependencies
import 'dhtmlx-gantt/codebase/ext/dhtmlxgantt_smart_rendering';

import {GanttActions} from './GanttActions';
import {config, templates} from './ganttConfig';
import {eventListeners} from './ganttEvents';
import {ScaleUpdater} from './scale.updater';
import {GanttUpdater} from './gantt.updater';
import {LayoutUpdater} from './layout.updater';

import {collectTopMailCounterScript} from '../common/top-mail-ru';

import {calendarService, ganttService, store, storeService} from '../service/services';
import {CalendarActionCreators, ganttReady} from '../service/gantt.reducer';

import './gantt.less';


const gantt = window.gantt;

gantt.config = {
    ...gantt.config,
    ...config
};

gantt.templates = {
    ...gantt.templates,
    ...templates
};

for (const key of Object.keys(eventListeners)) {
    gantt.attachEvent(key, eventListeners[key]);
}

// todo обработка попадания на выходные конца или начала таска
// gantt.attachEvent('onTaskDrag', function(id, mode, task, original) {
//     var modes = gantt.config.drag_mode;
//     if (mode === modes.move || mode === modes.resize) {
//         var diff = original.duration * gantt.config.min_duration;
//
//         if (!gantt.isWorkTime(task.end_date)) {
//             task.end_date = new Date(task.end_date + diff);
//             return;
//         }
//         if (!gantt.isWorkTime(task.start_date)) {
//             task.start_date = new Date(task.start_date - diff);
//             return;
//         }
//     }
// });

function initGantt() {
    gantt.init('gantt-diagram-calendar');

    const eventDialog = AJS.InlineDialog('.gantt_event_object.issue_event', 'eventDialog', (content, trigger, showPopup) => {
        const eventId = $(trigger).attr('task_id');

        // Atlassian bug workaround
        content.click((e) => e.stopPropagation());

        if (!eventId) {
            content.html('');
            showPopup();
            eventDialog.hide();
            return;
        }

        content.html('<span class="aui-icon aui-icon-wait">Loading...</span>');
        showPopup();

        calendarService
            .getEventInfo(storeService.getCalendar().id, gantt.getTask(eventId).entityId)
            .then(issue => {
                content.html(JIRA.Templates.Plugins.MailRuCalendar.issueInfo({
                    issue: issue,
                    contextPath: AJS.contextPath()
                })).addClass('calendar-event-info-popup');
                eventDialog.refresh();
            });
    }, {
        calculatePositions: (popup, targetPosition, mousePosition, opts) => {
            if (targetPosition) {
                const {target} = targetPosition;

                if (target && target.length) {
                    const firstTarget = target[0];

                    if (!document.body.contains(firstTarget)) {
                        const taskId = firstTarget.getAttribute('task_id');

                        if (taskId) {
                            const targetOverride = document.querySelector(`.gantt_event_object[task_id="${taskId}"]`);

                            if (targetOverride) {
                                targetPosition = {target: targetOverride};
                            }
                        }
                    }
                }
            }

            return AJS.InlineDialog.opts.calculatePositions(popup, targetPosition, mousePosition, opts);
        },
        isRelativeToMouse: true,
        cacheContent: false,
        hideDelay: null,
        onTop: true,
        closeOnTriggerClick: true,
        useLiveEvents: true
    });
}

AJS.toInit(function() {
    try {
        collectTopMailCounterScript();

        const scaleUpdater = new ScaleUpdater(gantt, store);
        new GanttUpdater(gantt, store);
        new LayoutUpdater(gantt, store);

        const resourcesStore = gantt.createDatastore({
            name: gantt.config.resource_store,
            type: 'treeDatastore',
            initItem: function (item) {
                item.parent = item.parent || gantt.config.root_id;
                item[gantt.config.resource_property] = item.parent;
                item.open = true;
                return item;
            }
        });

        gantt.attachEvent('onBeforeParse', () => {
            gantt.serverList('resources');
        });

        gantt.attachEvent('onParse', () => {
            scaleUpdater.updateScales();
            resourcesStore.parse(gantt.serverList('resources'));
        });

        /* Router */
        const ViewRouter = Backbone.Router.extend({
            routes: {
                'calendar=:calendar': 'setCalendar'
            },
            setCalendar: function (id) {
                Promise
                    .all([calendarService.getCalendar(id), ganttService.getCalendarSprints(id)])
                    .then(
                        ([calendar, sprints]) => store.dispatch(CalendarActionCreators.setCalendar({...calendar, id}, sprints))
                    );
            }
        });

        new ViewRouter();

        ReactDOM.render(
            <Provider store={store}>
                <LayerManager>
                    <GanttActions gantt={gantt}/>
                </LayerManager>
            </Provider>,
            document.getElementById('gantt-actions')
        );

        calendarService
            .getUserPreference()
            .then(preference => {
                moment.tz.setDefault(preference.timezone);
                const {workingDays, workingTime} = preference;
                const workingHours = [
                    moment(workingTime.startTime, 'HH:mm').hours(),
                    moment(workingTime.endTime, 'HH:mm').hours()
                ];
                for (let i = 0; i <= 6; i++) {
                    gantt.setWorkTime({day: i, hours: _.contains(workingDays, i) ? workingHours : false});
                }
                for (const nonWorkingDay of preference.nonWorkingDays) {
                    gantt.setWorkTime({date: new Date(nonWorkingDay), hours: false});
                }

                Backbone.history.start();

                initGantt();
                store.dispatch(ganttReady());
            });
    } catch (e) {
        console.error(e);
    }
});
