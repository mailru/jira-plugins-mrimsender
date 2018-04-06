import React from 'react';
import PropTypes from 'prop-types';

import Lozenge from '@atlaskit/lozenge';


export function SprintState({state}) {
    let appearance = null;

    // eslint-disable-next-line default-case
    switch (state) {
        case 'FUTURE':
            appearance = 'new';
            break;
        case 'ACTIVE':
            appearance = 'inprogress';
            break;
        case 'CLOSED':
            appearance = 'default';
            break;
    }
    return <Lozenge appearance={appearance}>{state}</Lozenge>;
}

SprintState.propTypes = {
    state: PropTypes.string.isRequired
};
