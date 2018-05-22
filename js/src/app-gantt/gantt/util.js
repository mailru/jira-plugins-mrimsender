//@flow
import type {DhtmlxGantt, IdType} from './types';

export function matchesFilter(gantt: DhtmlxGantt, id: IdType, filter: string) {
    if (gantt.getTask(id).summary.toLocaleLowerCase().includes(filter)) {
        return true;
    }

    return gantt.getChildren(id).some(childId => matchesFilter(gantt, childId, filter));
}
