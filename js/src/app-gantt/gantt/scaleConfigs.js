/* eslint-disable flowtype/require-valid-file-annotation */
// eslint-disable-next-line import/no-extraneous-dependencies
import i18n from 'i18n';

import {hoursTaskCell} from './config';
import type {DhtmlxGantt} from './types';


//Setting available scales
export const scaleConfigs = [
    // hours
    {
        generate: (gantt: DhtmlxGantt) => ({
            min_width_override: 20,
            task_cell: hoursTaskCell(gantt),
            unit: 'hour', step: 1, scale_unit: 'day', date_scale: '%j %M',
            subscales: [
                { unit: 'hour', step: 1, date: '%H' }
            ]
        })
    },
    {
        key: 'week',
        title: i18n['ru.mail.jira.plugins.calendar.gantt.scales.week'],
        generate: () => ({
            min_width_override: 40,
            unit: 'day', step: 1, scale_unit: 'month', date_scale: '%F',
            subscales: [
                { unit: 'day', step: 1, date: '%j' }
            ]
        })
    },
    // days
    {
        key: 'month',
        title: i18n['ru.mail.jira.plugins.calendar.gantt.scales.month'],
        generate: () => ({
            min_width_override: 20,
            unit: 'week', step: 1, scale_unit: 'month', date_scale: '%F',
            subscales: [
                { unit: 'day', step: 1, date: '%j' }
            ]
        })
    },
    // weeks
    {
        key: 'quarter',
        title: i18n['ru.mail.jira.plugins.calendar.gantt.scales.quarter'],
        generate: (gantt: DhtmlxGantt) => ({
            min_width_override: 40,
            unit: 'week', step: 1, scale_unit: 'month', date_scale: '%F',
            subscales: [
                {
                    unit: 'week', step: 1,
                    css: () => 'text-left',
                    template: (date) => {
                        const dateToStr = gantt.date.date_to_str('%d');
                        //const endDate = gantt.date.add(gantt.date.add(date, 1, 'week'), -1, 'day');
                        return dateToStr(date);// + ' - ' + dateToStr(endDate);
                    },
                },
                /*{
                    unit: 'day',
                    step: 1,
                    template: (date) => gantt.date.date_to_str('%D')(date).substring(0, 1)
                }*/
            ]
        })
    },
    // months
    {
        generate: () => ({
            unit: 'month', step: 1, scale_unit: 'year', date_scale: '%Y',
            subscales: [
                { unit: 'month', step: 1, date: '%M' }
            ]
        })
    },
    // quarters
    {
        generate: (gantt: DhtmlxGantt) => ({
            unit: 'month', step: 3, scale_unit: 'year', date_scale: '%Y',
            subscales: [
                {
                    unit: 'month', step: 3,
                    template(date) {
                        const dateToStr = gantt.date.date_to_str('%M');
                        const endDate = gantt.date.add(gantt.date.add(date, 3, 'month'), -1, 'day');
                        return `${dateToStr(date)  } - ${  dateToStr(endDate)}`;
                    }
                }
            ]
        })
    },
    // years
    {
        generate: (gantt: DhtmlxGantt) => ({
            unit: 'year', step: 1, scale_unit: 'year', date_scale: '%Y',
            subscales: [
                {
                    unit: 'year', step: 5,
                    template(date) {
                        const dateToStr = gantt.date.date_to_str('%Y');
                        const endDate = gantt.date.add(gantt.date.add(date, 5, 'year'), -1, 'day');
                        return `${dateToStr(date)  } - ${  dateToStr(endDate)}`;
                    }
                }
            ]
        })
    },
    // decades
    {
        generate: (gantt: DhtmlxGantt) => ({
            unit: 'year', step: 10, scale_unit: 'year',
            template(date) {
                const dateToStr = gantt.date.date_to_str('%Y');
                const endDate = gantt.date.add(gantt.date.add(date, 10, 'year'), -1, 'day');
                return `${dateToStr(date)  } - ${  dateToStr(endDate)}`;
            },
            subscales: [
                {
                    unit: 'year', step: 100,
                    template(date) {
                        const dateToStr = gantt.date.date_to_str('%Y');
                        const endDate = gantt.date.add(gantt.date.add(date, 100, 'year'), -1, 'day');
                        return `${dateToStr(date)  } - ${  dateToStr(endDate)}`;
                    }
                }
            ]
        })
    }
];

export const keyedConfigs = scaleConfigs
    .map((config, i) => {
        return {
            key: config.key,
            i
        };
    })
    .filter(config => config.key);
