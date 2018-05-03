/* eslint-disable flowtype/require-valid-file-annotation */
// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';

import {buildJiraFieldColumn, ganttColumns, defaultColumns} from './ganttColumns';


const { gantt } = window;

export const DEFAULT_MIN_COLUMN_WIDTH = 70;

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

export function calculateDuration(startWorkingDay, endWorkingDay, taskStart, taskEnd) {
    const start = Math.min(Math.max(startWorkingDay.getTime(), taskStart.getTime()), endWorkingDay.getTime());
    const end = Math.max(Math.min(endWorkingDay.getTime(), taskEnd.getTime()), startWorkingDay.getTime());
    return Math.round(moment.duration(end - start).asHours());
}

export function calculateWorkingHours(tasks, startCell, endCell) {
    let hours = 0;

    const scale = gantt.config.subscales[0].unit;
    const workingHours = gantt.getWorkHours(startCell);
    const startWorkingDay = new Date(startCell.getTime());
    let endWorkingDay = new Date(endCell.getTime());
    if (scale !== 'hour') {
        startWorkingDay.setHours(workingHours[0]);
        endWorkingDay = new Date(endCell.getTime() - 1000 * 60 * 60 * 24);
        endWorkingDay.setHours(workingHours[1]);
    }
    tasks.forEach(task => {
        if (gantt.isWorkTime(startCell, scale)) {
            if (scale === 'hour' || scale === 'day') {
                hours += calculateDuration(startWorkingDay, endWorkingDay, task.start_date, task.end_date);
            }
            if (scale === 'week' || scale === 'month' || scale === 'year') {
                if (task.start_date >= startCell && task.end_date <= endCell) {
                    hours += Math.round(task.duration / 60);
                } else {
                    const startCurrentDay = new Date(startWorkingDay);
                    const endCurrentDay = new Date(startWorkingDay.getTime() + 1000 * 60 * 60 * 8);
                    while (startCurrentDay.getDate() <= endWorkingDay.getDate()) {
                        if (gantt.isWorkTime(startCurrentDay, 'day')) {
                            hours += calculateDuration(startCurrentDay, endCurrentDay, task.start_date, task.end_date);
                        }
                        startCurrentDay.setDate(startCurrentDay.getDate() + 1);
                        endCurrentDay.setDate(endCurrentDay.getDate() + 1);
                    }
                }
            }
        }
    });
    return hours;
}

export const resourceConfig = {
    columns: [
        {
            name: 'name', label: 'Name', tree: true, template (resource) {
                return resource.text;
            }
        },
        {
            name: 'workload', label: 'Workload', template (resource) {
                let tasks;
                const store = gantt.getDatastore(gantt.config.resource_store);
                const field = gantt.config.resource_property;

                if (store.hasChild(resource.id)){
                    tasks = gantt.getTaskBy(field, store.getChildren(resource.id));
                } else {
                    tasks = gantt.getTaskBy(field, resource.id);
                }

                let totalDuration = 0;
                for (let i = 0; i < tasks.length; i++) {
                    totalDuration += Math.round(tasks[i].duration / 60);
                }

                return `${totalDuration}h`;
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

    // types: {
    //     ...gantt.config.types,
    //     sprint: 'sprint'
    // },
    // type_renderers: {
    //     ...gantt.config.type_renderers,
    //     sprint: (task) => {
    //         const el = document.createElement("div");
    //         el.setAttribute(gantt.config.task_attribute, task.id);
    //         const size = gantt.getTaskPosition(task);
    //         el.innerHTML = [
    //             "<div class='project-left'></div>",
    //             "<div class='project-right'></div>"
    //         ].join('');
    //         el.className = "custom-project";
    //
    //         el.style.left = `${size.left}px`;
    //         el.style.top = `${size.top + 7}px`;
    //         el.style.width = `${size.width}px`;
    //
    //         return el;
    //     }
    // },

    columns: buildColumns(defaultColumns),
    task_height: 20,
    row_height: 34,
    grid_width: 600,
};

function createBox(sizes, className) {
    const box = document.createElement('div');
    box.style.cssText = [
        `height: ${sizes.height}px`,
        `line-height: ${sizes.height}px`,
        `width: ${sizes.width}px`,
        `top: ${sizes.top}px`,
        `left: ${sizes.left}px`,
        'position: absolute'
    ].join(';');
    box.className = className;
    return box;
}

// eslint-disable-next-line no-unused-expressions
gantt && gantt.addTaskLayer((task) => {
    if (task.type === 'group' && !task.$open && gantt.hasChild(task.id) && !task.unscheduled) {
        const el = document.createElement('div');
        const sizes = gantt.getTaskPosition(task);

        const subTasks = gantt.getChildren(task.id);

        for (let i = 0; i < subTasks.length; i++) {
            const child = gantt.getTask(subTasks[i]);
            const childSizes = gantt.getTaskPosition(child);

            if (!child.unscheduled) {
                const childEl = createBox({
                    height: 20,
                    top: sizes.top,
                    left: childSizes.left + 2,
                    width: childSizes.width - 4,
                }, 'child_preview gantt_task_line gantt_event_object');
                el.appendChild(childEl);
            }
        }
        return el;
    }
    return false;
});

export const defaultTaskCell = (_task, date) => {
    if (!gantt.isWorkTime(date, 'day')) {
        return 'gantt-diagram-weekend-day';
    }
    return '';
};

export const hoursTaskCell = (_item, date) => {
    if (!gantt.isWorkTime(date, 'hour')) {
        return 'gantt-diagram-weekend-day';
    }
    return '';
};

export const templates = {
    grid_file: () => '',
    xml_date: (date) => moment(date).toDate(),
    xml_format: (date) => moment(date).format(),
    task_cell_class: defaultTaskCell,
    task_class: (_start, _end, task) => {
        const classes = ['gantt_event_object'];

        if (task.type === 'issue') {
            classes.push('issue_event');
        }

        if (task.type === 'group' || task.type === 'sprint' || !task.linkable) {
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

        if (task.type === 'group' && gantt.hasChild(task.id)) {
            classes.push('parent');
        }

        if (task.type === 'group' && !task.$open && gantt.hasChild(task.id)) {
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
        const css = [];
        if (gantt.getScale().col_width < 28) {
            css.push('resource_marker_min');
        } else {
            css.push('resource_marker');
        }

        const currentScale = gantt.config.subscales[0].unit;
        const workingHours = {
            'hour': 1,
            'day': 8,
            'week': 40,
            'month': 160,
            'year': 1920
        };

        if (calculateWorkingHours(tasks, _start, _end) <= (workingHours[currentScale] || 8)) {
            css.push('workday_ok');
        } else {
            css.push('workday_over');
        }
        return css.join(' ');
    },
    resource_cell_value: (_start, _end, resource, tasks) => {
        return `<div>${calculateWorkingHours(tasks, _start, _end)}</div>`;
    },
};
