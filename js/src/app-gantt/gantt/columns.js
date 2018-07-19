//@flow
//eslint-disable-next-line import/no-extraneous-dependencies
import i18n from 'i18n';

import type {GanttGridColumn, GanttTask} from './types';

import {escapeHtml, getBaseUrl, getContextPath} from '../../common/ajs-helpers';
import {PreferenceService} from '../../service/PreferenceService';

export type ColumnParams = {
    key: string,
    name?: string,
    isJiraField: boolean,
    colParams?: $Shape<GanttGridColumn>
}

function getIconSrc(src) {
    if (src.startsWith('http')) {
        return src;
    }
    return getContextPath() + src;
}

export function buildJiraFieldColumn({key, name, colParams}: ColumnParams, resizable: boolean = true) {
    return {
        ...colParams,
        name: key,
        label: name,
        align: 'left',
        isJiraField: true,
        resize: resizable,
        width: PreferenceService.get(`${PreferenceService.getPropertyPrefix()}column.${key}.width`),
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
        template: (item: GanttTask) => {
            if (item.entityId) {
                const id = escapeHtml(item.entityId);
                return `<a href="${getBaseUrl()}/browse/${id}" ${(item.type === 'issue' && item.resolved) ? 'style="text-decoration: line-through;"' : ''}>${id}</a>`;
            }
            return '';
        }
    },
    name: {
        name: 'summary',
        resize: true,
        label: i18n['ru.mail.jira.plugins.calendar.gantt.columns.name'],
        width: '*',
        align: 'left',
        template: (item: GanttTask) => {
            if (!item.icon_src) {
                return escapeHtml(item.summary);
            }

            return `<img
                class="calendar-event-issue-type"
                alt="" height="16" width="16" style="margin-right: 5px;"
                src="${item.type === 'group' ? item.icon_src : getIconSrc(item.icon_src)}"/> ${escapeHtml(item.summary)}`
        }
    },
    progress: {
        name: 'progress',
        resize: false,
        label: i18n['ru.mail.jira.plugins.calendar.gantt.columns.progress'],
        width: '80px',
        template: (item: GanttTask) => {
            if (item.type === 'issue') {
                const progress = item.progress || 0;
                const overdue = progress > 1;

                return (
                    `<div class="progressBar">
                        <div class="progressIndicator ${overdue ? 'overdue' : ''}" style="width: ${(overdue ? 1 : progress) * (80 - 12)}px"></div>
                    </div>`
                );
            }
            return '';
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

export const defaultColumns: $ReadOnlyArray<ColumnParams> = Object
    .keys(ganttColumns)
    .map(key => {
        return {
            isJiraField: false,
            key
        };
    });
