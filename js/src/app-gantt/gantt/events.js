//@flow
// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';
// eslint-disable-next-line import/no-extraneous-dependencies
import AJS from 'AJS';
// eslint-disable-next-line import/no-extraneous-dependencies
import JIRA from 'JIRA';

import { updateTask } from './util';
import type { DhtmlxGantt } from './types';

import { ganttService, storeService } from '../../service/services';


export function bindEvents(gantt: DhtmlxGantt) {
    const eventListeners = {
        onLoadStart: () => {
            JIRA.Loading.showLoadingIndicator();
            AJS.dim();
        },
        onLoadEnd: () => {
            JIRA.Loading.hideLoadingIndicator();
            AJS.undim();
            // eslint-disable-next-line no-param-reassign
            gantt.config.show_task_cells = gantt.getTaskCount() < 100;
        },
        onAfterTaskAdd: (id, task) => {
            console.log('task add', id, task);
        },
        onAfterTaskUpdate: (id, updatedTask) => {
            ganttService
                .updateTask(
                    storeService.getCalendar().id,
                    id,
                    {
                        start_date: gantt.templates.xml_format(updatedTask.start_date),
                        duration: updatedTask.duration,
                    },
                    {
                        fields: gantt.config.columns.filter(col => col.isJiraField).map(col => col.name)
                    }
                )
                .then(updatedTasks => {
                    for (const newTask of updatedTasks) {
                        const task = gantt.getTask(newTask.id);

                        if (task.type === 'issue') {
                            updateTask(gantt, task, newTask);
                        } else {
                            console.error('not issue task', task.id);
                        }
                    }
                });
        },
        onAfterTaskDelete: (id) => {
            //todo: taskService.delete(id);
            console.log('task delete', id);
        },
        onAfterLinkAdd: (id, {id: _id, ...data}) => {
            ganttService
                .createLink(storeService.getCalendar().id, data)
                .then(link => {
                    gantt.changeLinkId(id, link.id);
                    Object.assign(gantt.getLink(link.id), link);
                    gantt.refreshLink(link.id);
                })
                .catch(e => {
                    console.error('unable to create link', e);
                    gantt.deleteLink(id);
                    if (e.response && e.response.responseText) {
                        gantt.message({type: 'error', text: e.response.responseText});
                    }
                })
        },
        onAfterLinkUpdate: (id, link) => {
            console.log('todo: link update', id, link);
        },
        onAfterLinkDelete: (id, item) => {
            if (item.entityId !== null && item.entityId !== undefined) {
                ganttService.deleteLink(storeService.getCalendar().id, id);
            }
        },
        onBeforeTaskDrag: (id, mode) => {
            const task = gantt.getTask(id);
            if (task.type === 'issue') {
                switch (mode) {
                    case 'resize':
                        return task.resizable;
                    default:
                        return task.movable;
                }
            }
            return false;
        },
        onBeforeLinkDelete: (id) => {
            return id >= 0;
        },
        onParse: () => {
            const {sprint} = storeService.getOptions();

            if (sprint) {
                const sprintObject = storeService
                    .getSprints()
                    .find(it => it.id === sprint);

                if (sprintObject) {
                    const {startDate, endDate} = sprintObject;

                    if (startDate && endDate) {
                        const taskId = gantt.addTask({
                            id: `sprint-${sprintObject.id}`,
                            summary: sprintObject.name,
                            type: 'sprint',
                            linkable: false,
                            $open: true,
                            start_date: moment(startDate).toDate(),
                            end_date: moment(endDate).toDate()
                        });

                        for (const task of gantt.getTaskBy(it => !it.parent)) {
                            if (task.id !== taskId) {
                                task.parent = taskId;
                            }
                        }
                        gantt.refreshData();
                    }
                }
            }
        },
        onBeforeTaskAutoSchedule: (task) => task.type !== 'milestone',
        onBeforeTaskMove: (id, parent) => {
            const task = gantt.getTask(id);
            return task.parent === parent;
        },
        onBeforeRowDragEnd: (id, parent) => {
            const task = gantt.getTask(id);
            const dropTarget = task.$drop_target;
            if(dropTarget) {
                const targetTask = gantt.getTask(dropTarget.substring(dropTarget.indexOf(':') + 1));
                return task.parent === parent && task.type === 'issue' && targetTask.type === 'issue';
            }
            return false;
        },
        onRowDragEnd: (dragId) => {
            const task = gantt.getTask(dragId);
            const dropTarget = task.$drop_target;
            if(!task.entityId)
                return;
            const rankUpdateData:{issues: Array<string>, rankAfterIssue?: string, rankBeforeIssue?: string} = {issues: [task.entityId]};
            if(dropTarget) {
                if (dropTarget.startsWith('next:')) {
                    const afterTask = gantt.getTask(dropTarget.substring(dropTarget.indexOf(':') + 1));
                    rankUpdateData.rankAfterIssue = afterTask.entityId;
                } else {
                    const beforeTask = gantt.getTask(dropTarget);
                    rankUpdateData.rankBeforeIssue = beforeTask.entityId;
                }
                ganttService.updateTaskRank(rankUpdateData);
            }
        }
        /*onGanttScroll: (oldLeft, oldTop, left, top) => {
            //updateRender(oldLeft, oldTop, left, top);
            if (oldTop !== top) {
                if (timeoutId) {
                    clearTimeout(timeoutId);
                }
                const el = document.getElementById('gantt-diagram-calendar');
                el.classList.add('scrolling');
                timeoutId = setTimeout(() => {
                    el.classList.remove('scrolling');
                    gantt._smart_render.updateRender();
                }, scrollTimeout);
            }
        },*/
    };

    for (const key of Object.keys(eventListeners)) {
        gantt.attachEvent(key, eventListeners[key]);
    }
}
