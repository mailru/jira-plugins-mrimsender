/* eslint-disable flowtype/require-valid-file-annotation */
import React from 'react';
import ReactDOM from 'react-dom';

import { Provider } from 'react-redux';

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
// eslint-disable-next-line import/no-extraneous-dependencies
import 'dhtmlx-gantt/codebase/ext/dhtmlxgantt_auto_scheduling';
// eslint-disable-next-line import/no-extraneous-dependencies
import 'dhtmlx-gantt/codebase/ext/dhtmlxgantt_keyboard_navigation';


import { GanttActions } from './GanttActions';
import { config, templates } from './ganttConfig';
import { eventListeners } from './ganttEvents';
import { ScaleUpdater } from './scale.updater';
import { GanttUpdater } from './gantt.updater';
import { LayoutUpdater } from './layout.updater';

import { collectTopMailCounterScript } from '../common/top-mail-ru';

import {calendarService, ganttService, preferenceService, store, storeService} from '../service/services';
import { CalendarActionCreators, ganttReady } from '../service/gantt.reducer';

import './gantt.less';


const {gantt} = window;

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

gantt.removeShortcut('enter', 'taskRow');
gantt.removeShortcut('ctrl+enter', 'taskRow');
gantt.removeShortcut('ctrl+z', 'taskRow');
gantt.removeShortcut('ctrl+r', 'taskRow');
gantt.removeShortcut('space', 'taskRow');
gantt.removeShortcut('delete', 'taskRow');

gantt.addShortcut(
    'space',
    e => {
        const taskId = gantt.locate(e);

        if (taskId) {
            const task = gantt.getTask(taskId);

            if (task.type === 'group') {
                if (task.$open) {
                    gantt.close(taskId);
                } else {
                    gantt.open(taskId);
                }
            } else if (gantt.getSelectedId() !== taskId) {
                gantt.selectTask(taskId);
            } else {
                gantt.unselectTask(taskId);
            }
        }
    },
    'taskRow'
);

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
                    issue,
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
                                return AJS.InlineDialog.opts.calculatePositions(popup, {target: targetOverride}, mousePosition, opts);
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

AJS.toInit(() => {
    JIRA.Loading.showLoadingIndicator();
    AJS.dim();
    try {
        collectTopMailCounterScript();

        const scaleUpdater = new ScaleUpdater(gantt, store);
        // eslint-disable-next-line no-unused-vars
        const ganttUpdater = new GanttUpdater(gantt, store);
        // eslint-disable-next-line no-unused-vars
        const layoutUpdaater = new LayoutUpdater(gantt, store);

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
            scaleUpdater.updateScales();
            resourcesStore.parse(gantt.serverList('resources'));
        });

        /* Router */
        const ViewRouter = Backbone.Router.extend({
            routes: {
                'calendar=:calendar': 'setCalendar'
            },
            setCalendar(idString) {
                const id = parseInt(idString, 10);
                if (!Number.isNaN(id)) {
                    if (id === -1) {
                        const lastGantt = preferenceService.get('ru.mail.jira.gantt.lastGantt');
                        if (lastGantt) {
                            this.navigate(`calendar=${lastGantt}`, {trigger: true});
                        } else {
                            store.dispatch(CalendarActionCreators.setCalendar(null, []));
                        }
                    } else {
                        JIRA.Loading.showLoadingIndicator();
                        AJS.dim();
                        Promise
                            .all([calendarService.getCalendar(id), ganttService.getCalendarSprints(id), ganttService.getErrors(id)])
                            .then(
                                ([calendar, sprints, errors]) => store.dispatch(CalendarActionCreators.setCalendar(
                                    {...calendar, errors, id}, sprints
                                ))
                            )
                            .finally(() => {
                                JIRA.Loading.hideLoadingIndicator();
                                AJS.undim();
                            });
                    }
                } else {
                    store.dispatch(CalendarActionCreators.setCalendar(null, []));
                }
            }
        });

        // eslint-disable-next-line no-unused-vars
        const viewRouter = new ViewRouter();

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
    } finally {
        JIRA.Loading.hideLoadingIndicator();
        AJS.undim();
    }
});
