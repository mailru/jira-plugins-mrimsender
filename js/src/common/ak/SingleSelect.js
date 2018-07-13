// @flow
import React from 'react';
import { Label } from '@atlaskit/field-base';
import Select from '@atlaskit/select';

import type { SelectOption } from '../types';


type Props = {
    label: string,
    isClearable?: bool,
    isDisabled?: bool,
    isRequired?: bool,
    isLabelHidden?: bool,
    value?: SelectOption | null,
    onChange?: (SelectOption, {}) => void,
    options: $ReadOnlyArray<SelectOption>
};

export class SingleSelect extends React.Component<Props> {

    render() {
        const { label, isRequired, isLabelHidden, options, value, onChange, ...props } = this.props;

        return (
            <div>
                <Label label={label} isRequired={isRequired} isHidden={isLabelHidden} />
                <Select value={value} onChange={onChange} options={options} {...props} />
            </div>
        );
    }
}
