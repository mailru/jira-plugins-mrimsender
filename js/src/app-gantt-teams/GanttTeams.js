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
        calendar: PropTypes.object,
        teams: PropTypes.array
    };

    state ={
        activeDialog: null,
        teams: [
            {
                id: 1,
                name: 'svdfgsgfsdg sgdfgs sfgdf sdfg sdfgsdfsg dfg sdfgsd gf',
                users: [
                    {
                        key: 'admin',
                        displayName: 'Admin Admin',
                        email: 'dfdfas@sdfas.rt',
                        avatarUrl: 'http://www.gravatar.com/avatar/64e1b8d34f425d19e1ee2ea7236d3028?d=mm&s=48',
                        weeklyHours: 40
                    },
                    {
                        key: 'daria',
                        displayName: 'Daria Sabitova',
                        email: 'dfdfas@sdfas.rt',
                        avatarUrl: 'http://www.gravatar.com/avatar/64e1b8d34f425d19e1ee2ea7236d3028?d=mm&s=48',
                        weeklyHours: 16
                    },
                    {
                        key: 'daria',
                        displayName: 'Daria Sabitova',
                        email: 'dfdfas@sdfas.rt',
                        avatarUrl: 'http://www.gravatar.com/avatar/64e1b8d34f425d19e1ee2ea7236d3028?d=mm&s=48',
                        weeklyHours: 16
                    },
                    {
                        key: 'daria',
                        displayName: 'Daria Sabitova',
                        email: 'dfdfas@sdfas.rt',
                        avatarUrl: 'http://www.gravatar.com/avatar/64e1b8d34f425d19e1ee2ea7236d3028?d=mm&s=48',
                        weeklyHours: 16
                    },
                    {
                        key: 'daria',
                        displayName: 'Daria Sabitova',
                        email: 'dfdfas@sdfas.rt',
                        avatarUrl: 'http://www.gravatar.com/avatar/64e1b8d34f425d19e1ee2ea7236d3028?d=mm&s=48',
                        weeklyHours: 16
                    }
                ]
            },
            {
                id: 2,
                name: 'Test Team',
                users: [
                    {
                        key: 'admin',
                        displayName: 'Admin Admin',
                        email: 'dfdfas@sdfas.rt',
                        avatarUrl: 'http://www.gravatar.com/avatar/64e1b8d34f425d19e1ee2ea7236d3028?d=mm&s=48',
                        weeklyHours: 40
                    },
                    {
                        key: 'daria',
                        displayName: 'Daria Sabitova',
                        email: 'dfdfas@sdfas.rt',
                        avatarUrl: 'http://www.gravatar.com/avatar/64e1b8d34f425d19e1ee2ea7236d3028?d=mm&s=48',
                        weeklyHours: 16
                    }
                ]
            }
        ]
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
                        teams.map((team, index) => (
                            <GanttTeam team={team} key={index}/>
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
