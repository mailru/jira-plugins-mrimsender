//@flow
import React from 'react';
import PropTypes from 'prop-types';

import {connect} from 'react-redux';

// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';
// eslint-disable-next-line import/no-extraneous-dependencies
import i18n from 'i18n';

import memoize from 'lodash.memoize';

import {Label} from '@atlaskit/field-base';
import Modal from '@atlaskit/modal-dialog';
import {DatePicker} from '@atlaskit/datetime-picker';
import {FieldTextStateless} from '@atlaskit/field-text';
import {colors} from '@atlaskit/theme';
import Flag from '@atlaskit/flag';

import ErrorIcon from '@atlaskit/icon/glyph/error';

import {updateTask} from './gantt/util';

import {ganttService} from '../service/services';

import type {CalendarType, VoidCallback} from './types';
import type {DhtmlxGantt, GanttIssueTask} from './gantt/types';


type Props = {
    task: GanttIssueTask,
    gantt: DhtmlxGantt,
    calendar: CalendarType,
    onClose: VoidCallback
}

type State = {
    startDate: string,
    startTime: string,
    estimate: string,
    error: ?string
}

class ScheduleDialogInternal extends React.Component<Props, State> {
    static propTypes = {
        // eslint-disable-next-line react/forbid-prop-types
        task: PropTypes.object.isRequired,
        // eslint-disable-next-line react/forbid-prop-types
        gantt: PropTypes.any.isRequired,
        // eslint-disable-next-line react/forbid-prop-types
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
                    start: startDate ? gantt.templates.xml_format(moment(`${startDate} ${startTime}`).toDate()) : null,
                    estimate
                },
                {
                    fields: gantt.config.columns.filter(col => col.isJiraField).map(col => col.name)
                }
            )
            .then(
                newTask => {
                    updateTask(gantt, task, newTask);
                    onClose();
                },
                error => this.setState({ error: error.response.data })
            );
    };

    constructor(props) {
        super(props);

        const {task} = props;
        // eslint-disable-next-line camelcase
        const {unscheduled, start_date, estimate} = task;

        // eslint-disable-next-line camelcase
        const startMoment = moment(start_date);

        if (unscheduled) {
            this.state = {
                startDate: '',
                startTime: '',
                estimate: estimate || '',
                error: null
            };
        } else {
            this.state = {
                startDate: startMoment.format('YYYY-MM-DD'),
                startTime: startMoment.format('HH:mm'),
                estimate: estimate || '',
                error: null
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
                text: i18n['ru.mail.jira.plugins.calendar.gantt.schedule.doEstimate'],
                onClick: this._updateTask,
                isDisabled: waiting
            },
            {
                text: i18n['ru.mail.jira.plugins.calendar.common.cancel'],
                onClick: onClose,
                isDisabled: waiting
            }
        ];

        return (
            <Modal
                heading={`${i18n['ru.mail.jira.plugins.calendar.gantt.schedule.title']} ${task.entityId ? task.entityId : ''}`}
                scrollBehavior="outside"
                autoFocus={false}

                actions={actions}
                onClose={onClose}
            >
                {error &&
                    //$FlowFixMe
                    <Flag
                        icon={<ErrorIcon primaryColor={colors.R300} label="Info" />}
                        title={error}
                    />
                }
                <Label
                    label={i18n['ru.mail.jira.plugins.calendar.gantt.schedule.startDate']}
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
                            isLabelHidden
                            value={startTime}
                            onChange={this._setText('startTime')}
                        />
                    </div>
                </div>
                <FieldTextStateless
                    label={i18n['ru.mail.jira.plugins.calendar.gantt.schedule.estimate']}
                    placeholder={i18n['ru.mail.jira.plugins.calendar.gantt.schedule.estimatePlaceholder']}
                    isRequired
                    shouldFitContainer

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
            calendar: state.calendar,
            options: state.options
        };
    }
)(ScheduleDialogInternal);
