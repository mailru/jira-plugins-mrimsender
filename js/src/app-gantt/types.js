//@flow
import type{GanttGridColumn} from './gantt/types';
import {views} from './views';


//todo
export type TaskType = any;

//todo
export type GanttType = any;
export type GanttTaskType = any;

//todo: add all relevant fields
export type CalendarType = {
    id: number,
    name: string,
    color: string,
}

export type CurrentCalendarType = CalendarType & {
    errors: $ReadOnlyArray<string>,
    favouriteQuickFilters: $ReadOnlyArray<QuickFilterType>
}

export type VoidCallback = () => void;

export type QuickFilterType = {
    id: number,
    favourite: boolean,
    name: string,
    description: string,
    selected: boolean
};

export type SprintType = {
    id: number,
    boardName: string,
    name: string,
    state: 'FUTURE' | 'ACTIVE' | 'CLOSED',
    startDate: ?string,
    endDate: ?string
};

export type PersistentOptions = {
    startDate: string,
    endDate: string,
    groupBy: ?string,
    order: ?boolean,
    orderBy: ?string,
    columns: $ReadOnlyArray<GanttGridColumn>,
    filter: ?string,
    sprint: ?number,
    withUnscheduled: boolean
}

export type OptionsType = PersistentOptions & {
    liveData: boolean,
    scale: number,
    view: $Keys<views>
};
