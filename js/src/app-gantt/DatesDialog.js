import React from 'react';
import PropTypes from 'prop-types';
import {connect} from 'react-redux';

import Button, {ButtonGroup} from '@atlaskit/button';
import {DatePicker} from '@atlaskit/datetime-picker';
import {Label} from '@atlaskit/field-base';

import {OptionsActionCreators} from '../service/gantt.reducer';


class DatesDialogInternal extends React.Component {
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

    _saveOptions = () => {
        const {onClose, updateOptions} = this.props;

        onClose();
        updateOptions(this.state.options);
    };

    constructor(props) {
        super(props);

        this.state = {
            options: props.options
        };
    }

    render() {
        const {options} = this.state;
        const {onClose} = this.props;

        return (
            <div className="flex-column">
                <Label label="Дата начала" isFirstChild={true}/>
                <DatePicker value={options.startDate} onChange={this._setDate('startDate')}/>
                <Label label="Дата конца"/>
                <DatePicker value={options.endDate} onChange={this._setDate('endDate')}/>
                <div className="ak-field-margin">
                    <ButtonGroup>
                        <Button
                            appearance="primary"
                            onClick={this._saveOptions}
                        >
                            Применить
                        </Button>
                        <Button
                            appearance="link"
                            onClick={onClose}
                        >
                            Отменить
                        </Button>
                    </ButtonGroup>
                </div>
            </div>
        );
    }
}

export const DatesDialog =
    connect(
        state => {
            return {
                options: state.options,
            };
        },
        OptionsActionCreators
    )(DatesDialogInternal);
