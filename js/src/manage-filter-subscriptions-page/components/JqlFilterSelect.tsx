import React, { ReactElement, useLayoutEffect, useState } from 'react';
import {
  AsyncSelect,
  FormatOptionLabelMeta,
  OptionType,
} from '@atlaskit/select';
import styled from '@emotion/styled';
import { I18n } from '@atlassian/wrm-react-i18n';
import { loadJqlFilters } from '../../shared/api/CommonApiClient';
import { JqlFilter } from '../../shared/types';

const OptionLabel = styled.div`
  display: flex;
  alight-items: center;
  justify-content: space-between;

  i {
    color: #6b778c;
  }
`;

type Props = {
  id: string;
  selectedValue?: OptionType | null;
  onChange: (value: OptionType | null) => void;
  placeholder?: string;
  isClearable?: boolean;
};

export const createFilterOption = (filter: JqlFilter): OptionType => {
  return {
    label: filter.name,
    value: filter.id,
    owner: filter.owner,
  };
};

const loadFilters = async (query: string): Promise<Array<OptionType>> => {
  return new Promise((resolve, reject) => {
    loadJqlFilters(query)
      .then((response) => {
        resolve(response.data.map((filter) => createFilterOption(filter)));
      })
      .catch(reject);
  });
};

const formatOptionLabel = (
  option: OptionType,
  meta: FormatOptionLabelMeta<OptionType>,
) => {
  if (meta.context === 'value') return option.label;

  return (
    <OptionLabel>
      <div>{option.label}</div>
      {option.owner ? (
        <div>
          <i>{I18n.getText('admin.common.words.owner')}</i>
        </div>
      ) : null}
    </OptionLabel>
  );
};

function JqlFilterSelect({
  id,
  selectedValue,
  onChange,
  placeholder,
  isClearable,
}: Props): ReactElement {
  const [currentValue, setCurrentValue] = useState<OptionType | null>();

  const handleChange = (value: OptionType | null): void => {
    setCurrentValue(value);
    onChange(value);
  };

  useLayoutEffect(() => {
    if (selectedValue) {
      setCurrentValue(selectedValue);
    }
  }, [selectedValue]);

  return (
    <AsyncSelect
      defaultOptions
      onChange={handleChange}
      inputId={id}
      value={currentValue}
      placeholder={placeholder}
      isClearable={isClearable}
      cacheOptions
      loadOptions={loadFilters}
      formatOptionLabel={formatOptionLabel}
      menuPortalTarget={document.body}
      styles={{ menuPortal: (base) => ({ ...base, zIndex: 9999 }) }}
    />
  );
}

JqlFilterSelect.defaultProps = {
  selectedValue: undefined,
  placeholder: undefined,
  isClearable: false,
};

export default JqlFilterSelect;
