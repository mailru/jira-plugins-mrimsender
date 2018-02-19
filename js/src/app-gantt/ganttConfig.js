// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';

import {getContextPath, escapeHtml, getBaseUrl} from '../common/ajs-helpers';


const gantt = window.gantt;

export const default_min_column_width = 70;

export const config = {
    min_duration: 24 * 60 * 60 * 1000, // minimum task duration : 1 day

    work_time: true,
    skip_off_time: true,
    fit_tasks: true,
    details_on_dblclick: false,
    show_progress: false,

    columns: [
        {
            name: 'id',
            //resize: true,
            label: 'Код',
            align: 'left',
            template: (item) => {
                const id = escapeHtml(item.id);
                return `<a href="${getBaseUrl()}/browse/${id}" ${item.resolved ? 'style="text-decoration: line-through;"' : ''}>${id}</a>`;
            }
        },
        {
            name: 'summary',
            //resize: true,
            label: 'Задача',
            width: '*',
            align: 'left',
            template: (item) => `<img
                class="calendar-event-issue-type"
                alt="" height="16" width="16" style="margin-right: 5px;"
                src="${getContextPath()}${item.icon_src}"/> ${escapeHtml(item.summary)}`
        },
        {
            name: 'progress',
            label: 'Прогресс',
            width: '100px',
            template: (item) => {
                const {progress} = item;
                const overdue = progress > 1;

                return (
                    `<div class="progressBar">
                        <div class="progressIndicator ${overdue ? 'overdue' : ''}" style="width: ${(overdue ? 1 : item.progress) * 88}px"></div>
                    </div>`
                );
            },
        },
        {
            name: 'estimate',
            label: 'Оценка',
            align: 'left',
            template: (item) => `${escapeHtml(item.estimate) || ''}`
        },
        {
            name: 'assignee',
            //resize: true,
            label: 'Исполнитель',
            width: '*',
            align: 'left',
            template: (item) => item.assignee || ''
        },
    ],
    task_height: 20,
    row_height: 34,
    grid_width: 500,
};

export const default_task_cell = (_item, date) => {
    //todo: hide non work time
    if (!gantt.isWorkTime(date)) {
        return 'gantt-diagram-weekend-day';
    }
};

export const hours_task_cell = (_item, date) => {
    if (!gantt.isWorkTime(date) || !gantt.isWorkTime(date, 'hour')) {
        return 'gantt-diagram-weekend-day';
    }
};

export const templates = {
    grid_file: () => '',
    xml_date: (date) => moment(date).toDate(),
    xml_format: (date) => moment(date).format(),
    task_cell_class: default_task_cell,
    task_class: (start, end, task) => {
        const classes = ['gantt_event_object'];

        console.log(task.overdueDate);
        if (task.overdueDate) {
            classes.push('overdue');
        }

        if (!task.resizable) {
            classes.push('no_resize');
        }

        if (!task.movable) {
            classes.push('no_move');
        }

        if (task.resolved) {
            classes.push('resolved');
        }

        return classes.join(' ');
    },
    task_text: () => '',
    leftside_text: (_start, end, task) => {
        const overdueSeconds = task.overdueSeconds;
        if (overdueSeconds) {
            const {width, height} = gantt.getTaskPosition(task, moment(end).subtract(overdueSeconds, 'seconds').toDate(), end);
            return `<div class="gantt_overdue_extension gantt_task_line" style="width: ${width}px; height: ${height}px"/>`;
        }
        const earlySeconds = task.earlySeconds;
        if (earlySeconds) {
            const {width, height} = gantt.getTaskPosition(task, moment(end).subtract(earlySeconds, 'seconds').toDate(), end);
            return `<div class="gantt_early_extension gantt_task_line" style="width: ${width}px; height: ${height}px"/>`;
        }
        return '';
    },
    rightside_text: (_start, _end, task) => {
        if (task.overdueDays && task.overdueDays > 0) {
            return `Overdue: ${task.overdueDays} days`;
        }
        return '';
    }
};
