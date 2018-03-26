import React from 'react';
import PropTypes from 'prop-types';

import {connect} from 'react-redux';

import memoize from 'lodash.memoize';

import {FieldTextStateless} from '@atlaskit/field-text';
import Button, {ButtonGroup} from '@atlaskit/button';
import InlineDialog from '@atlaskit/inline-dialog';
import DropdownMenu, { DropdownItemGroupRadio, DropdownItemRadio } from '@atlaskit/dropdown-menu';

import ChevronDownIcon from '@atlaskit/icon/glyph/chevron-down';
import MediaServicesZoomInIcon from '@atlaskit/icon/glyph/media-services/zoom-in';
import MediaServicesZoomOutIcon from '@atlaskit/icon/glyph/media-services/zoom-out';
import PreferencesIcon from '@atlaskit/icon/glyph/preferences';
import SearchIcon from '@atlaskit/icon/glyph/search';
import ListIcon from '@atlaskit/icon/glyph/list';
import JiraLabsIcon from '@atlaskit/icon/glyph/jira/labs';
import FilterIcon from '@atlaskit/icon/glyph/filter';

import {keyedConfigs, scaleConfigs} from './scaleConfigs';
import {OptionsDialog} from './OptionsDialog';
import {viewItems} from './views';
import {MagicDialog} from './MagicDialog';

import {OptionsActionCreators} from '../service/gantt.reducer';
import {calendarService} from '../service/services';


const enableMagic = true;

class GanttActionsInternal extends React.Component {
    static propTypes = {
        gantt: PropTypes.object.isRequired,
        options: PropTypes.object.isRequired,
        calendar: PropTypes.object
    };

    state ={
        activeDialog: null,
        waitingForMagic: false,
        calendars: null,
        filter: ''
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

        const project = gantt.getSubtaskDates(),
            areaWidth = gantt.$task.offsetWidth;

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

        let start = new Date(from),
            end = new Date(to);
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

    render() {
        const {activeDialog, waitingForMagic, calendars, filter} = this.state;
        const {options, calendar, gantt} = this.props;

        return (
            <div className="gantt-actions">
                {/*<PageHeader>
                    {calendar && i18n.calendarTitle(calendar.selectedName)}
                </PageHeader>*/}
                <div className="flex-row">
                    <div>
                        <ButtonGroup>
                            {enableMagic && <Button
                                iconBefore={<JiraLabsIcon label=""/>}
                                isDisabled={waitingForMagic}

                                onClick={this._toggleDialog('magic')}
                            >
                                Запустить магию
                            </Button>}
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
                        {activeDialog === 'params' && <OptionsDialog gantt={gantt} onClose={this._toggleDialog('params')}/>}
                    </div>
                    <div>
                        <ButtonGroup>
                            <Button
                                iconBefore={<PreferencesIcon/>}
                                onClick={this._toggleDialog('params')}
                            >
                                Параметры
                            </Button>
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
                    <div>
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
                    </div>
                    <div className="flex-grow"/>
                    <ButtonGroup>
                        <Button appearance="subtle" iconBefore={<FilterIcon label=""/>}/>
                        <InlineDialog
                            content={
                                <div className="flex-column">
                                    <FieldTextStateless isLabelHidden={true} label="" value={filter} onChange={this._setFilter}/>
                                    <div style={{marginTop: '20px'}}>
                                        <Button onClick={this._applyFilter} shouldFitContainer={true}>
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
        );
    }
}

export const GanttActions =
    connect(
        state => {
            return {
                options: state.options,
                calendar: state.calendar
            };
        },
        OptionsActionCreators
    )(GanttActionsInternal);
