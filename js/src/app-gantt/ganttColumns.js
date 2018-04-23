/* eslint-disable flowtype/require-valid-file-annotation */
import {escapeHtml, getBaseUrl, getContextPath} from '../common/ajs-helpers';


function getIconSrc(src) {
    if (src.startsWith('http')) {
        return src;
    }
    return getContextPath() + src;
}

export function buildJiraFieldColumn({key, name, colParams}, resizable=true) {
    return {
        ...colParams,
        name: key,
        label: name,
        align: 'left',
        isJiraField: true,
        resize: resizable,
        template: (item) => {
            if (item.fields) {
                return item.fields[key] || '';
            }
            return '';
        }
    };
}

export const ganttColumns = {
    id: {
        name: 'id',
        resize: true,
        tree: true,
        width: '110px',
        label: 'Код',
        align: 'left',
        template: (item) => {
            if (item.type === 'group') {
                return '';
            }
            const id = escapeHtml(item.entityId);
            return `<a href="${getBaseUrl()}/browse/${id}" ${item.resolved ? 'style="text-decoration: line-through;"' : ''}>${id}</a>`;
        }
    },
    name: {
        name: 'summary',
        resize: true,
        label: 'Название',
        width: '*',
        align: 'left',
        template: (item) => `<img
                class="calendar-event-issue-type"
                alt="" height="16" width="16" style="margin-right: 5px;"
                src="${item.type === 'group' ? item.icon_src : getIconSrc(item.icon_src)}"/> ${escapeHtml(item.summary)}`
    },
    progress: {
        name: 'progress',
        label: 'Прогресс',
        width: '80px',
        template: (item) => {
            const {progress} = item;
            const overdue = progress > 1;

            return (
                `<div class="progressBar">
                        <div class="progressIndicator ${overdue ? 'overdue' : ''}" style="width: ${(overdue ? 1 : item.progress) * (80 - 12)}px"></div>
                    </div>`
            );
        },
    }
};

export const emptyColumn = {
    name: '___empty',
    resize: false,
    label: '',
    width: '12px',
    template: () => ''
};

export const defaultColumns = Object
    .keys(ganttColumns)
    .map(key => {
        return {
            key
        };
    });
