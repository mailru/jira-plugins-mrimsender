//@flow
// eslint-disable-next-line import/no-extraneous-dependencies
import i18n from 'i18n';

import {views} from '../views';
import type {DhtmlxGantt, GanttResource, LayoutRow} from './types';

function resourceConfigFactory(gantt) {
    return ({
        columns: [
            {
                name: 'name',
                label: i18n['ru.mail.jira.plugins.calendar.gantt.columns.name'],
                tree: true,
                template (resource: GanttResource) {
                    return resource.text;
                }
            },
            {
                name: 'workload',
                label: i18n['ru.mail.jira.plugins.calendar.gantt.columns.workload'],
                template (resource: GanttResource) {
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
                        totalDuration += tasks[i].duration;
                    }

                    return `${Math.round(totalDuration / 60)}h`;
                }
            }
        ]
    });
}

export function getRowsForView(gantt: DhtmlxGantt, view: $Keys<views>): $ReadOnlyArray<LayoutRow> {
    if (view === 'ganttOnly') {
        return [
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
    }
    if (view === 'withResources') {
        return [
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
                config: resourceConfigFactory(gantt),
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
    //basic
    return [
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
    ];
}
