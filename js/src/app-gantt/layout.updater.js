import {storeService} from '../service/services';


export class LayoutUpdater {
    constructor(gantt, store) {
        this.gantt = gantt;

        this._update();

        store.subscribe(this._update);
    }

    _update = () => {
        const showGrid = storeService.getOptions().showGrid;

        if (this.showGrid !== showGrid) {
            this.showGrid = showGrid;

            //avoid unnecessary re-renders
            if (this.gantt.config.show_grid !== showGrid) {
                this.gantt.config.show_grid = showGrid;

                if (storeService.isGanttReady()) {
                    this.gantt.render();
                }
            }
        }
    };
}
