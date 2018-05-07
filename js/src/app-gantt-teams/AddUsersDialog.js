/* eslint-disable flowtype/require-valid-file-annotation */
import React from 'react';
import PropTypes from 'prop-types';

import {connect} from 'react-redux';

import Modal from '@atlaskit/modal-dialog';
import Spinner from '@atlaskit/spinner';
import {AsyncSelect} from '@atlaskit/select';

import {noop} from '../common/util';
import {GanttTeamActionCreators} from '../service/gantt.teams.reducer';
import {ganttTeamService} from '../service/gantt.team.store';


class AddUsersDialogInternal extends React.Component {
    static propTypes = {
        team: PropTypes.object.isRequired, // eslint-disable-line react/forbid-prop-types
        onClose: PropTypes.func.isRequired,
        setTeams: PropTypes.func
    };

    state = {
        selectedUsers: [],
        waitingForAdd: false
    };

    _addUsers = () => {
        this.setState({ waitingForAdd: true });
        ganttTeamService
            .addUsers(this.props.team, this.state.selectedUsers)
            .then(
                teams => {
                    this.props.setTeams(teams);
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

    _loadOptions = inputValue => {
        return ganttTeamService
            .findUsers(this.props.team, inputValue)
            .then(users => {
                return users;
            });
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
                        onChange={this._onChange}
                        placeholder="Выберите пользователей"
                        getOptionLabel={this._getOptionLabel}
                        getOptionValue={this._getOptionValue}
                        isMulti
                    />
                </div>
            </Modal>
        );
    }
}

export const AddUsersDialog =
    connect(
        null,
        GanttTeamActionCreators
    )(AddUsersDialogInternal);
