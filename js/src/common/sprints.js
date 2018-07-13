// @flow
import React, { type ElementProps } from 'react';

import Lozenge from '@atlaskit/lozenge';

type Props = {
    state?: string,
};

type Appearances = $PropertyType<ElementProps<typeof Lozenge>, 'appearance'>;

export const SprintState = ({ state }: Props) => {
    let appearance: ?Appearances;

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
        default:
            appearance = undefined; //default appearance is specified in defaultProps in Lozenge
            break;
    }

    return <Lozenge appearance={appearance}>{state}</Lozenge>;
};
