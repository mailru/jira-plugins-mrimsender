import {views} from './views';

import {storeService} from '../service/services';


export class LayoutUpdater {
    constructor(gantt, store) {
        this.gantt = gantt;

        this._update();

        store.subscribe(this._update);
    }

    _update = () => {
        const {view} = storeService.getOptions();

        let init = false;

        if (this.view !== view) {
            this.view = view;

            this.gantt.config.layout.rows = views[view].rows;
            init = true;
        }

        if (init && storeService.isGanttReady()) {
            this.gantt.init('gantt-diagram-calendar');
        }
    };
}
