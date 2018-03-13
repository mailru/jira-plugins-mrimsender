import React from 'react';
import PropTypes from 'prop-types';

import {connect} from 'react-redux';

import Button, {ButtonGroup} from '@atlaskit/button';
import PageHeader from '@atlaskit/page-header';
import InlineDialog from '@atlaskit/inline-dialog';
import {DatePicker} from '@atlaskit/datetime-picker';
import {Label} from '@atlaskit/field-base';

import MediaServicesZoomInIcon from '@atlaskit/icon/glyph/media-services/zoom-in';
import MediaServicesZoomOutIcon from '@atlaskit/icon/glyph/media-services/zoom-out';
import ChevronDownIcon from '@atlaskit/icon/glyph/chevron-down';
import PreferencesIcon from '@atlaskit/icon/glyph/preferences';
import SearchIcon from '@atlaskit/icon/glyph/search';
import ListIcon from '@atlaskit/icon/glyph/list';
import JiraLabsIcon from '@atlaskit/icon/glyph/jira/labs';
import ArrowDownIcon from '@atlaskit/icon/glyph/arrow-down';
import ArrowUpIcon from '@atlaskit/icon/glyph/arrow-up';

// eslint-disable-next-line import/no-extraneous-dependencies
import i18n from 'gantt-i18n';

import {keyedConfigs, scaleConfigs} from './scaleConfigs';

import {OptionsActionCreators} from '../service/gantt.reducer';
import {calendarService, ganttService} from '../service/services';
import {SingleSelect} from '../common/ak/SingleSelect';


const groupOptions = [
    {
        value: 'assignee',
        label: 'По исполнителю'
    }
];

class GanttActionsInternal extends React.Component {
    static propTypes = {
        gantt: PropTypes.object.isRequired,
        options: PropTypes.object.isRequired,
        calendar: PropTypes.object
    };

    state ={
        showDateDialog: false,
        waitingForMagic: false,
        fields: []
    };

    componentDidMount() {
        calendarService
            .getFields()
            .then(fields =>
                this.setState({
                    fields: fields
                        .filter(field => field.navigable && field.clauseNames && field.clauseNames.length)
                        .map(field => {
                            return {
                                label: field.name,
                                value: field.id
                            };
                        })
                })
            );
    }

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

    _setDate = (field) => (value) => this.props.updateOptions({ [field]: value });

    _setGroup = (value) => this.props.updateOptions({ 'groupBy': value ? value.value : null });

    _setOrder = (value) => this.props.updateOptions({ 'orderBy': value ? value.value : null });

    _toggleGrid = () => this.props.updateOptions({ showGrid: !this.props.options.showGrid });

    _toggleOrder = () => this.props.updateOptions({ order: !this.props.options.order });

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
        const {showDateDialog, waitingForMagic, fields} = this.state;
        const {options, calendar} = this.props;
        const {showGrid} = options;

        let datePickers = null;

        if (showDateDialog) {
            datePickers = (
                <div style={{ width: '250px' }} className="flex-column">
                    <Label label="Дата начала" isFirstChild={true}/>
                    <DatePicker value={options.startDate} onChange={this._setDate('startDate')}/>
                    <Label label="Дата конца"/>
                    <DatePicker value={options.endDate} onChange={this._setDate('endDate')}/>
                    <SingleSelect
                        label="Группировка"
                        isClearable={true}
                        options={groupOptions}

                        value={options.groupBy ? groupOptions.find(val => val.value === options.groupBy) : null}
                        onChange={this._setGroup}
                    />
                    <div className="flex-row">
                        <div className="flex-grow">
                            <SingleSelect
                                label="Сортировать по"
                                isClearable={true}
                                options={fields}

                                value={options.orderBy ? fields.find(val => val.value === options.orderBy) : null}
                                onChange={this._setOrder}
                            />
                        </div>
                        <div className="flex-none" style={{paddingTop: '45px', marginLeft: '5px'}}>
                            <Button
                                iconBefore={options.order ? <ArrowDownIcon label=""/> : <ArrowUpIcon label=""/>}
                                onClick={this._toggleOrder}
                            />
                        </div>
                    </div>
                </div>
            );
        }

        return (
            <div>
                <PageHeader>
                    {calendar && i18n.calendarTitle(calendar.selectedName)}
                </PageHeader>
                <div className="flex-row">
                    <div>
                        <ButtonGroup>
                            <Button
                                iconBefore={<ListIcon label=""/>}
                                isSelected={showGrid}

                                onClick={this._toggleGrid}
                            >
                                Список задач
                            </Button>
                            {/*TODO: <Button
                                iconBefore={<JiraLabsIcon label=""/>}
                                isDisabled={waitingForMagic}

                                onClick={this._runMagic}
                            >
                                Запустить магию
                            </Button>*/}
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
                            <InlineDialog
                                content={datePickers}
                                isOpen={showDateDialog}
                                onClose={this._toggleDateDialog}
                            >
                                <Button
                                    iconBefore={<PreferencesIcon/>}
                                    iconAfter={<ChevronDownIcon/>}
                                    onClick={this._toggleDateDialog}
                                    isSelected={showDateDialog}
                                >
                                    Параметры
                                </Button>
                            </InlineDialog>
                        </ButtonGroup>
                    </div>
                    <div>
                        <ButtonGroup>
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
