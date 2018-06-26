//@flow
// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';

import {buildJiraFieldColumn, ganttColumns, defaultColumns} from './columns';
import type {DhtmlxGantt, GanttTask} from './types';
import type {ColumnParams} from './columns';

export const DEFAULT_MIN_COLUMN_WIDTH = 70;

export function buildColumns(names: $ReadOnlyArray<ColumnParams>) {
    const lastId = names.length - 1;

    return names
        .map((column, i) => {
            if (column.key) {
                const builtInColumn = ganttColumns[column.key];

                if (builtInColumn) {
                    return {...builtInColumn};
                }
            }

            if (column.isJiraField) {
                return buildJiraFieldColumn(column, i !== lastId);
            }

            console.warn('unknown column', column);
            return null;
        })
        .filter(Boolean);
}

export function calculateDuration(startWorkingDay: Date, endWorkingDay: Date, taskStart: Date, taskEnd: Date) {
    const start = Math.min(Math.max(startWorkingDay.getTime(), taskStart.getTime()), endWorkingDay.getTime());
    const end = Math.max(Math.min(endWorkingDay.getTime(), taskEnd.getTime()), startWorkingDay.getTime());
    return moment.duration(end - start).asMinutes();
}

export function defaultTaskCell(gantt: DhtmlxGantt) {
    return (_task: GanttTask, date: Date) => {
        if (!gantt.isWorkTime(date, 'day')) {
            return 'gantt-diagram-weekend-day';
        }
        return '';
    };
}

export function hoursTaskCell(gantt: DhtmlxGantt) {
    return (_item: GanttTask, date: Date) => {
        if (!gantt.isWorkTime(date, 'hour')) {
            return 'gantt-diagram-weekend-day';
        }
        return '';
    };
}


export function configure(gantt: DhtmlxGantt) {
    // $FlowFixMe
    gantt.$data.tasksStore._getIndexById = gantt.$data.tasksStore.getIndexById; //eslint-disable-line no-param-reassign
    // $FlowFixMe
    gantt.$data.tasksStore.getIndexById = function(id) { //eslint-disable-line no-param-reassign
        const task = this.getItem(id);

        if (task.type === 'milestone' && task.parent && !this.getItem(task.parent).$open) {
            return this._getIndexById(task.parent);
        }

        return this._getIndexById(id);
    };

    const config = {
        showGrid: true,
        work_time: true,
        //skip_off_time: true,
        fit_tasks: true,
        details_on_dblclick: false,
        show_progress: false,
        smart_rendering: false,
        smart_scales: true,
        open_tree_initially: true,
        static_background: false,
        show_unscheduled: true,
        auto_scheduling: true,
        auto_scheduling_strict: true,
        auto_scheduling_initial: false,
        keyboard_navigation: true,
        show_task_cells: true,
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

        types: {
            ...gantt.config.types,
            sprint: 'sprint',
            milestone: 'milestone'
        },
        type_renderers: {
            ...gantt.config.type_renderers,
            sprint: (task) => {
                const el = document.createElement("div");
                el.setAttribute(gantt.config.task_attribute, task.id.toString());
                const size = gantt.getTaskPosition(task);
                el.innerHTML = [
                    "<div class='project-left'></div>",
                    "<div class='project-right'></div>"
                ].join('');
                el.className = "custom-project";

                el.style.left = `${size.left}px`;
                el.style.top = `${size.top + 7}px`;
                el.style.width = `${size.width}px`;

                return el;
            },
            milestone: (task) => {
                const size = gantt.getTaskPosition(task);
                return createMilestone(size.top + 7, size.left - 10, task.id.toString());
            }
        },

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

    function createLinkControlEl(side, date) {
        const el = document.createElement('div');
        el.className = `gantt_link_control task_${side} task_${date}_date`;
        el.style.height = '20px';
        el.style.lineHeight = '20px';

        const point = document.createElement('div');
        point.className = 'gantt_link_point';

        el.appendChild(point);

        return el;
    }

    function createMilestone(top, left, taskId) {
        const el = createBox({
            height: 20,
            width: 20,
            top, left
        }, 'gantt_task_line gantt_event_object no_move gantt_milestone gantt-with-tooltip');
        el.setAttribute("task_id", taskId);

        const content = document.createElement('div');
        content.className = 'gantt_task_content';

        el.appendChild(content);
        el.appendChild(createLinkControlEl('left', 'start'));
        el.appendChild(createLinkControlEl('right', 'end'));

        return el;
    }

    // eslint-disable-next-line no-unused-expressions
    gantt.addTaskLayer((task) => {
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
        } else if (task.type === 'issue' && !task.$open && gantt.hasChild(task.id) && !task.unscheduled) {
            const el = document.createElement('div');
            const sizes = gantt.getTaskPosition(task);

            const subTasks = gantt.getChildren(task.id);

            for (let i = 0; i < subTasks.length; i++) {
                const child = gantt.getTask(subTasks[i]);
                const childSizes = gantt.getTaskPosition(child);

                if (!child.unscheduled) {
                    const milestoneEl = createMilestone(sizes.top + 7, childSizes.left - 10, child.id.toString());
                    milestoneEl.setAttribute('title', child.summary);
                    milestoneEl.classList.add('no_link');
                    el.appendChild(milestoneEl);

                    gantt.refreshTask(child.id);
                }
            }
            return el;
        }
        return false;
    });

    function calculateWorkingHours(tasks, startCell, endCell) {
        let minutes = 0;

        const scale = gantt.config.subscales[0].unit;
        tasks.forEach(task => {
            let taskMinutes = 0;
            if (scale === 'hour' && gantt.isWorkTime(startCell, 'hour')) {
                taskMinutes = calculateDuration(startCell, endCell, task.start_date, task.end_date);
            }
            if (scale === 'day' && gantt.isWorkTime(startCell, 'day')) {
                const workingHours = gantt.getWorkHours(startCell);

                const startWorkingDay = new Date(startCell.getTime());
                const endWorkingDay = new Date(startCell.getTime());
                startWorkingDay.setHours(workingHours[0]);
                endWorkingDay.setHours(workingHours[1]);
                taskMinutes = calculateDuration(startWorkingDay, endWorkingDay, task.start_date, task.end_date);
            }
            if (scale === 'week' || scale === 'month' || scale === 'year') {
                if (task.start_date >= startCell && task.end_date <= endCell) {
                    taskMinutes += task.duration;
                } else {
                    const startWorkingDay = new Date(Math.max(task.start_date.getTime(), startCell.getTime()));
                    const endWorkingDay = new Date(Math.min(task.end_date.getTime(), endCell.getTime()));

                    const startDay = new Date(startWorkingDay.getTime());
                    const endDay = new Date(startWorkingDay.getTime());
                    while (startDay.getTime() <= endWorkingDay.getTime()) {
                        if (gantt.isWorkTime(startDay, 'day')) {
                            const workingHours = gantt.getWorkHours(startDay);
                            startDay.setHours(workingHours[0]);
                            endDay.setHours(workingHours[1]);

                            taskMinutes += calculateDuration(startDay, endDay, task.start_date, task.end_date);
                        }
                        startDay.setDate(startDay.getDate() + 1);
                        endDay.setDate(endDay.getDate() + 1);
                    }
                }
            }
            minutes += taskMinutes;
        });
        return Math.round(minutes / 60);
    }

    const templates = {
        grid_file: () => '',
        xml_date: (date) => moment(date).toDate(),
        xml_format: (date) => moment(date).format(),
        task_cell_class: defaultTaskCell(gantt),
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
                const {width, height} = gantt.getTaskPosition(task, gantt.calculateEndDate({
                    start_date: start,
                    duration: plannedDuration
                }), end);
                return `<div class="gantt_overdue_extension gantt_task_line" style="width: ${width}px; height: ${height}px"/>`;
            }
            if (earlyDuration) {
                const {width, height} = gantt.getTaskPosition(task, gantt.calculateEndDate({
                    start_date: start,
                    duration: earlyDuration
                }), end);
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

            if (currentScale !== 'minute') {
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
            }

            return css.join(' ');
        },
        resource_cell_value: (_start, _end, resource, tasks) => {
            return `<div>${calculateWorkingHours(tasks, _start, _end)}</div>`;
        },
    };

    // eslint-disable-next-line no-param-reassign
    gantt.config = {
        ...gantt.config,
        ...config
    };

    // eslint-disable-next-line no-param-reassign
    gantt.templates = {
        ...gantt.templates,
        ...templates
    };
}
