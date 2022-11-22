import React, { ReactElement, useLayoutEffect, useState } from 'react';
import { AsyncSelect, OptionsType, OptionType } from '@atlaskit/select';
import { I18n } from '@atlassian/wrm-react-i18n';
import { loadJiraUsers } from '../../shared/api/CommonApiClient';
import { User } from '../../shared/types';

type Props = {
  selectedValue?: OptionsType;
  id: string;
  onChange: (value: OptionsType) => void;
  isMulti?: boolean;
  placeholder?: string;
};

export const createUserOption = (user: User): OptionType => {
  return {
    label: user.displayName,
    value: user.userKey,
  };
};

const loadUsers = async (query?: string): Promise<OptionType[]> => {
  return new Promise((resolve, reject) => {
    loadJiraUsers(query === undefined ? '' : query)
      .then((response) => {
        resolve(response.data.map((user) => createUserOption(user)));
      })
      .catch(reject);
  });
};

function UsersSelect({
  id,
  onChange,
  selectedValue,
  placeholder,
}: Props): ReactElement {
  const [currentValue, setCurrentValue] = useState<
    OptionsType | OptionType | null
  >();

  const handleChange = (value: OptionsType): void => {
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
      loadOptions={loadUsers}
      noOptionsMessage={() => I18n.getText('common.concepts.no.matches')}
      placeholder={placeholder}
      isMulti
      menuPortalTarget={document.body}
      styles={{ menuPortal: (base) => ({ ...base, zIndex: 9999 }) }}
    />
  );
}

UsersSelect.defaultProps = {
  selectedValue: undefined,
  placeholder: I18n.getText('admin.common.words.users'),
};

export default UsersSelect;
