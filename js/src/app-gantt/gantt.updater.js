import {storeService} from '../service/services';
import {getPluginBaseUrl} from '../common/ajs-helpers';


export class GanttUpdater {
    constructor(gantt, store, loadGantt) {
        this.gantt = gantt;
        this.loadGantt = loadGantt;

        this._update();

        store.subscribe(this._update);
    }

    _update = () => {
        const {startDate, endDate} = storeService.getOptions();
        const calendar = storeService.getCalendar();
        if (storeService.isGanttReady() && calendar) {
            if (this.startDate !== startDate || this.endDate !== endDate || calendar.id !== (this.calendar || {}).id) {
                this.startDate = startDate;
                this.endDate = endDate;
                this.calendar = calendar;

                console.log('loading gantt');

                this.gantt.clearAll();
                this.gantt.addMarker({
                    start_date: new Date(),
                    css: 'today',
                    //text: 'Today'
                });
                this.gantt.load(`${getPluginBaseUrl()}/gantt/${this.calendar.id}?start=${startDate}&end=${endDate}`);
            }
        } else {
            console.log('no calendar');
        }
    };
}
