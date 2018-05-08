// @flow
import React from 'react';

import { connect } from 'react-redux';

import Modal from '@atlaskit/modal-dialog';
import Spinner from '@atlaskit/spinner';
import { Label } from '@atlaskit/field-base';
import { DatePicker } from '@atlaskit/datetime-picker';
import Select from '@atlaskit/select';
// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';

import { groupOptions } from './staticOptions';

import { SingleSelect } from '../common/ak/SingleSelect';
import { SprintState } from '../common/sprints';

import { noop } from '../common/util';
import { ganttService } from '../service/services';
import { OptionsActionCreators } from '../service/gantt.reducer';
import type { SelectOption } from '../common/types';


const orderOptions: Array<SelectOption> = [
    {
        value: 'priority',
        label: 'По приоритету'
    },
    {
        value: 'rank',
        label: 'По рангу'
    }
];

function mapSprint(sprint) {
    if (!sprint) {
        return null;
    }

    const { id, name, ...etc } = sprint;
    return {
        ...etc,
        value: id,
        label: name
    };
}

function formatSprintLabel({ boardName, state, label }: { boardName: string, state: string, label: string }) {
    if (!boardName) {
        return label;
    }
    return (
        <span>
            <SprintState state={state} />
            {' '}
            <strong>
                {boardName}
            </strong>
            {' - '}
            {label}
        </span>
    );
}

type Props = {
    onClose: () => void,
    updateOptions: ({ liveData: bool }) => void,
    gantt: any,// todo
    sprints: Array<{ id: string, name: string }>,
    groupBy: { value: string } | null,
    sprint?: number,
    calendar: { id: string }
}

type State = {
    groupBy: { value: string } | null,
    orderBy: { value: string } | null,
    sprint: { value: string, label: string } | null,
    deadline: string,
    waitingForMagic: bool
}

class MagicDialogInternal extends React.Component<Props, State> {

    state = {
        groupBy: null,
        orderBy: null,
        sprint: null,
        deadline: moment().add(3, 'months').format('YYYY-MM-DD'),
        waitingForMagic: false
    };

    componentDidMount() {
        const { sprint, sprints, groupBy } = this.props;

        // todo remove setState
        // eslint-disable-next-line react/no-did-mount-set-state
        this.setState({
            groupBy,
            sprint: sprint ? mapSprint(sprints.find(item => item.id === sprint)) : null
        });
    }

    _runMagic = () => {
        const { orderBy, groupBy, deadline, sprint } = this.state;

        this.setState({ waitingForMagic: true });
        ganttService
            .getOptimized(
                this.props.calendar.id,
                {
                    fields: this.props.gantt.config.columns.filter(col => col.isJiraField).map(col => col.name),
                    sprint: sprint ? sprint.value : '',
                    groupBy, orderBy, deadline
                }
            )
            .then(
                data => {
                    const { gantt } = this.props;
                    gantt.clearAll();
                    gantt.addMarker({
                        start_date: new Date(),
                        css: 'today'
                    });
                    gantt.config.show_task_cells = data.data.length < 100;
                    gantt.parse(data);
                    this.setState({ waitingForMagic: false });
                    this.props.updateOptions({ liveData: false });
                    this.props.onClose();
                },
                error => {
                    this.setState({ waitingForMagic: false });
                    alert(error);
                    throw error;
                }
            );
    };

    _setOrder = (value) => this.setState({ orderBy: value ? value.value : null });

    _setGroup = (value) => this.setState({ groupBy: value ? value.value : null });

    _setDeadline = (deadline) => this.setState({ deadline });

    _setSprint = (sprint) => this.setState({ sprint });

    render() {
        const { onClose, sprints } = this.props;
        const { orderBy, groupBy, sprint, deadline, waitingForMagic } = this.state;

        const actions = [
            {
                text: !waitingForMagic && 'Запустить',
                onClick: this._runMagic,
                iconBefore: waitingForMagic && <Spinner />,
                isDisabled: waitingForMagic
            },
            {
                text: 'Отмена',
                onClick: onClose,
                isDisabled: waitingForMagic
            }
        ];

        return (
            <Modal
                heading="Параметры планирования"
                scrollBehavior="outside"

                actions={actions}
                onClose={waitingForMagic ? noop : onClose}
            >
                <div className="flex-column full-width">
                    <SingleSelect
                        label="Группировка"
                        isClearable
                        isDisabled={waitingForMagic}
                        options={groupOptions}

                        value={groupBy ? groupOptions.find(val => val.value === groupBy) : null}
                        onChange={this._setGroup}
                    />
                    <SingleSelect
                        label="Ранжирование"
                        isClearable
                        isDisabled={waitingForMagic}
                        options={orderOptions}

                        value={orderBy ? orderOptions.find(val => val.value === orderBy) : null}
                        onChange={this._setOrder}
                    />
                    <div>
                        <Label label="Спринт" />
                        <Select
                            defaultOptions
                            formatOptionLabel={formatSprintLabel}
                            options={sprints.map(mapSprint)}

                            value={sprint}
                            onChange={this._setSprint}
                        />
                    </div>
                    <div>
                        <Label label="Дедлайн" isRequired />
                        <DatePicker
                            value={deadline}
                            isDisabled={waitingForMagic}
                            onChange={this._setDeadline}
                        />
                    </div>
                </div>
            </Modal>
        );
    }
}

export const MagicDialog =
    connect(
        ({ calendar, sprints, options }) => {
            return {
                calendar, sprints,
                groupBy: options.groupBy,
                sprint: options.sprint
            };
        },
        OptionsActionCreators
    )(MagicDialogInternal);
