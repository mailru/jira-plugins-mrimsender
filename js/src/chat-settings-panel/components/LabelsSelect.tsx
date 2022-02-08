import React, { useState } from 'react';
import { CreatableSelect, OptionType, ValueType } from '@atlaskit/select';

const defaultOptions = [
  { label: 'autocreated', value: 'autocreated' },
  { label: 'bot', value: 'bot' },
  { label: 'vkteams', value: 'vkteams' },
];

const createOption = (label: string) => ({
  label,
  value: label.toLowerCase().replace(/\W/g, ''),
});

type Option = { label: string; value: string };

type Props = {
  className?: string;
  onChange: (value: Option) => void;
};

const LabelsSelect = ({ className, onChange }: Props) => {
  const [value, setValue] = useState<Array<ValueType<OptionType>>>();
  const [options, setOptions] = useState<Array<Option>>(defaultOptions);

  const handleChange = (newValue: any, actionMeta: any) => {
    setValue(newValue);
    onChange(newValue);
  };

  const handleCreate = (inputValue: any) => {
    const newOption = createOption(inputValue);

    const newOptions = options.slice();
    newOptions.push(newOption);

    const newValue = value ? value.slice() : [];

    newValue.push(newOption);
    setValue(value);
    setOptions(options);
  };

  return (
    <CreatableSelect
      className={className}
      inputId="labels-select"
      isClearable
      isMulti
      onChange={handleChange}
      onCreateOption={handleCreate}
      options={options}
      value={value}
    />
  );
};

export default LabelsSelect;
