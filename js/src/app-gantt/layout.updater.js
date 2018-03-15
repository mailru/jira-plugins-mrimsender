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

        let render = false;

        if (this.view !== view) {
            this.view = view;

            const viewObject = views[view];

            //avoid unnecessary re-renders
            const showGrid = viewObject.panels.grid;
            if (this.gantt.config.show_grid !== showGrid) {
                this.gantt.config.show_grid = showGrid;
                render = true;
            }
        }

        if (render && storeService.isGanttReady()) {
            this.gantt.render();
        }
    };
}
