import React, { Component } from 'react';
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

type State = {
  isLoading: boolean;
  options: Array<Option>;
  value?: Array<ValueType<OptionType>>;
};

type Props = {
  onChange: (value: Option) => void;
};

export default class LabelsSelect extends Component<Props, State> {
  state: State = {
    isLoading: false,
    options: defaultOptions,
    value: undefined,
  };

  handleChange = (newValue: any, actionMeta: any) => {
    this.setState({ value: newValue });
    this.props.onChange(newValue);
  };

  handleCreate = (inputValue: any) => {
    this.setState({ isLoading: true });

    const { options } = this.state;
    const newOption = createOption(inputValue);

    const newOptions = options.slice();
    newOptions.push(newOption);

    const newValue = this.state.value ? this.state.value.slice() : [];

    newValue.push(newOption);
    this.setState({
      isLoading: false,
      options: newOptions,
      value: newValue,
    });
  };

  render() {
    const { isLoading, options, value } = this.state;
    return (
      <CreatableSelect
        inputId="createable-select-example"
        isClearable
        isMulti
        isDisabled={isLoading}
        isLoading={isLoading}
        onChange={this.handleChange}
        onCreateOption={this.handleCreate}
        options={options}
        value={value}
      />
    );
  }
}
