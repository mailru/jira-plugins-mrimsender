/* eslint-disable flowtype/require-valid-file-annotation */
import React from 'react';
import PropTypes from 'prop-types';

import {connect} from 'react-redux';

import memoize from 'lodash.memoize';

import Button, {ButtonGroup} from '@atlaskit/button';
import AddIcon from '@atlaskit/icon/glyph/add';

import {CreateDialog} from './CreateDialog';
import {GanttTeam} from './GanttTeam';

import {GanttTeamActionCreators} from '../service/gantt.reducer';


class GanttTeamsInternal extends React.Component {
    static propTypes = {
        teams: PropTypes.array // eslint-disable-line react/forbid-prop-types
    };

    state = {
        activeDialog: null
    };

    _toggleDialog = memoize(
        (dialog) => () => this.setState(state => {
            if (state.activeDialog === dialog) {
                return {
                    activeDialog: null
                };
            }
            return {
                activeDialog: dialog
            };
        })
    );

    render() {
        const {activeDialog} = this.state;
        const {teams} = this.props;

        return (
            <div className="gantt-teams">
                <div className="gantt-header">
                    <div className="gantt-title">Команды</div>
                    <div className="flex-grow"/>
                    <ButtonGroup>
                        <Button
                            appearance="subtle"
                            iconBefore={<AddIcon label=""/>}
                            onClick={this._toggleDialog('create')}
                        />
                        {activeDialog === 'create' && <CreateDialog onClose={this._toggleDialog('create')}/>}
                    </ButtonGroup>
                </div>
                <div className="gantt-teams-list">
                    {teams.length === 0 ?
                        <div>There are no teams created yet. <Button appearance="link" spacing="none" onClick={this._toggleDialog('create')}>Create team.</Button></div> :
                        teams.map((team) => (
                            <GanttTeam team={team} key={team.id}/>
                        ))
                    }
                </div>
            </div>
        )
    };
}

export const GanttTeams =
    connect(
        state => {
            return {
                calendar: state.calendar,
                teams: state.teams
            };
        },
        GanttTeamActionCreators
    )(GanttTeamsInternal);
