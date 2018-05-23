//@flow
// eslint-disable-next-line import/no-extraneous-dependencies
import AJS from 'AJS';
// eslint-disable-next-line import/no-extraneous-dependencies
import $ from 'jquery';
// eslint-disable-next-line import/no-extraneous-dependencies
import JIRA from 'JIRA';


import type {DhtmlxGantt} from './types';
import {calendarService, storeService} from '../../service/services';

export function attachPopover(gantt: DhtmlxGantt) {
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

        const task = gantt.getTask(eventId);

        if (task.type !== 'issue') {
            content.html('');
            showPopup();
            eventDialog.hide();
            return;
        }

        content.html('<span class="aui-icon aui-icon-wait">Loading...</span>');
        showPopup();

        calendarService
            .getEventInfo(storeService.getCalendar().id, task.entityId)
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

                    if (!document.contains(firstTarget)) {
                        const taskId = firstTarget.getAttribute('task_id');

                        if (taskId) {
                            const targetOverride = $(document.querySelector(`.gantt_event_object[task_id="${taskId}"]`) || []);

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