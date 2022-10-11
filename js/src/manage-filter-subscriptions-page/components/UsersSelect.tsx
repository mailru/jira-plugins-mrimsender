import React, { ReactElement, useLayoutEffect, useState } from 'react';
import { AsyncSelect, OptionsType, OptionType } from '@atlaskit/select';
import { I18n } from '@atlassian/wrm-react-i18n';
import { loadJiraUsers } from '../../shared/api/CommonApiClient';
import { User } from '../../shared/types';

type Props = {
  selectedValue?: User[];
  id: string;
  onChange: (value: OptionsType) => void;
};

export const createUserOption = (user: {
  key: string;
  displayName: string;
}): OptionType => {
  return {
    label: user.displayName,
    value: user.key,
  };
};

const loadUsers = async (query?: string): Promise<OptionType[]> => {
  return new Promise((resolve, reject) => {
    loadJiraUsers(query === undefined ? '' : query)
      .then((response) => {
        resolve(response.data.users.map((user) => createUserOption(user)));
      })
      .catch(reject);
  });
};

function UsersSelect({ id, onChange, selectedValue }: Props): ReactElement {
  const [currentValue, setCurrentValue] = useState<OptionsType>();

  const handleChange = (value: OptionsType): void => {
    setCurrentValue(value);
    onChange(value);
  };

  useLayoutEffect(() => {
    if (selectedValue) {
      setCurrentValue(
        selectedValue.map((val) =>
          createUserOption({ key: val.userKey, displayName: val.displayName }),
        ),
      );
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
      placeholder={I18n.getText('admin.common.words.users')}
      isMulti
      menuPortalTarget={document.body}
      styles={{ menuPortal: (base) => ({ ...base, zIndex: 9999 }) }}
    />
  );
}

UsersSelect.defaultProps = {
  selectedValue: undefined,
};

export default UsersSelect;
