//@flow

export type IdType = string | number;

export type GanttEventType = (
    'onLoadStart' | 'onLoadEnd' | 'onAfterTaskAdd' | 'onAfterTaskUpdate' | 'onAfterTaskDelete' | 'onAfterLinkAdd' |
    'onAfterLinkUpdate' | 'onAfterLinkDelete' | 'onBeforeTaskDrag' | 'onBeforeLinkDelete' | 'onParse' |
    'onBeforeTaskDisplay' | 'onBeforeParse' | 'onTaskDblClick' | 'onBeforeTaskAutoSchedule' |
    'onBeforeTaskMove' | 'onBeforeRowDragEnd' | 'onRowDragEnd' | 'onGanttReady'
);

export type ShortcutScope = 'gantt' | 'taskRow' | 'taskCell' | 'headerCell';

export type DurationUnit = 'minute' | 'hour' | 'day' | 'week' | 'month' | 'year';

export type GanttTaskData = {
    entityId: string,
    start_date: string,
    id: number,
    duration: number,  //maybe optional
    overdueSeconds?: number
}

export type GanttTaskInternalState = {
    $open?: boolean,
    $drop_target?: string
}

type GanttGenericTask = GanttTaskInternalState & {
    start_date?: ?Date,
    end_date?: ?Date,
    duration?: ?number,
    id: IdType,
    entityId?: string,
    summary: string,
    unscheduled?: ?boolean,
    parent?: ?IdType,
    icon_src?: ?string
}

export type GanttIssueTask = GanttGenericTask & {
    type: 'issue',
    progress?: number,
    overdueSeconds?: number,
    resizable?: boolean,
    movable?: boolean,
    resolved?: boolean,
}

type GanttGroupTask = GanttGenericTask & {
    type: 'group'
}

type GanttSprintTask = GanttGenericTask & {
    type: 'sprint'
}

export type GanttTask = GanttIssueTask | GanttGroupTask | GanttSprintTask

export type GanttLink = {}

type CalculationConfig = {
    start_date: Date,
    duration: number,
    unit?: DurationUnit,
    task?: GanttTask
}

export type GanttGridColumn = {
    isJiraField?: boolean,
    name: string,
    width?: string
};

export type SubscaleConfig = {
    css?: (Date) => string,
    format?: string,
    step?: number,
    template?: (Date) => string,
    unit: DurationUnit
};

type TypeRenderer = (task: GanttTask, defaultRenderer: TypeRenderer) => HTMLElement;

type LayoutView = 'grid' | 'timeline' | 'resizer' | 'resourceGrid' | 'resourceTimeline';

type LayoutScrollBar = {
    id: string,
    view: 'scrollbar',
    scroll: 'x' | 'y'
}

type LayoutResizer = {
    resizer: true,
    width: number
}

type LayoutColumn = LayoutScrollBar | LayoutResizer | {
    view: LayoutView,
    scrollY?: string,
}

export type LayoutRow = LayoutScrollBar | LayoutResizer | {
    view: LayoutView,
    id: string
} | {
    cols: $ReadOnlyArray<LayoutColumn>
}

export type Layout = {
    css: string,
    rows: $ReadOnlyArray<LayoutRow>
}

type GanttConfig = {
    layout: Layout,

    min_column_width: number,
    columns: Array<GanttGridColumn>,

    scale_unit: DurationUnit,
    date_scale: string,
    step: number,
    subscales: $ReadOnlyArray<SubscaleConfig>,

    types: {[string]: string},
    type_renderers: {[string]: TypeRenderer},

    start_date: ?Date,
    end_date: ?Date,

    root_id: IdType,

    show_task_cells: boolean,

    resource_property: string,
    resource_store: string,

    task_attribute: string, //??

    order_branch: boolean,
};

type GanttTemplates = {
    xml_format(Date): string,
    task_cell_class: (GanttTask, Date) => string,
    date_scale?: (Date) => string,
};

type GanttMarker = {
    id?: string,
    start_date: Date,
    end_date?: Date,
    css?: string,
    text?: string,
    title?: string
}

export type GanttResource = {
    text: string,
    id: string
}

type DatastoreConfig = {
    name: string,
    initItem?: Function,
    type?: 'treeDatastore'
}

export type ScaleConfig = {
    col_width: number
}

type WorkTimeConfig = ({day: 0 | 1 | 2 | 3 | 4 | 5 | 6} | {date: Date}) & {
    hours: [number, number] | boolean
}

type TaskCallback = (task: GanttTask) => void;

type Position = {
    left: number,
    top: number,
    height: number,
    width: number
}

//todo: update when datastore will be documented
interface DatastoreType {
    parse($ReadOnlyArray<any>): void,
    hasChild(id: string): boolean,
    getChildren(id: string): $ReadOnlyArray<string>,
    getIndexById(id: string): number,
}

type DateUtil = {
    add(date: Date, number: number, unit: DurationUnit): Date,
    date_to_str(format: string, utc?: boolean): (Date) => string,
    day_start(Date): Date,
    week_start(Date): Date,
    month_start(Date): Date,
    year_start(Date): Date
}

export interface DhtmlxGantt {
    config: GanttConfig,
    templates: GanttTemplates,
    date: DateUtil,

    //internal things
    $task: any, //todo
    $data: {
        tasksStore: DatastoreType,
    },

    //initialize & refresh
    load(url: string, type?: 'json' | 'xml' | 'oldxml', callback?: Function): void,
    refreshData(): void,
    clearAll(): void,
    destructor(): void,
    render(): void,
    init(container: string | HTMLElement): void,

    addTaskLayer((task: GanttTask) => (HTMLElement | false)): void,

    //scale
    getScale(): ScaleConfig,

    //datastore
    createDatastore(config: DatastoreConfig): DatastoreType,
    getDatastore(name: string): DatastoreType,
    serverList(name: string, options: any): $ReadOnlyArray<any>,

    //events
    attachEvent(eventType: GanttEventType, handler: Function): string,

    //shortcuts
    addShortcut(shortcut: string, handler: Function, scope: ShortcutScope): void,
    removeShortcut(shortcut: string, scope: ShortcutScope): void,

    //dom related
    locate(e: Event): IdType,
    getTaskPosition(task: GanttTask): Position,
    getTaskPosition(task: GanttTask, from: Date, to: Date): Position,

    //marker
    addMarker(marker: GanttMarker): string,

    //link
    getLink(id: IdType): GanttLink;
    changeLinkId(id: IdType, new_id: IdType): void,
    refreshLink(id: IdType): void,

    //open/close
    close(id: IdType): void,
    open(id: IdType): void,

    //selection
    getSelectedId(): IdType,
    selectTask(id: IdType): IdType,
    unselectTask(): void,

    //task
    addTask(task: GanttTask, parent: ?IdType, index?: number): IdType,

    eachTask(callback: TaskCallback, parent?: IdType, master?: any): void,

    getChildren(id: IdType): $ReadOnlyArray<IdType>,
    hasChild(id: IdType): boolean,

    getTask(id: IdType): GanttTask,
    getTaskBy((task: GanttTask) => boolean): $ReadOnlyArray<GanttTask>,
    getTaskBy(property: string, value: any): $ReadOnlyArray<GanttTask>,
    getTaskCount(): number,

    refreshTask(id: IdType, refresh_links?: boolean): void,

    //calculation
    calculateEndDate(config: CalculationConfig): Date,
    calculateEndDate(start_date: Date, duration: number): Date,
    getSubtaskDates(task_id?: IdType): {start_date: Date, end_date: Date},

    //worktime
    setWorkTime(config: WorkTimeConfig): void,
    isWorkTime(date: Date, unit: DurationUnit): void,
    getWorkHours(date: Date): [number, number]
}
