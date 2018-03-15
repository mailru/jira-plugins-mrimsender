import React from 'react';
import PropTypes from 'prop-types';
import {connect} from 'react-redux';

import Modal from '@atlaskit/modal-dialog';
import Button from '@atlaskit/button';
import {DatePicker} from '@atlaskit/datetime-picker';
import {Label} from '@atlaskit/field-base';

import ArrowDownIcon from '@atlaskit/icon/glyph/arrow-down';
import ArrowUpIcon from '@atlaskit/icon/glyph/arrow-up';

import {OptionsActionCreators} from '../service/gantt.reducer';
import {SingleSelect} from '../common/ak/SingleSelect';
import {calendarService} from '../service/services';


const groupOptions = [
    {
        value: 'assignee',
        label: 'По исполнителю'
    }
];

const columnSizes = {
    timeoriginalestimate: '53px',
    assignee: '200px'
};

class OptionsDialogInternal extends React.Component {
    static propTypes = {
        gantt: PropTypes.object.isRequired,
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

    _setDate = (field) => (value) => this._updateOptions({ [field]: value });

    _setGroup = (value) => this._updateOptions({ groupBy: value ? value.value : null });

    _setOrder = (value) => this._updateOptions({ orderBy: value ? value.value : null });

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
                                value: field.id
                            };
                        })
                })
            );
    }

    render() {
        const {options, fields} = this.state;
        const {onClose} = this.props;

        return (
            <Modal
                heading="Параметры"
                scrollBehavior="outside"
                actions={[
                    {
                        text: 'Сохранить',
                        onClick: this._saveOptions
                    },
                    {
                        text: 'Отменить',
                        onClick: onClose
                    }
                ]}
                onClose={onClose}
            >
                <div className="flex-column">
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
                    <SingleSelect
                        label="Дополнительные поля"
                        options={fields}

                        isMulti={true}

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
                </div>
            </Modal>
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
