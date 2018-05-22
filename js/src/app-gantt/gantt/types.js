//@flow

export type IdType = string | number;

export type GanttEventType = (
    'onLoadStart' | 'onLoadEnd' | 'onAfterTaskAdd' | 'onAfterTaskUpdate' | 'onAfterTaskDelete' | 'onAfterLinkAdd' |
    'onAfterLinkUpdate' | 'onAfterLinkDelete' | 'onBeforeTaskDrag' | 'onBeforeLinkDelete' | 'onParse' |
    'onBeforeTaskDisplay'
);

export type ShortcutScope = 'gantt' | 'taskRow' | 'taskCell' | 'headerCell';

export type DurationUnit = '';

export type GanttTaskData = {
    start_date: string,
    id: number,
    duration: number,  //maybe optional
    overdueSeconds?: number
}

export type GanttTask = {
    start_date: ?Date,
    end_date: ?Date,
    id: IdType,
    summary: string,
    unscheduled?: boolean,
    duration?: number, //maybe optional
    overdueSeconds?: number,
    resizable?: boolean,
    movable?: boolean,
    parent?: IdType
}

export type GanttLink = {

}

type CalculationConfig = {
    start_date: Date,
    duration: number,
    unit?: DurationUnit,
    task?: GanttTask
}

export type GanttGridColumn = {
    isJiraField?: boolean,
    name: string
};

type GanttConfig = {
    columns: Array<GanttGridColumn>,
    show_task_cells: boolean
};

type GanttTemplates = {
    xml_format(Date): string
};

type GanttMarker = {
    id?: string,
    start_date: Date,
    end_date?: Date,
    css?: string,
    text?: string,
    title?: string
}

export type DhtmlxGantt = {
    config: GanttConfig,
    templates: GanttTemplates,

    load(url: string, type?: 'json' | 'xml' | 'oldxml', callback?: Function): void,
    refreshData(): void,
    clearAll(): void,
    destructor(): void,

    attachEvent(eventType: GanttEventType, handler: Function): string,

    addShortcut(shortcut: string, handler: Function, scope: ShortcutScope): void,
    removeShortcut(shortcut: string, scope: ShortcutScope): void,

    locate(e: Event): IdType,

    addMarker(marker: GanttMarker): string,

    getLink(id: IdType): GanttLink;
    changeLinkId(id: IdType, new_id: IdType): void,
    refreshLink(id: IdType): void,

    close(id: IdType): void,
    open(id: IdType): void,

    getSelectedId(): IdType,
    selectTask(id: IdType): IdType,
    unselectTask(): void,

    addTask(task: GanttTask, parent: ?string, index?: number): void,
    getTask(id: IdType): GanttTask,
    getTaskBy(property: string, value: any): $ReadOnlyArray<GanttTask>,
    getTaskBy((task: GanttTask) => boolean): $ReadOnlyArray<GanttTask>,
    getChildren(id: IdType): $ReadOnlyArray<IdType>,
    getTaskCount(): number,
    refreshTask(id: IdType, refresh_links?: boolean): void,

    calculateEndDate(config: CalculationConfig): Date,
    calculateEndDate(start_date: Date, duration: number): Date,
};
