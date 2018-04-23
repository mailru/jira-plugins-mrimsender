/* eslint-disable flowtype/require-valid-file-annotation */
export const views = {
    basic: {
        key: 'basic',
        name: 'Диаграмма',
        panels: {
            grid: true,
            gantt: true,
            resource: false
        }
    },
    ganttOnly: {
        key: 'ganttOnly',
        name: 'Диаграмма без списка',
        panels: {
            grid: false,
            gantt: true,
            resource: false
        }
    },
    withResources: {
        key: 'withResources',
        name: 'Диаграмма с ресурсами',
        panels: {
            grid: true,
            gantt: true,
            resource: true
        }
    }
};

export const viewKeys = Object.keys(views);

export const viewItems = viewKeys.map(key => views[key]);
