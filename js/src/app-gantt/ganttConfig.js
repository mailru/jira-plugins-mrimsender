// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';

import {buildJiraFieldColumn, ganttColumns, defaultColumns} from './ganttColumns';


const gantt = window.gantt;

export const default_min_column_width = 70;

export function buildColumns(names) {
    const lastId = names.length - 1;

    return names.map((column, i) => {
        const builtInColumn = ganttColumns[column.key];

        if (builtInColumn) {
            return {...builtInColumn};
        }

        if (column.isJiraField) {
            return buildJiraFieldColumn(column, i !== lastId);
        }

        console.warn('unknown column', column);
        return null;
    });
}

export function calculateWorkingHours(task, current) {
    if (!gantt.isWorkTime(current)) {
        return 0;
    }
    let workingHours = gantt.getWorkHours(current);
    let currentDate = new Date(current.getFullYear(), current.getMonth(), current.getDate());
    let start = task.start_date;
    let startDate = new Date(start.getFullYear(), start.getMonth(), start.getDate());
    let end = task.end_date;
    let endDate = new Date(end.getFullYear(), end.getMonth(), end.getDate());
    if (currentDate.getTime() > startDate.getTime() && currentDate.getTime() < endDate.getTime()) {
        return 8;
    }
    if (currentDate.getTime() === startDate.getTime() ) {
        if (start.getHours() > workingHours[1]) {
            return 0;
        }
        if (start.getHours() < workingHours[0]) {
            start.setHours(workingHours[0]);
            start.setMinutes(0);
            start.setSeconds(0);
        }
    } else {
        start = new Date(current);
        start.setHours(workingHours[0]);
        start.setMinutes(0);
        start.setSeconds(0);
    }
    if (currentDate.getTime() === endDate.getTime()) {
        if (end.getHours() < workingHours[0]) {
            return 0;
        }
        if (end.getHours() >= workingHours[1]) {
            end.setHours(workingHours[1]);
            end.setMinutes(0);
            end.setSeconds(0);
        }
    } else {
        end = new Date(current);
        end.setHours(workingHours[1]);
        end.setMinutes(0);
        end.setSeconds(0);
    }
    return moment.duration(end.getTime() - start.getTime()).asHours();
}

export const resourceConfig = {
    columns: [
        {
            name: 'name', label: 'Name', tree:true, template: function (resource) {
                return resource.text;
            }
        },
        {
            name: 'workload', label: 'Workload', template: function (resource) {
                let tasks;
                let store = gantt.getDatastore(gantt.config.resource_store),
                    field = gantt.config.resource_property;

                if (store.hasChild(resource.id)){
                    tasks = gantt.getTaskBy(field, store.getChildren(resource.id));
                } else {
                    tasks = gantt.getTaskBy(field, resource.id);
                }

                let totalDuration = 0;
                for (let i = 0; i < tasks.length; i++) {
                    let task = tasks[i];
                    let start = task.start_date;
                    let end = task.end_date;
                    let taskDuration = 0;
                    if (end.getTime() >= start.getTime()) {
                        let currentDate = new Date(start.getFullYear(), start.getMonth(), start.getDate());
                        while (currentDate.getTime() <= end.getTime()) {
                            taskDuration += calculateWorkingHours(task, currentDate);
                            currentDate.setDate(currentDate.getDate() + 1);
                        }
                    }
                    totalDuration += Math.round(taskDuration);
                }

                return totalDuration + 'h';
            }
        }
    ]
};

export const config = {
    showGrid: true,
    work_time: true,
    //skip_off_time: true,
    fit_tasks: true,
    details_on_dblclick: false,
    show_progress: false,
    smart_rendering: true,
    smart_scales: true,
    open_tree_initially: true,
    static_background: false,
    show_unscheduled: true,
    auto_scheduling: true,
    auto_scheduling_strict: true,
    auto_scheduling_initial: false,
    keyboard_navigation: true,
    //show_task_cells: false,
    resource_store: 'resources',
    resource_property: 'resource',

    duration_unit: 'minute',
    duration_step: 1,
    min_duration: 30 * 60 * 1000, // minimum task duration : 30 min

    layout: {
        css: 'gantt_container',
        rows: [
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

function createBox(sizes, class_name) {
    const box = document.createElement('div');
    box.style.cssText = [
        'height:' + sizes.height + 'px',
        'line-height:' + sizes.height + 'px',
        'width:' + sizes.width + 'px',
        'top:' + sizes.top + 'px',
        'left:' + sizes.left + 'px',
        'position:absolute'
    ].join(';');
    box.className = class_name;
    return box;
}

gantt && gantt.addTaskLayer((task) => {
    if (!task.$open && gantt.hasChild(task.id) && !task.unscheduled) {
        const el = document.createElement('div'),
            sizes = gantt.getTaskPosition(task);

        const subTasks = gantt.getChildren(task.id);

        console.log(subTasks);

        for (let i = 0; i < subTasks.length; i++) {
            const child = gantt.getTask(subTasks[i]);
            const child_sizes = gantt.getTaskPosition(child);

            if (child.unscheduled) {
                continue;
            }

            const child_el = createBox({
                height: 20,
                top: sizes.top,
                left: child_sizes.left+2,
                width: child_sizes.width-4,
            }, 'child_preview gantt_task_line gantt_event_object');
            el.appendChild(child_el);
        }
        return el;
    }
    return false;
});

export const default_task_cell = (_task, date) => {
    if (!gantt.isWorkTime(date, 'day')) {
        return 'gantt-diagram-weekend-day';
    }
};

export const hours_task_cell = (_item, date) => {
    if (!gantt.isWorkTime(date, 'hour')) {
        return 'gantt-diagram-weekend-day';
    }
};

export const templates = {
    grid_file: () => '',
    xml_date: (date) => moment(date).toDate(),
    xml_format: (date) => moment(date).format(),
    task_cell_class: default_task_cell,
    task_class: (_start, _end, task) => {
        const classes = ['gantt_event_object'];

        if (task.type === 'issue') {
            classes.push('issue_event');
        }

        if (task.type === 'group' || !task.linkable) {
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

        if (gantt.hasChild(task.id)) {
            classes.push('parent');
        }

        if (!task.$open && gantt.hasChild(task.id)) {
            classes.push('collapsed');
        }

        return classes.join(' ');
    },
    grid_row_class: (_start, _end, task) => {
        const classes = [];

        if (task.unscheduled) {
            classes.push('unscheduled');
        }

        return classes.join(' ');
    },
    task_text: () => '',
    leftside_text: (start, end, task) => {
        const {overdueSeconds, plannedDuration, earlyDuration} = task;
        if (overdueSeconds) {
            const {width, height} = gantt.getTaskPosition(task, gantt.calculateEndDate({ start_date: start, duration: plannedDuration }), end);
            return `<div class="gantt_overdue_extension gantt_task_line" style="width: ${width}px; height: ${height}px"/>`;
        }
        if (earlyDuration) {
            const {width, height} = gantt.getTaskPosition(task, gantt.calculateEndDate({ start_date: start, duration: earlyDuration }), end);
            return `<div class="gantt_early_extension gantt_task_line" style="width: ${width}px; height: ${height}px"/>`;
        }
        return '';
    },
    rightside_text: (_start, _end, task) => {
        if (task.overdueDays && task.overdueDays > 0) {
            return `Overdue: ${task.overdueDays} days`;
        }
        return '';
    },
    resource_cell_class: (_start, _end, resource, tasks) => {
        let css = [];
        if (gantt.getScale().col_width < 28) {
            css.push('resource_marker_min');
        } else {
            css.push('resource_marker');
        }

        let workingHours = 0;
        tasks.forEach(task => {
            workingHours += Math.round(calculateWorkingHours(task, _start));
        });
        if (workingHours <= 8) {
            css.push('workday_ok');
        } else {
            css.push('workday_over');
        }
        return css.join(' ');
    },
    resource_cell_value: (_start, _end, resource, tasks) => {
        let workingHours = 0;
        tasks.forEach(task => {
            workingHours += Math.round(calculateWorkingHours(task, _start));
        });
        return '<div>' + workingHours + '</div>';
    },
};
