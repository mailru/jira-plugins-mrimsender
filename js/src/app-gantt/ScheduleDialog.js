import React from 'react';
import PropTypes from 'prop-types';

import Modal from '@atlaskit/modal-dialog';
import {DatePicker} from '@atlaskit/datetime-picker';
import {FieldTextStateless} from '@atlaskit/field-text';


export class ScheduleDialog extends React.Component {
    static propTypes = {
        taskId: PropTypes.string.isRequired,
        gantt: PropTypes.any.isRequired,
        onClose: PropTypes.func.isRequired,
    };

    _updateTask = () => alert('todo');

    render() {
        const {onClose, taskId} = this.props;

        const waiting = false;

        const actions = [
            {
                text: 'Запланировать',
                onClick: this._updateTask,
                isDisabled: waiting
            },
            {
                text: 'Отмена',
                onClick: onClose,
                isDisabled: waiting
            }
        ];

        //todo: start date & estimate

        return (
            <Modal
                heading={`Запланировать задачу ${taskId}`}
                scrollBehavior="outside"

                actions={actions}
                onClose={onClose}
            >
                <div className="flex-row">
                    <DatePicker/>
                    <FieldTextStateless/>
                </div>
                <FieldTextStateless/>
            </Modal>
        );
    }
}


