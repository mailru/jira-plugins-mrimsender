/* eslint-disable flowtype/require-valid-file-annotation */
import React from 'react';
import PropTypes from 'prop-types';
import {connect} from 'react-redux';

// eslint-disable-next-line import/no-extraneous-dependencies
import i18n from 'i18n';

import Button, {ButtonGroup} from '@atlaskit/button';
import {CheckboxStateless, CheckboxGroup} from '@atlaskit/checkbox';

import ArrowDownIcon from '@atlaskit/icon/glyph/arrow-down';
import ArrowUpIcon from '@atlaskit/icon/glyph/arrow-up';

import {groupOptions} from './staticOptions';

import {OptionsActionCreators} from '../service/gantt.reducer';
import {SingleSelect} from '../common/ak/SingleSelect';
import {calendarService} from '../service/services';


const columnSizes = {
    timeoriginalestimate: '53px',
    assignee: '200px'
};

class OptionsDialogInternal extends React.Component {
    static propTypes = {
        // eslint-disable-next-line react/forbid-prop-types
        // gantt: PropTypes.object.isRequired,
        // eslint-disable-next-line react/forbid-prop-types
        options: PropTypes.object.isRequired,
        onClose: PropTypes.func.isRequired,
        updateOptions: PropTypes.func.isRequired
    };

    _updateOptions = (delta) => this.setState(state => {
        return {
            options: {
                ...state.options,
                ...delta
            }
        };
    });

    _setGroup = (value) => this._updateOptions({ groupBy: value ? value.value : null });

    _setOrder = (value) => this._updateOptions({ orderBy: value ? value.value : null, isOrderedByRank: value && value.isRank });

    _setFields = (value) => this._updateOptions({
        columns: value ?
            value.map(item => {
                return {
                    key: item.value,
                    name: item.label,
                    isJiraField: true //todo: column width
                };
            })
            : null
    });

    _toggleOrder = () => this._updateOptions({ order: !this.state.options.order });

    _toggleUnscheduled = () => this._updateOptions({ withUnscheduled: !this.state.options.withUnscheduled });

    _toggleProgress = () => this._updateOptions({ hideProgress: !this.state.options.hideProgress });

    _saveOptions = () => {
        const {onClose, updateOptions} = this.props;

        onClose();
        updateOptions(this.state.options);
    };

    constructor(props) {
        super(props);

        this.state = {
            options: props.options,
            fields: []
        };
    }

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
                                value: field.id,
                                isRank: field.schema && field.schema.custom === 'com.pyxis.greenhopper.jira:gh-lexo-rank'
                            };
                        })
                })
            );
    }

    render() {
        const {options, fields} = this.state;
        const {onClose} = this.props;

        return (
            <div className="flex-column">
                <SingleSelect
                    label={i18n['ru.mail.jira.plugins.calendar.gantt.params.groupBy']}
                    isClearable
                    options={groupOptions}

                    value={options.groupBy ? groupOptions.find(val => val.value === options.groupBy) : null}
                    onChange={this._setGroup}
                />
                <div className="flex-row">
                    <div className="flex-grow">
                        <SingleSelect
                            label={i18n['ru.mail.jira.plugins.calendar.gantt.params.orderBy']}
                            isClearable
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
                <SingleSelect
                    label={i18n['ru.mail.jira.plugins.calendar.gantt.params.fields']}
                    options={fields}

                    isMulti

                    value={options.columns ? options.columns.map(col => {
                        return {
                            value: col.key,
                            label: col.name,
                            colParams: {
                                width: columnSizes[col.key]
                            }
                        };
                    }) : null}
                    onChange={this._setFields}
                />
                <div className="ak-field-margin">
                    <CheckboxGroup>
                        <CheckboxStateless
                            label={i18n['ru.mail.jira.plugins.calendar.gantt.params.hideProgress']}
                            isChecked={options.hideProgress}
                            onChange={this._toggleProgress}
                        />
                        <CheckboxStateless
                            label={i18n['ru.mail.jira.plugins.calendar.gantt.params.showUnestimated']}
                            isChecked={options.withUnscheduled}
                            onChange={this._toggleUnscheduled}
                        />
                    </CheckboxGroup>
                </div>
                <div className="ak-field-margin">
                    <ButtonGroup>
                        <Button
                            appearance="primary"
                            onClick={this._saveOptions}
                        >
                            {i18n['ru.mail.jira.plugins.calendar.common.apply']}
                        </Button>
                        <Button
                            appearance="link"
                            onClick={onClose}
                        >
                            {i18n['ru.mail.jira.plugins.calendar.common.cancel']}
                        </Button>
                    </ButtonGroup>
                </div>
            </div>
        );
    }
}

export const OptionsDialog =
    connect(
        state => {
            return {
                options: state.options,
            };
        },
        OptionsActionCreators
    )(OptionsDialogInternal);
