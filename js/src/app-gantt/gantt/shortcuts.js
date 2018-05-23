//@flow
import type {DhtmlxGantt} from './types';

export function setupShortcuts(gantt: DhtmlxGantt) {
    gantt.removeShortcut('enter', 'taskRow');
    gantt.removeShortcut('ctrl+enter', 'taskRow');
    gantt.removeShortcut('ctrl+z', 'taskRow');
    gantt.removeShortcut('ctrl+r', 'taskRow');
    gantt.removeShortcut('space', 'taskRow');
    gantt.removeShortcut('delete', 'taskRow');

    gantt.addShortcut(
        'space',
        e => {
            const taskId = gantt.locate(e);

            if (taskId) {
                const task = gantt.getTask(taskId);

                if (task.type === 'group') {
                    if (task.$open) {
                        gantt.close(taskId);
                    } else {
                        gantt.open(taskId);
                    }
                } else if (gantt.getSelectedId() !== taskId) {
                    gantt.selectTask(taskId);
                } else {
                    gantt.unselectTask();
                }
            }
        },
        'taskRow'
    );
}
