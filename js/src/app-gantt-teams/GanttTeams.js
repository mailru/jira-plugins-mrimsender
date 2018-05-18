/* eslint-disable flowtype/require-valid-file-annotation */
import React from 'react';
import PropTypes from 'prop-types';

import {connect} from 'react-redux';

import memoize from 'lodash.memoize';

// eslint-disable-next-line import/no-extraneous-dependencies
import i18n from 'i18n';

import Button, {ButtonGroup} from '@atlaskit/button';
import AddIcon from '@atlaskit/icon/glyph/add';

import {CreateDialog} from './CreateDialog';
import {GanttTeam} from './GanttTeam';

class GanttTeamsInternal extends React.Component {
    static propTypes = {
        calendar: PropTypes.object, // eslint-disable-line react/forbid-prop-types
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
        const {teams, calendar} = this.props;

        return (
            <div className="gantt-teams">
                <div className="gantt-header">
                    <div className="gantt-title">
                        {calendar == null ?
                            i18n['ru.mail.jira.plugins.calendar.teams.title'] :
                            `${i18n['ru.mail.jira.plugins.calendar.teams.titleForCalendar']} "${calendar.name}"`
                        }
                    </div>
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
                        <div>
                            {i18n['ru.mail.jira.plugins.calendar.teams.noTeams']}{' '}
                            <Button appearance="link" spacing="none" onClick={this._toggleDialog('create')}>
                                {i18n['ru.mail.jira.plugins.calendar.teams.createTeam']}
                            </Button>
                        </div> :
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
        null
    )(GanttTeamsInternal);
