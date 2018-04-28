// @flow
import React from 'react';

import Lozenge from '@atlaskit/lozenge';
import type { Appearances } from '@atlaskit/lozenge/dist/cjs/Lozenge/index';
import { APPEARANCE_ENUM } from '@atlaskit/lozenge/dist/cjs/Lozenge/index';

type Props = {
    state?: string,
};

export const SprintState = ({ state }: Props) => {
    let appearance: Appearances = APPEARANCE_ENUM.defaultValue;

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
            appearance = APPEARANCE_ENUM.defaultValue;
    }
    return <Lozenge appearance={appearance}>{state}</Lozenge>;
};
