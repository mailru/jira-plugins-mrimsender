// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';

import {buildJiraFieldColumn, ganttColumns, defaultColumns} from './ganttColumns';


const gantt = window.gantt;

export const default_min_column_width = 70;

export function buildColumns(names) {
    return names.map(column => {
        const builtInColumn = ganttColumns[column.key];

        if (builtInColumn) {
            return {...builtInColumn};
        }

        if (column.isJiraField) {
            return buildJiraFieldColumn(column);
        }

        console.warn('unknown column', column);
        return null;
    });
}

export const config = {
    min_duration: 24 * 60 * 60 * 1000, // minimum task duration : 1 day

    showGrid: true,
    work_time: true,
    skip_off_time: true,
    fit_tasks: true,
    details_on_dblclick: false,
    show_progress: false,
    smart_rendering: true,
    smart_scales: true,
    open_tree_initially: true,
    //show_task_cells: false,

    layout: {
        css: 'gantt_container',
        rows: [
            /*{
                cols: [
                    {
                        html: '<div>ka</div>',
                        group: 'grids',
                    },
                    {
                        resizer: true,
                        width: 1
                    },
                    {
                        html: '<div>ka</div>',
                    }
                ]
            },*/
            {
                cols: [
                    {
                        view: 'grid',
                        id: 'grid',
                        group: 'grids',
                        scrollX: 'scrollHor',
                        scrollY: 'scrollVer'
                    },
                    {
                        resizer: true,
                        width: 1
                    },
                    {
                        view: 'timeline',
                        id: 'timeline',
                        scrollX: 'scrollHor',
                        scrollY: 'scrollVer'
                    },
                    {
                        view: 'scrollbar',
                        scroll: 'y',
                        id: 'scrollVer',
                        group: 'vertical'
                    }
                ]
            },
            {
                view: 'scrollbar',
                scroll: 'x',
                id: 'scrollHor',
                height: 20
            }
        ]
    },

    columns: buildColumns(defaultColumns),
    task_height: 20,
    row_height: 34,
    grid_width: 600,
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

        if (task.type === 'issue') {
            classes.push('issue_event');
        }

        if (task.type === 'group') {
            classes.push('no_link');
        }

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
