//@flow
// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';

import type {DhtmlxGantt, GanttIssueTask, GanttTaskData, IdType} from './types';


export function matchesFilter(gantt: DhtmlxGantt, id: IdType, filter: string) {
    if (gantt.getTask(id).summary.toLocaleLowerCase().includes(filter)) {
        return true;
    }

    return gantt.getChildren(id).some(childId => matchesFilter(gantt, childId, filter));
}

export function updateTask(gantt: DhtmlxGantt, task: GanttIssueTask, data: GanttTaskData) {
    // eslint-disable-next-line camelcase
    const {start_date, id, duration, overdueSeconds, ...etc} = data;
    const start = moment(start_date).toDate();

    //$FlowFixMe
    Object.assign(
        task,
        {
            ...etc, duration,
            start_date: start,
            end_date: gantt.calculateEndDate(start, duration),
            overdueSeconds: overdueSeconds || undefined
        }
    );
    gantt.refreshTask(task.id);
}
