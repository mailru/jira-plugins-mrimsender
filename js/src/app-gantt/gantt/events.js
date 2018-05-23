//@flow
// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';
// eslint-disable-next-line import/no-extraneous-dependencies
import AJS from 'AJS';
// eslint-disable-next-line import/no-extraneous-dependencies
import JIRA from 'JIRA';

import {updateTask} from './util';
import type {DhtmlxGantt} from './types';

import {ganttService, storeService} from '../../service/services';


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
        onAfterTaskUpdate: (id, task) => {
            ganttService
                .updateTask(
                    storeService.getCalendar().id,
                    id,
                    {
                        start_date: gantt.templates.xml_format(task.start_date),
                        duration: task.duration,
                    },
                    {
                        fields: gantt.config.columns.filter(col => col.isJiraField).map(col => col.name)
                    }
                )
                .then(updatedTasks => {
                    for (const newTask of updatedTasks) {
                        updateTask(gantt, gantt.getTask(newTask.id), newTask);
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
                });
        },
        onAfterLinkUpdate: (id, link) => {
            console.log('todo: link update', id, link);
        },
        onAfterLinkDelete: (id) => {
            ganttService.deleteLink(storeService.getCalendar().id, id);
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

            console.log(sprint);
            if (sprint) {
                const sprintObject = storeService
                    .getSprints()
                    .find(it => it.id === sprint);

                console.log(sprint, sprintObject);
                if (sprintObject) {
                    const {startDate, endDate} = sprintObject;

                    const taskId = gantt.addTask({
                        id: 'sprint',
                        summary: sprintObject.name,
                        name: sprintObject.name,
                        type: 'sprint',
                        $open: true,
                        unscheduled: !(startDate && endDate),
                        start_date: startDate ? moment(startDate).toDate() : null,
                        end_date: endDate ? moment(endDate).toDate() : null
                    }, null, 0);

                    console.log(taskId);

                    for (const task of gantt.getTaskBy(it => !it.parent)) {
                        if (task.id !== taskId) {
                            //console.log(task.id, taskId);
                            //gantt.setParent(task.id, taskId);
                            task.parent = taskId;
                            gantt.refreshTask(task.id);
                        }
                    }
                }
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
