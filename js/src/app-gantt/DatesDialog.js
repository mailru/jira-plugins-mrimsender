/* eslint-disable flowtype/require-valid-file-annotation */
import React from 'react';
import PropTypes from 'prop-types';
import {connect} from 'react-redux';

// eslint-disable-next-line import/no-extraneous-dependencies
import i18n from 'i18n';

import Button, {ButtonGroup} from '@atlaskit/button';
import {DatePicker} from '@atlaskit/datetime-picker';
import {Label} from '@atlaskit/field-base';

import {OptionsActionCreators} from '../service/gantt.reducer';


class DatesDialogInternal extends React.Component {
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
                <Label label={i18n['ru.mail.jira.plugins.calendar.gantt.period.startDate']} isFirstChild/>
                <DatePicker value={options.startDate} onChange={this._setDate('startDate')}/>
                <Label label={i18n['ru.mail.jira.plugins.calendar.gantt.period.endDate']}/>
                <DatePicker value={options.endDate} onChange={this._setDate('endDate')}/>
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

export const DatesDialog =
    connect(
        state => {
            return {
                options: state.options,
            };
        },
        OptionsActionCreators
    )(DatesDialogInternal);
