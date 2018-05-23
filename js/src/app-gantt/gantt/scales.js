/* eslint-disable no-param-reassign */
//@flow
import {DEFAULT_MIN_COLUMN_WIDTH, defaultTaskCell} from './config';
import {scaleConfigs} from './scaleConfigs';
import type {DhtmlxGantt} from './types';

export function updateScales(gantt: DhtmlxGantt, scale: number) {
    const dates = gantt.getSubtaskDates();
    const config = scaleConfigs[scale].generate(gantt);

    console.log('applying config', config);
    if (config.min_width_override) {
        gantt.config.min_column_width = config.min_width_override;
    } else {
        gantt.config.min_column_width = DEFAULT_MIN_COLUMN_WIDTH;
    }

    if (config.task_cell) {
        gantt.templates.task_cell_class = config.task_cell;
    } else {
        gantt.templates.task_cell_class = defaultTaskCell(gantt);
    }

    gantt.config.scale_unit = config.scale_unit;
    if (config.date_scale) {
        gantt.config.date_scale = config.date_scale;
        gantt.templates.date_scale = undefined;
    }
    else {
        gantt.templates.date_scale = config.template;
    }

    gantt.config.step = config.step;
    gantt.config.subscales = config.subscales;

    if (dates && dates.start_date && dates.end_date) {
        gantt.config.start_date = gantt.date.add(dates.start_date, -1, config.unit);
        gantt.config.end_date = gantt.date.add(gantt.date[`${config.unit  }_start`](dates.end_date), 2, config.unit);
    } else {
        gantt.config.start_date = null;
        gantt.config.end_date = null;
    }

    gantt.config.show_task_cells = gantt.getTaskCount() < 100;

    console.log('calling render');
}
