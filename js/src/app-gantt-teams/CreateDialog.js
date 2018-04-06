import React from 'react';
import PropTypes from 'prop-types';

import {connect} from 'react-redux';

import {FieldTextStateless} from '@atlaskit/field-text';
import Modal from '@atlaskit/modal-dialog';
import Spinner from '@atlaskit/spinner';

import {noop} from '../common/util';
import {GanttTeamActionCreators} from '../service/gantt.reducer';
import {ganttTeamService, store} from '../service/services';


class CreateDialogInternal extends React.Component {
    static propTypes = {
        onClose: PropTypes.func.isRequired,
        calendar: PropTypes.object.isRequired,
        teams: PropTypes.array
    };

    state = {
        name: null,
        hasError: false,
        errorMessage: null,
        waitingForCreate: false
    };

    _createTeam = () => {
        this.setState({ waitingForCreate: true, hasError: false, errorMessage: null });
        ganttTeamService.createTeam({
            name: this.state.name,
            calendarId: this.props.calendar.id
        }).then(
            teams => {
                store.dispatch(GanttTeamActionCreators.setTeams(teams));
                this.setState({ waitingForCreate: false });
                this.props.onClose();
            },
            error => {
                this.setState({ waitingForCreate: false });
                if (error.response.data.hasOwnProperty('errors')) {
                    this.setState({ hasError: true, errorMessage: error.response.data.errors.field });
                }
            }
        )
    };

    _setName = (value) => this.setState({ name: value.target.value });

    render() {
        const {onClose} = this.props;
        const {waitingForCreate, hasError, errorMessage} = this.state;

        const actions = [
            {
                text: !waitingForCreate && 'Создать',
                onClick: this._createTeam,
                iconBefore: waitingForCreate && <Spinner/>,
                isDisabled: waitingForCreate
            },
            {
                text: 'Отмена',
                onClick: onClose,
                isDisabled: waitingForCreate
            }
        ];

        return (
            <Modal
                heading="Создать команду"
                scrollBehavior="outside"

                actions={actions}
                onClose={waitingForCreate ? noop : onClose}
                width="small"
            >
                <div className="flex-column full-width">
                    <FieldTextStateless
                        required={true}
                        label="Название команды"
                        onChange={this._setName}
                        isInvalid={hasError}
                        invalidMessage={errorMessage}
                    />
                </div>
            </Modal>
        );
    }
}

export const CreateDialog =
    connect(
        state => {
            return {
                calendar: state.calendar,
                teams: state.teams
            };
        },
        GanttTeamActionCreators
    )(CreateDialogInternal);
