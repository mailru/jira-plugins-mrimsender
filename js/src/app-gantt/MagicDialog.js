import React from 'react';
import PropTypes from 'prop-types';

import {connect} from 'react-redux';

import Modal from '@atlaskit/modal-dialog';
import Spinner from '@atlaskit/spinner';
import {Label} from '@atlaskit/field-base';
import {DatePicker} from '@atlaskit/datetime-picker';

// eslint-disable-next-line import/no-extraneous-dependencies
import moment from 'moment';

import {groupOptions} from './staticOptions';

import {SingleSelect} from '../common/ak/SingleSelect';
import {noop} from '../common/util';
import {ganttService} from '../service/services';
import {OptionsActionCreators} from '../service/gantt.reducer';


const orderOptions = [
    {
        value: 'priority',
        label: 'По приоритету'
    },
    {
        value: 'rank',
        label: 'По рангу'
    }
];

class MagicDialogInternal extends React.Component {
    static propTypes = {
        onClose: PropTypes.func.isRequired,
        gantt: PropTypes.any.isRequired,
        groupBy: PropTypes.string,
        calendar: PropTypes.object
    };

    state = {
        groupBy: null,
        orderBy: null,
        deadline: moment().add(3, 'months').format('YYYY-MM-DD'),
        waitingForMagic: false
    };

    componentDidMount() {
        this.setState({
            groupBy: this.props.groupBy
        });
    }

    _runMagic = () => {
        const {orderBy, groupBy, deadline} = this.state;

        this.setState({ waitingForMagic: true });
        ganttService
            .getOptimized(
                this.props.calendar.id,
                {
                    fields: this.props.gantt.config.columns.filter(col => col.isJiraField).map(col => col.name),
                    groupBy, orderBy, deadline
                }
            )
            .then(
                data => {
                    const {gantt} = this.props;
                    gantt.clearAll();
                    gantt.addMarker({
                        start_date: new Date(),
                        css: 'today'
                    });
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

    render() {
        const {onClose} = this.props;
        const {orderBy, groupBy, deadline, waitingForMagic} = this.state;

        const actions = [
            {
                text: !waitingForMagic && 'Запустить',
                onClick: this._runMagic,
                iconBefore: waitingForMagic && <Spinner/>,
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
                        isClearable={true}
                        isDisabled={waitingForMagic}
                        options={groupOptions}

                        value={groupBy ? groupOptions.find(val => val.value === groupBy) : null}
                        onChange={this._setGroup}
                    />
                    <SingleSelect
                        label="Ранжирование"
                        isClearable={true}
                        isDisabled={waitingForMagic}
                        options={orderOptions}

                        value={orderBy ? orderOptions.find(val => val.value === orderBy) : null}
                        onChange={this._setOrder}
                    />
                    <div>
                        <Label label="Дедлайн" isRequired={true}/>
                        <DatePicker value={deadline} onChange={this._setDeadline}/>
                    </div>
                </div>
            </Modal>
        );
    }
}

export const MagicDialog =
    connect(
        state => {
            return {
                calendar: state.calendar,
                groupBy: state.options.groupBy
            };
        },
        OptionsActionCreators
    )(MagicDialogInternal);
