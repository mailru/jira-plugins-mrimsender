import React from 'react';
import PropTypes from 'prop-types';

import {connect} from 'react-redux';

import Button, {ButtonGroup} from '@atlaskit/button';
import PageHeader from '@atlaskit/page-header';
import DropdownMenu, { DropdownItemGroupRadio, DropdownItemRadio } from '@atlaskit/dropdown-menu';

import MediaServicesZoomInIcon from '@atlaskit/icon/glyph/media-services/zoom-in';
import MediaServicesZoomOutIcon from '@atlaskit/icon/glyph/media-services/zoom-out';
import PreferencesIcon from '@atlaskit/icon/glyph/preferences';
import SearchIcon from '@atlaskit/icon/glyph/search';
import ListIcon from '@atlaskit/icon/glyph/list';
import JiraLabsIcon from '@atlaskit/icon/glyph/jira/labs';

// eslint-disable-next-line import/no-extraneous-dependencies
import i18n from 'gantt-i18n';

import {keyedConfigs, scaleConfigs} from './scaleConfigs';
import {OptionsDialog} from './OptionsDialog';
import {viewItems} from './views';

import {OptionsActionCreators} from '../service/gantt.reducer';
import {ganttService} from '../service/services';


const enableMagic = true;

class GanttActionsInternal extends React.Component {
    static propTypes = {
        gantt: PropTypes.object.isRequired,
        options: PropTypes.object.isRequired,
        calendar: PropTypes.object
    };

    state ={
        showDateDialog: false,
        waitingForMagic: false,
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

    _toggleDateDialog = () => this.setState(state => {
        return {
            showDateDialog: !state.showDateDialog
        };
    });

    _setScale = (scale) => () => this.props.updateOptions({ scale });

    _setView = (view) => () => this.props.updateOptions({ view });

    _runMagic = () => {
        this.setState({ waitingForMagic: true });
        ganttService
            .getOptimized(this.props.calendar.id)
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
                },
                error => {
                    this.setState({ waitingForMagic: false });
                    throw error;
                }
            );
    };

    render() {
        const {showDateDialog, waitingForMagic} = this.state;
        const {options, calendar, gantt} = this.props;

        return (
            <div>
                <PageHeader>
                    {calendar && i18n.calendarTitle(calendar.selectedName)}
                </PageHeader>
                <div className="flex-row">
                    <div>
                        <ButtonGroup>
                            {enableMagic && <Button
                                iconBefore={<JiraLabsIcon label=""/>}
                                isDisabled={waitingForMagic}

                                onClick={this._runMagic}
                            >
                                Запустить магию
                            </Button>}
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
                        {showDateDialog && <OptionsDialog gantt={gantt} onClose={this._toggleDateDialog}/>}
                    </div>
                    <div>
                        <ButtonGroup>
                            <Button
                                iconBefore={<PreferencesIcon/>}
                                onClick={this._toggleDateDialog}
                                isSelected={showDateDialog}
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
