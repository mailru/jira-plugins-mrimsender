/* eslint-disable flowtype/require-valid-file-annotation */
import React from 'react';
import PropTypes from 'prop-types';

import {connect} from 'react-redux';

import memoize from 'lodash.memoize';

// eslint-disable-next-line import/no-extraneous-dependencies
import AJS from 'AJS';

import {FieldTextStateless} from '@atlaskit/field-text';
import Button, {ButtonGroup} from '@atlaskit/button';
import InlineDialog from '@atlaskit/inline-dialog';
import DropdownMenu, { DropdownItemGroupRadio, DropdownItemRadio } from '@atlaskit/dropdown-menu';
import Spinner from '@atlaskit/spinner';
import Tooltip from '@atlaskit/tooltip';
import Banner from '@atlaskit/banner';

import ChevronDownIcon from '@atlaskit/icon/glyph/chevron-down';
import MediaServicesZoomInIcon from '@atlaskit/icon/glyph/media-services/zoom-in';
import MediaServicesZoomOutIcon from '@atlaskit/icon/glyph/media-services/zoom-out';
import CalendarIcon from '@atlaskit/icon/glyph/calendar-filled';
import SearchIcon from '@atlaskit/icon/glyph/search';
import ListIcon from '@atlaskit/icon/glyph/list';
import JiraLabsIcon from '@atlaskit/icon/glyph/jira/labs';
import FilterIcon from '@atlaskit/icon/glyph/filter';
import CheckIcon from '@atlaskit/icon/glyph/check';
import VidFullScreenOnIcon from '@atlaskit/icon/glyph/vid-full-screen-on';
import VidFullScreenOffIcon from '@atlaskit/icon/glyph/vid-full-screen-off';
import WarningIcon from '@atlaskit/icon/glyph/warning';

import {ScheduleDialog} from './ScheduleDialog';
import {keyedConfigs, scaleConfigs} from './scaleConfigs';
import {viewItems} from './views';

import {OptionsDialog} from './OptionsDialog';
import {DatesDialog} from './DatesDialog';
import {MagicDialog} from './MagicDialog';

import {SprintState} from '../common/sprints';

import {OptionsActionCreators} from '../service/gantt.reducer';
import {calendarService, ganttService} from '../service/services';


const enableMagic = true;

class GanttActionsInternal extends React.Component {
    static propTypes = {
        // eslint-disable-next-line react/forbid-prop-types
        gantt: PropTypes.object.isRequired,
        // eslint-disable-next-line react/forbid-prop-types
        options: PropTypes.object.isRequired,
        // eslint-disable-next-line react/forbid-prop-types
        calendar: PropTypes.object,
        sprints: PropTypes.arrayOf(PropTypes.object.isRequired),
        updateOptions: PropTypes.func
    };

    state = {
        activeDialog: null,
        waitingForPlan: false,
        calendars: null,
        filter: '',
        schedulingTask: null
    };

    componentDidMount() {
        const {gantt} = this.props;

        gantt.attachEvent(
            'onTaskDblClick',
            (id) => {
                if (id) {
                    this._openScheduleDialog(gantt.getTask(id));
                }
                return true;
            }
        );

        gantt.addShortcut(
            'enter',
            e => {
                const taskId = gantt.locate(e);
                if (taskId) {
                    this._openScheduleDialog(gantt.getTask(taskId));
                }
            },
            'taskRow'
        );
    }

    _applyPlan = () => {
        const {gantt, calendar, options} = this.props;

        if (options.liveData) {
            return;
        }

        const tasks = gantt.getTaskBy(task => task.type === 'issue');

        this.setState({
            waitingForPlan: true
        });

        ganttService
            .applyPlan(
                calendar.id,
                {
                    // eslint-disable-next-line camelcase
                    items: tasks.map(({id, start_date, duration}) => {
                        return {
                            taskId: id,
                            start_date: gantt.templates.xml_format(start_date),
                            duration
                        };
                    })
                }
            )
            .then(
                () => {
                    this.setState({waitingForPlan: false});
                    this.props.updateOptions({ liveData: true });
                },
                error => {
                    this.setState({waitingForPlan: false});
                    AJS.flag({
                        title: 'Не удалось применить изменения',
                        body: error.response.responseText,
                        type: 'error',
                        close: 'manual'
                    });
                }
            );
    };

    _zoomIn = () => {
        const currentScale = this.props.options.scale;
        if (currentScale > 0) {
            this.props.updateOptions({ scale: currentScale - 1 });
        }
    };

    _zoomOut = () => {
        const currentScale = this.props.options.scale;
        if ((currentScale + 1) < scaleConfigs.length) {
            this.props.updateOptions({ scale: currentScale + 1 });
        }
    };

    _zoomToFit = () => {
        const {gantt} = this.props;

        const project = gantt.getSubtaskDates();
        const areaWidth = gantt.$task.offsetWidth;

        let i;
        for (i = 0; i < scaleConfigs.length; i++) {
            const columnCount = this._getUnitsBetween(project.start_date, project.end_date, scaleConfigs[i].unit, scaleConfigs[i].step);
            if ((columnCount + 2) * gantt.config.min_column_width <= areaWidth) {
                break;
            }
        }

        if (i === scaleConfigs.length) {
            i--;
        }

        this.props.updateOptions({ scale: i });
    };

    // get number of columns in timeline
    _getUnitsBetween =  (from, to, unit, step) => {
        const {gantt} = this.props;

        let start = new Date(from);
        const end = new Date(to);
        let units = 0;
        while (start.valueOf() < end.valueOf()) {
            units++;
            start = gantt.date.add(start, step, unit);
        }
        return units;
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

    _setFilter = (e) => this.setState({ filter: e.target.value });

    _setScale = (scale) => () => this.props.updateOptions({ scale });

    _setView = (view) => () => this.props.updateOptions({ view });

    _applyFilter = () => this.props.updateOptions({ filter: this.state.filter });

    _fetchCalendars = () => calendarService
        .getUserCalendars()
        .then(calendars => this.setState({
            calendars: calendars
                .filter(cal => cal.ganttEnabled)
        }));

    _onCalendarListOpen = () => {
        if (!this.state.calendars) {
            this._fetchCalendars();
        }
    };

    _selectSprint = (sprint) => () => this.props.updateOptions({ sprint });

    _updateStructure = (isOpen) => {
        const {gantt} = this.props;

        gantt.eachTask((task) => {
            // eslint-disable-next-line no-param-reassign
            task.$open = isOpen;
        });

        gantt.render();
    };

    _collapseStructure = () => this._updateStructure(false);

    _expandStructure = () => this._updateStructure(true);

    _openScheduleDialog = (task) => {
        if (task.type === 'issue') {
            this.setState({schedulingTask: task}, this._toggleDialog('scheduleTask'));
        }
    };

    render() {
        const {activeDialog, waitingForPlan, calendars, filter, schedulingTask} = this.state;
        const {options, calendar, sprints, gantt} = this.props;

        const currentSprint = options.sprint ?
            (sprints.find(sprint => sprint.id === options.sprint) || { id: options.sprint, name: 'Неизвестный спринт' }) : null;

        return (
            <div>
                {calendar && calendar.errors && !!calendar.errors.length &&
                    <div style={{margin: '0 -20px'}}>
                        <Banner isOpen icon={<WarningIcon label="warning" secondaryColor="inherit"/>}>
                            <div className="flex-column">
                                {calendar.errors.map((e) => <div key={ e }>{e}</div>)}
                            </div>
                        </Banner>
                    </div>
                }
                {activeDialog === 'scheduleTask' &&
                    <ScheduleDialog
                        gantt={gantt}
                        onClose={this._toggleDialog('scheduleTask')}
                        task={schedulingTask}
                    />
                }
                {/*<PageHeader>
                    {calendar && i18n.calendarTitle(calendar.selectedName)}
                </PageHeader>*/}
                <div className="gantt-actions">
                    <div className="flex-row">
                        <div>
                            <ButtonGroup>
                                {enableMagic &&
                                <Button
                                    iconBefore={<JiraLabsIcon label=""/>}

                                    onClick={this._toggleDialog('magic')}
                                >
                                    Запустить магию
                                </Button>}
                                {!options.liveData &&
                                    <Button
                                        onClick={this._applyPlan}

                                        isDisabled={waitingForPlan}
                                        iconBefore={waitingForPlan ? <Spinner/> : <CheckIcon label=""/>}
                                    >
                                        Применить изменения
                                    </Button>
                                }
                                {activeDialog === 'magic' && <MagicDialog onClose={this._toggleDialog('magic')} gantt={gantt}/>}
                            </ButtonGroup>
                        </div>
                        <div className="flex-horizontal-middle flex-grow">
                            <ButtonGroup>
                                {keyedConfigs.map(config =>
                                    <Button
                                        key={config.key}
                                        isSelected={options.scale === config.i}

                                        onClick={this._setScale(config.i)}
                                    >
                                        {scaleConfigs[config.i].title}
                                    </Button>
                                )}
                                <Button
                                    iconBefore={<MediaServicesZoomInIcon label="Zoom in"/>}

                                    isDisabled={options.scale === 0}

                                    onClick={this._zoomIn}
                                />
                                <Button
                                    iconBefore={<MediaServicesZoomOutIcon label="Zoom out"/>}

                                    isDisabled={options.scale+1 === scaleConfigs.length}

                                    onClick={this._zoomOut}
                                />
                                <Button
                                    onClick={this._zoomToFit}
                                    iconBefore={<SearchIcon label="Reset zoom"/>}
                                />
                            </ButtonGroup>
                        </div>
                        <div>
                            <ButtonGroup>
                                <InlineDialog
                                    position="bottom right"
                                    isOpen={activeDialog === 'dates'}
                                    content={(activeDialog === 'dates') && <DatesDialog gantt={gantt} onClose={this._toggleDialog('dates')}/>}
                                >
                                    <Button
                                        iconBefore={<CalendarIcon/>}
                                        onClick={this._toggleDialog('dates')}
                                    >
                                        Период
                                    </Button>
                                </InlineDialog>
                                <DropdownMenu
                                    trigger="Вид"
                                    triggerType="button"
                                    triggerButtonProps={{iconBefore: <ListIcon label=""/>}}
                                    shouldFlip={false}
                                    position="bottom right"
                                    boundariesElement="window"
                                >
                                    <DropdownItemGroupRadio id="gantt-view">
                                        {viewItems.map(item =>
                                            <DropdownItemRadio
                                                key={item.key}
                                                id={item.key}
                                                isSelected={options.view === item.key}
                                                onClick={this._setView(item.key)}
                                            >
                                                {item.name}
                                            </DropdownItemRadio>
                                        )}
                                    </DropdownItemGroupRadio>
                                </DropdownMenu>
                            </ButtonGroup>
                        </div>
                    </div>

                    <div className="gantt-header">
                        <ButtonGroup>
                            <DropdownMenu
                                trigger={<span className="calendar-title">{calendar && calendar.selectedName}</span>}
                                triggerType="button"
                                triggerButtonProps={{
                                    appearance: 'subtle',
                                    iconAfter: <ChevronDownIcon label=""/>
                                }}

                                onOpenChange={this._onCalendarListOpen}

                                isLoading={!calendars}
                            >
                                {calendars &&
                                    <DropdownItemGroupRadio id="gantt-calendar">
                                        {calendars.map(cal =>
                                            <DropdownItemRadio
                                                href={`#calendar=${cal.id}`}
                                                key={cal.id}
                                                id={cal.id}

                                                isSelected={calendar && parseInt(calendar.id, 10) === cal.id}
                                            >
                                                {cal.name}
                                            </DropdownItemRadio>
                                        )}
                                    </DropdownItemGroupRadio>
                                }
                            </DropdownMenu>
                            {!!sprints.length &&
                                <DropdownMenu
                                    trigger={<span className="calendar-title">{(currentSprint && currentSprint.name) || 'Спринт'}</span>}
                                    triggerType="button"
                                    triggerButtonProps={{
                                        appearance: 'subtle',
                                        iconAfter: <ChevronDownIcon label=""/>
                                    }}
                                >
                                    <DropdownItemGroupRadio id="gantt-sprint">
                                        <DropdownItemRadio
                                            key="null"
                                            id="null"

                                            onClick={this._selectSprint(null)}

                                            isSelected={!currentSprint}
                                        >
                                            Не выбран
                                        </DropdownItemRadio>
                                        {sprints.map(sprintItem =>
                                            <DropdownItemRadio
                                                key={sprintItem.id}
                                                id={sprintItem.id}

                                                onClick={this._selectSprint(sprintItem.id)}

                                                isSelected={currentSprint && (currentSprint.id === sprintItem.id)}
                                            >
                                                <SprintState state={sprintItem.state}/>
                                                {' '}
                                                <strong>
                                                    {sprintItem.boardName}
                                                </strong>
                                                {' - '}
                                                {sprintItem.name}
                                            </DropdownItemRadio>
                                        )}
                                    </DropdownItemGroupRadio>
                                </DropdownMenu>
                            }
                        </ButtonGroup>
                        <div className="flex-grow"/>
                        <ButtonGroup appearance="subtle">
                            <Tooltip content="Развернуть структуру">
                                <Button
                                    iconBefore={<VidFullScreenOnIcon label="Expand"/>}
                                    onClick={this._expandStructure}
                                />
                            </Tooltip>
                            <Tooltip content="Свернуть структуру">
                                <Button
                                    iconBefore={<VidFullScreenOffIcon label="Collapse"/>}
                                    onClick={this._collapseStructure}
                                />
                            </Tooltip>
                            <InlineDialog
                                position="bottom right"
                                isOpen={activeDialog === 'params'}
                                content={(activeDialog === 'params') && <OptionsDialog gantt={gantt} onClose={this._toggleDialog('params')}/>}
                            >
                                <Button
                                    appearance="subtle"
                                    iconBefore={<FilterIcon label=""/>}
                                    onClick={this._toggleDialog('params')}
                                />
                            </InlineDialog>
                            <InlineDialog
                                content={
                                    <div className="flex-column">
                                        <FieldTextStateless isLabelHidden label="" value={filter} onChange={this._setFilter}/>
                                        <div style={{marginTop: '20px'}}>
                                            <Button onClick={this._applyFilter} shouldFitContainer>
                                                Применить
                                            </Button>
                                        </div>
                                    </div>
                                }
                                position="bottom right"
                                isOpen={activeDialog === 'filter'}
                                onClose={this._toggleDialog('filter')}
                            >
                                <Button
                                    appearance="subtle"
                                    iconBefore={<SearchIcon label=""/>}
                                    onClick={this._toggleDialog('filter')}
                                />
                            </InlineDialog>
                        </ButtonGroup>
                    </div>
                </div>
            </div>
        );
    }
}

export const GanttActions =
    connect(
        state => {
            return {
                options: state.options,
                calendar: state.calendar,
                sprints: state.sprints
            };
        },
        OptionsActionCreators
    )(GanttActionsInternal);
