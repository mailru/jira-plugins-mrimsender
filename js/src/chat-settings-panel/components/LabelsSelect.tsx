import React, { ReactElement, useLayoutEffect, useState } from 'react';
import { CreatableSelect, OptionsType, OptionType } from '@atlaskit/select';

const defaultOptions = [
  { label: 'autocreated', value: 'autocreated' },
  { label: 'bot', value: 'bot' },
  { label: 'vkteams', value: 'vkteams' },
];

const createOption = (label: string) => ({
  label,
  value: label.toLowerCase().replace(/\W/g, ''),
});

type Props = {
  className?: string;
  defaultLabels?: Array<string>;
  onChange: (value: OptionsType<OptionType>) => void;
};

const LabelsSelect = ({ className, defaultLabels, onChange }: Props): ReactElement => {
  const [value, setValue] = useState<OptionsType<OptionType>>();
  const [options, setOptions] = useState<OptionsType<OptionType>>(defaultOptions);

  const handleChange = (newValue: OptionsType<OptionType>) => {
    setValue(newValue);
    onChange(newValue);
  };

  const handleCreate = (inputValue: string) => {
    const newOption = createOption(inputValue);

    const newOptions = options.slice();
    newOptions.push(newOption);

    const newValue = value ? value.slice() : [];

    newValue.push(newOption);
    setValue(value);
    setOptions(options);
  };

  useLayoutEffect(() => {
    if (defaultLabels) {
      const newValues = new Set<string>(defaultLabels);

      setValue(Array.from(newValues).map(createOption));

      options.forEach((l) => !newValues.has(String(l.value)) && newValues.add(String(l.value)));

      setOptions(Array.from(newValues).map(createOption));
    }
  }, [defaultLabels]);

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
