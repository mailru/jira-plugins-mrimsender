// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';

import {ganttService, storeService} from '../service/services';


const gantt = window.gantt;

export const eventListeners = {
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
            .then(newTask => {
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
    }
};
