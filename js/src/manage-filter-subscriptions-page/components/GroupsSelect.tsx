import React, { ReactElement, useLayoutEffect, useState } from 'react';
import { AsyncSelect, OptionsType, OptionType } from '@atlaskit/select';
import { I18n } from '@atlassian/wrm-react-i18n';
import { loadJiraGroups } from '../../shared/api/CommonApiClient';

type Props = {
  id: string;
  selectedValue?: OptionsType;
  onChange: (value: OptionsType) => void;
};

export const createGroupOption = (group: string): OptionType => {
  return {
    label: group,
    value: group,
  };
};

const loadGroups = async (query?: string): Promise<OptionType[]> => {
  return new Promise((resolve, reject) => {
    loadJiraGroups(query === undefined ? '' : query)
      .then((response) => {
        resolve(response.data.map((group) => createGroupOption(group.name)));
      })
      .catch(reject);
  });
};

function GroupsSelect({ id, onChange, selectedValue }: Props): ReactElement {
  const [currentValue, setCurrentValue] = useState<OptionsType>();

  const handleChange = (value: OptionsType) => {
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
      cacheOptions
      loadOptions={loadGroups}
      noOptionsMessage={() => I18n.getText('common.concepts.no.matches')}
      placeholder={I18n.getText('common.words.groups')}
      isMulti
      menuPortalTarget={document.body}
      styles={{ menuPortal: (base) => ({ ...base, zIndex: 9999 }) }}
    />
  );
}

GroupsSelect.defaultProps = {
  selectedValue: undefined,
};

export default GroupsSelect;
