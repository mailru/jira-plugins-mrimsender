// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';
// eslint-disable-next-line import/no-extraneous-dependencies
import AJS from 'AJS';
import debounce from 'lodash.debounce';

import {ganttService, storeService} from '../service/services';


const gantt = window.gantt;

const scrollTimeout = 100;

//detach default smart render scroll event handler
//gantt.detachEvent('ev_onganttscroll:0');

const updateRender = debounce(
    (oldLeft, oldTop, left, top) => {
        console.log('executing debounced update render');
        if (gantt.config.smart_rendering) {
            if((oldTop !== top) || (oldLeft !== left)){
                gantt._smart_render.updateRender();
            }
        }
    },
    50,
    {
        maxWait: 200
    }
);

let timeoutId = null;

export const eventListeners = {
    onLoadStart: () => {
        AJS.dim();
    },
    onLoadEnd: () => {
        AJS.undim();
    },
    onAfterTaskAdd: (id, task) => {
        console.log('task add', id, task);
    },
    onAfterTaskUpdate: (id, task) => {
        ganttService
            .updateTask(
                storeService.getCalendar().id, id,
                {
                    start_date: gantt.templates.xml_format(task.start_date),
                    end_date: gantt.templates.xml_format(task.end_date)
                })
            .then(updatedTasks => {
                for (const newTask of updatedTasks) {
                    const {start_date, end_date, id, ...etc} = newTask;
                    Object.assign(
                        gantt.getTask(id),
                        {
                            ...etc,
                            start_date: moment(start_date).toDate(),
                            end_date: moment(end_date).toDate()
                        }
                    );
                    gantt.refreshTask(id);
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
    onBeforeTaskDrag: (id) => {
        return gantt.getTask(id).movable;
    },
    onBeforeLinkDelete: (id) => {
        return id >= 0;
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
