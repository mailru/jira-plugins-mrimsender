/* eslint-disable flowtype/require-valid-file-annotation */
import { DEFAULT_MIN_COLUMN_WIDTH, defaultTaskCell } from './ganttConfig';
import { scaleConfigs } from './scaleConfigs';

import { storeService } from '../service/services';


export class ScaleUpdater {
    constructor(gantt, store) {
        this.gantt = gantt;

        this._update();

        store.subscribe(this._update);
    }

    _update = () => {
        const newScale = this._getScale();

        if (storeService.isGanttReady()) {
            if (this.scale !== newScale) {
                this.scale = newScale;

                this.updateScales();
                this.gantt.render();
            }
        }
    };

    updateScales = () => {
        const dates = this.gantt.getSubtaskDates();
        const config = scaleConfigs[this.scale];

        console.log('applying config', config);
        if (config.min_width_override) {
            this.gantt.config.min_column_width = config.min_width_override;
        } else {
            this.gantt.config.min_column_width = DEFAULT_MIN_COLUMN_WIDTH;
        }

        if (config.task_cell) {
            this.gantt.templates.task_cell_class = config.task_cell;
        } else {
            this.gantt.templates.task_cell_class = defaultTaskCell;
        }

        this.gantt.config.scale_unit = config.scale_unit;
        if (config.date_scale) {
            this.gantt.config.date_scale = config.date_scale;
            this.gantt.templates.date_scale = null;
        }
        else {
            this.gantt.templates.date_scale = config.template;
        }

        this.gantt.config.step = config.step;
        this.gantt.config.subscales = config.subscales;

        if (dates && dates.start_date && dates.end_date) {
            this.gantt.config.start_date = this.gantt.date.add(dates.start_date, -1, config.unit);
            this.gantt.config.end_date = this.gantt.date.add(this.gantt.date[`${config.unit  }_start`](dates.end_date), 2, config.unit);
        } else {
            this.gantt.config.start_date = null;
            this.gantt.config.end_date = null;
        }

        this.gantt.config.show_task_cells = this.gantt.getTaskCount() < 100;

        console.log('calling render');
    };

    _getScale = () => {
        return storeService.getOptions().scale;
    };
}
