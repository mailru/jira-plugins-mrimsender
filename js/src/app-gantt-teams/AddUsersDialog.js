import React from 'react';
import PropTypes from 'prop-types';

import {connect} from 'react-redux';

import Modal from '@atlaskit/modal-dialog';
import Spinner from '@atlaskit/spinner';
import {AsyncSelect} from '@atlaskit/select';

import {noop} from '../common/util';
import {GanttTeamActionCreators} from '../service/gantt.reducer';
import {ganttTeamService, store} from '../service/services';


class AddUsersDialogInternal extends React.Component {
    static propTypes = {
        team: PropTypes.object.isRequired,
        onClose: PropTypes.func.isRequired,
    };

    state = {
        inputValue: '',
        selectedUsers: [],
        waitingForAdd: false
    };

    componentDidMount() {
        ganttTeamService.findUsers(this.props.team, '')
            .then(users => {
                this.setState({users: users});
            });
    }

    _addUsers = () => {
        this.setState({ waitingForAdd: true });
        ganttTeamService
            .addUsers(this.props.team, this.state.selectedUsers)
            .then(
                teams => {
                    store.dispatch(GanttTeamActionCreators.setTeams(teams));
                    this.setState({ waitingForAdd: false });
                    this.props.onClose();
                },
                error => {
                    this.setState({ waitingForAdd: false });
                    alert(error);
                    throw error;
                }
            );
    };

    _setOrder = (value) => this.setState({ orderBy: value ? value.value : null });

    _setGroup = (value) => this.setState({ groupBy: value ? value.value : null });

    _setDeadline = (deadline) => this.setState({ deadline });

    _loadOptions = inputValue => {
        return ganttTeamService
            .findUsers(this.props.team, inputValue)
            .then(users => {
                return users;
            });
    };

    _handleInputChange = (newValue) => {
        const inputValue = newValue.replace(/\W/g, '');
        this.setState({ inputValue });
        return inputValue;
    };

    _getOptionLabel = option => {
        return option.displayName;
    };

    _getOptionValue = option => {
        return option.key;
    };

    _onChange = options => {
        this.setState({selectedUsers: options});
    };

    render() {
        const {onClose} = this.props;
        const {waitingForAdd} = this.state;

        const actions = [
            {
                text: !waitingForAdd && 'Добавить',
                onClick: this._addUsers,
                iconBefore: waitingForAdd && <Spinner/>,
                isDisabled: waitingForAdd
            },
            {
                text: 'Отмена',
                onClick: onClose,
                isDisabled: waitingForAdd
            }
        ];

        return (
            <Modal
                heading="Добавить пользователей в команду"
                scrollBehavior="outside"

                actions={actions}
                onClose={waitingForAdd ? noop : onClose}
                width="small"
            >
                <div className="flex-column full-width">
                    <AsyncSelect
                        cacheOptions
                        loadOptions={this._loadOptions}
                        defaultOptions
                        onInputChange={this._handleInputChange}
                        onChange={this._onChange}
                        placeholder="Выберите пользователей"
                        getOptionLabel={this._getOptionLabel}
                        getOptionValue={this._getOptionValue}
                        isMulti={true}
                    />
                </div>
            </Modal>
        );
    }
}

export const AddUsersDialog =
    connect(
        state => {
            return {
                calendar: state.calendar
            };
        },
        GanttTeamActionCreators
    )(AddUsersDialogInternal);
