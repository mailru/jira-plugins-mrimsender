import React from 'react';
import PropTypes from 'prop-types';

import SingleLineTextInput from '@atlaskit/input';
import {InlineEditStateless} from '@atlaskit/inline-edit';


export class InlineEditSingleLineTextInput extends React.Component {
    static propTypes = {
        label: PropTypes.string,
        isLabelHidden: PropTypes.bool,
        isFitContainerWidthReadView: PropTypes.bool,
        id: PropTypes.any,
        value: PropTypes.any,
        viewClassNames: PropTypes.string,
        onConfirm: PropTypes.func,
    };

    state = {
        isEditing: false,
        editValue: this.props.value || '',
        readValue: this.props.value || '',
        hasError: false,
        errorMessage: null
    };

    _enterEditingMode = () => {
        this.setState({ isEditing: true });
    };

    _exitEditingMode = () => {
        this.setState({ isEditing: false });
    };

    _onChange = (event) => {
        this.setState({
            editValue: event.target.value,
        });
    };


    _renderInput = (isEditing, id) => (
        <SingleLineTextInput
            id={id}
            isEditing={isEditing}
            isInitiallySelected={true}
            label=""
            value={this.state.editValue}
            onChange={this._onChange}
        />
    );

    _onConfirm = () => {
        this.setState({ hasError: false, errorMessage: null });
        if (this.props.onConfirm && this.props.onConfirm instanceof Function) {
            this.props.onConfirm(this.state.editValue)
                .then(() => {
                    this.setState({readValue: this.state.editValue});
                    this._exitEditingMode();
                    },
                    error => {
                        if (error.response.data.hasOwnProperty('errors')) {
                            this.setState({ hasError: true, errorMessage: error.response.data.errors.field });
                        }
                    }

                )
        }
    };

    _onCancel = () => {
        this.setState({editValue: this.state.readValue, hasError: false, errorMessage: null});
        this._exitEditingMode();
    };

    render() {
        const {label, isLabelHidden, isFitContainerWidthReadView, id, viewClassNames} = this.props;
        const {isEditing, hasError, errorMessage} = this.state;

        return (
            <InlineEditStateless
                label={label}
                isLabelHidden={isLabelHidden}
                isFitContainerWidthReadView={isFitContainerWidthReadView}
                editView={this._renderInput(true, id, this.state.editValue)}
                readView={<span className={viewClassNames}>{this.state.readValue}</span>}
                onConfirm={this._onConfirm}
                shouldConfirmOnEnterboolean={true}
                onCancel={this._onCancel}
                isInvalid={hasError}
                invalidMessage={errorMessage}
                onEditRequested={this._enterEditingMode}
                isEditing={isEditing}
            />
        );
    }
}
