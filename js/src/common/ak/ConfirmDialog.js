/* eslint-disable flowtype/require-valid-file-annotation */
import React from 'react';
import PropTypes from 'prop-types';

import {connect} from 'react-redux';

import Modal from '@atlaskit/modal-dialog';
import Spinner from '@atlaskit/spinner';

import {noop} from '../util';


class ConfirmDialogInternal extends React.Component {
    static propTypes = {
        header: PropTypes.string.isRequired,
        onConfirm: PropTypes.func.isRequired,
        onClose: PropTypes.func.isRequired,
        children: PropTypes.oneOfType([
            PropTypes.arrayOf(PropTypes.node),
            PropTypes.node
        ])
    };

    state = {
        waiting: false
    };

    _onConfirm = () => {
        this.setState({ waiting: true });
        this.props.onConfirm();
        this.setState({ waiting: false });
        this.props.onClose();
    };

    render() {
        const {header, onClose} = this.props;
        const {waiting} = this.state;

        const actions = [
            {
                text: !waiting && 'Подтвердить',
                onClick: this._onConfirm,
                iconBefore: waiting && <Spinner/>,
                isDisabled: waiting
            },
            {
                text: 'Отмена',
                onClick: onClose,
                isDisabled: waiting
            }
        ];

        return (
            <Modal
                heading={header}
                scrollBehavior="outside"

                actions={actions}
                onClose={waiting ? noop : onClose}
                width="small"
            >
                <div className="flex-column full-width">
                    {this.props.children}
                </div>
            </Modal>
        );
    }
}

export const ConfirmDialog =
    connect(
        state => {
            return {
                calendar: state.calendar,
                teams: state.teams
            };
        }
    )(ConfirmDialogInternal);
