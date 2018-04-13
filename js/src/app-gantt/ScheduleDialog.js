import React from 'react';
import PropTypes from 'prop-types';

import {connect} from 'react-redux';

// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';

import memoize from 'lodash.memoize';

import {Label} from '@atlaskit/field-base';
import Modal from '@atlaskit/modal-dialog';
import {DatePicker} from '@atlaskit/datetime-picker';
import {FieldTextStateless} from '@atlaskit/field-text';
import {colors} from '@atlaskit/theme';
import Flag from '@atlaskit/flag';

import ErrorIcon from '@atlaskit/icon/glyph/error';

import {ganttService} from '../service/services';


class ScheduleDialogInternal extends React.Component {
    static propTypes = {
        task: PropTypes.object.isRequired,
        gantt: PropTypes.any.isRequired,
        calendar: PropTypes.object.isRequired,
        onClose: PropTypes.func.isRequired,
    };

    _updateTask = () => {
        const {calendar, task, gantt, onClose} = this.props;
        const {startDate, startTime, estimate} = this.state;

        ganttService
            .estimateTask(
                calendar.id,
                task.entityId,
                {
                    start: gantt.templates.xml_format(moment(`${startDate} ${startTime}`)),
                    estimate
                }
            )
            .then(
                newTask => {
                    const {start_date, end_date, id, ...etc} = newTask;
                    Object.assign(
                        task,
                        {
                            ...etc,
                            start_date: moment(start_date).toDate(),
                            end_date: moment(end_date).toDate()
                        }
                    );
                    gantt.refreshTask(task.id);
                    onClose();
                },
                error => this.setState({ error: error.response.data })
            );
    };

    constructor(props) {
        super(props);

        const {task} = props;
        const {unscheduled, start_date, estimateSeconds} = task;

        const startMoment = moment(start_date);

        if (unscheduled) {
            this.state = {
                startDate: '',
                startTime: '',
                estimate: ''
            };
        } else {
            this.state = {
                startDate: startMoment.format('YYYY-MM-DD'),
                startTime: startMoment.format('HH:mm'),
                estimate: (estimateSeconds || 0).toString()
            };
        }
    }

    _setValue = memoize((field) => (value) => this.setState({ [field]: value }));

    _setText = memoize((field) => (e) => this.setState({ [field]: e.target.value }));

    render() {
        const {onClose, task} = this.props;
        const {startDate, startTime, estimate, error} = this.state;

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

        return (
            <Modal
                heading={`Запланировать задачу ${task.entityId}`}
                scrollBehavior="outside"

                actions={actions}
                onClose={onClose}
            >
                {error &&
                    <Flag
                        icon={<ErrorIcon primaryColor={colors.R300} label="Info" />}
                        title={error}
                    />
                }
                <Label
                    label="Дата начала"
                    isRequired={true}
                />
                <div
                    className="flex-row"
                >
                    <div className="flex-grow">
                        <DatePicker
                            value={startDate}
                            onChange={this._setValue('startDate')}
                        />
                    </div>
                    <div className="flex-none time-field">
                        <FieldTextStateless
                            placeholder="00:00"
                            isLabelHidden={true}
                            value={startTime}
                            onChange={this._setText('startTime')}
                        />
                    </div>
                </div>
                <FieldTextStateless
                    label="Оценка"
                    placeholder="Например 3w 4d 12h"
                    isRequired={true}
                    shouldFitContainer={true}

                    value={estimate}
                    onChange={this._setText('estimate')}
                />
            </Modal>
        );
    }
}

export const ScheduleDialog = connect(
    state => {
        return {
            calendar: state.calendar
        };
    }
)(ScheduleDialogInternal);
