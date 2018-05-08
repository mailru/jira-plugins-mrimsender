/* eslint-disable flowtype/require-valid-file-annotation */
import {resourceConfig} from './ganttConfig';


export const views = {
    basic: {
        key: 'basic',
        name: 'Диаграмма',
        panels: {
            grid: true,
            gantt: true,
            resource: false
        },
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
    ganttOnly: {
        key: 'ganttOnly',
        name: 'Диаграмма без списка',
        panels: {
            grid: false,
            gantt: true,
            resource: false
        },
        rows: [
            {
                cols: [
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
    withResources: {
        key: 'withResources',
        name: 'Диаграмма с ресурсами',
        panels: {
            grid: true,
            gantt: true,
            resource: true
        },
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
                ],
                gravity: 2
            },
            {
                resizer: true,
                width: 1
            },
            {
                config: resourceConfig,
                cols: [
                    {
                        view: 'resourceGrid',
                        group: 'grids',
                        scrollY: 'resourceScrollVer'
                    },
                    {
                        resizer: true,
                        width: 1
                    },
                    {
                        view: 'resourceTimeline',
                        scrollX: 'scrollHor',
                        scrollY: 'resourceScrollVer'
                    },
                    {
                        view: 'scrollbar',
                        scroll: 'y',
                        id: 'resourceScrollVer',
                        group: 'vertical'
                    }
                ],
                gravity: 1
            },
            {
                view: 'scrollbar',
                scroll: 'x',
                id: 'scrollHor',
                height: 20
            }
        ]
    }
};

export const viewKeys = Object.keys(views);

export const viewItems = viewKeys.map(key => views[key]);
