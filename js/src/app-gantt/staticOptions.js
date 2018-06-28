//@flow
// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';
// eslint-disable-next-line import/no-extraneous-dependencies
import i18n from 'i18n';

import {keyedConfigs} from './gantt/scaleConfigs';
import {views} from './views';
import type {OptionsType} from './types';


const groupKeys = [
    'assignee',
    'reporter',
    'project',
    'issueType',
    'priority',
    'resolution',
    'component',
    'fixVersion',
    'affectsVersion',
    'labels',
    'epicLink'
];

export const groupOptions = groupKeys.map(key => ({
    value: key,
    label: i18n[`ru.mail.jira.plugins.calendar.group.${key}`]
}));

export const defaultOptions: OptionsType = {
    liveData: true,
    filter: '',
    scale: keyedConfigs[1].i,
    startDate: moment().subtract(1, 'months').format('YYYY-MM-DD'),
    endDate: moment().add(3, 'months').format('YYYY-MM-DD'),
    groupBy: undefined,
    orderBy: undefined,
    isOrderedByRank: false,
    sprint: undefined,
    order: true,
    withUnscheduled: false,
    view: views.basic.key,
    columns: [
        {
            key: 'timeoriginalestimate',
            name: i18n['issue.field.timeoriginalestimate'],
            isJiraField: true,
            colParams: {
                width: '53px'
            }
        },
        {
            key: 'assignee',
            name: i18n['issue.field.assignee'],
            isJiraField: true,
            colParams: {
                width: '200px'
            }
        }
    ]
};
