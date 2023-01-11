import React, { ReactElement, useLayoutEffect, useState } from 'react';
import { AsyncSelect, OptionsType, OptionType } from '@atlaskit/select';
import { I18n } from '@atlassian/wrm-react-i18n';
import { Field, ProjectRole } from '../../shared/types';
import { loadUserFields } from '../../shared/api/AccessRequestApiClient';

type Props = {
  projectKey: string;
  selectedValue?: OptionsType;
  id: string;
  onChange: (value: OptionsType) => void;
};

export const createUserFieldOption = (role: Field): OptionType => {
  return {
    label: role.name,
    value: role.id,
  };
};

const loadFields = async (
  projectKey: string,
  query?: string,
): Promise<OptionType[]> => {
  return new Promise((resolve, reject) => {
    loadUserFields(projectKey, query === undefined ? '' : query)
      .then((response) => {
        resolve(response.data.map(createUserFieldOption));
      })
      .catch(reject);
  });
};

function UserFieldsSelect({
  id,
  onChange,
  selectedValue,
  projectKey,
}: Props): ReactElement {
  const [currentValue, setCurrentValue] = useState<OptionsType | undefined>(
    selectedValue,
  );

  const handleChange = (value: OptionsType): void => {
    setCurrentValue(value);
    onChange(value);
  };

  return (
    <AsyncSelect
      defaultOptions
      onChange={handleChange}
      inputId={id}
      value={currentValue}
      cacheOptions
      loadOptions={(query?: string) => loadFields(projectKey, query)}
      noOptionsMessage={() => I18n.getText('common.concepts.no.matches')}
      isMulti
      menuPortalTarget={document.body}
      styles={{ menuPortal: (base) => ({ ...base, zIndex: 9999 }) }}
    />
  );
}

UserFieldsSelect.defaultProps = {
  selectedValue: undefined,
};

export default UserFieldsSelect;
