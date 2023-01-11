import React, { ReactElement, useLayoutEffect, useState } from 'react';
import { AsyncSelect, OptionsType, OptionType } from '@atlaskit/select';
import { I18n } from '@atlassian/wrm-react-i18n';
import { ProjectRole } from '../../shared/types';
import { loadProjectRoles } from '../../shared/api/AccessRequestApiClient';

type Props = {
  projectKey: string;
  selectedValue?: OptionsType;
  id: string;
  onChange: (value: OptionsType) => void;
};

export const createProjectRoleOption = (role: ProjectRole): OptionType => {
  return {
    label: role.name,
    value: role.id,
  };
};

const loadRoles = async (
  projectKey: string,
  query?: string,
): Promise<OptionType[]> => {
  return new Promise((resolve, reject) => {
    loadProjectRoles(projectKey, query === undefined ? '' : query)
      .then((response) => {
        resolve(response.data.map(createProjectRoleOption));
      })
      .catch(reject);
  });
};

function ProjectRolesSelect({
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
      loadOptions={(query?: string) => loadRoles(projectKey, query)}
      noOptionsMessage={() => I18n.getText('common.concepts.no.matches')}
      isMulti
      menuPortalTarget={document.body}
      styles={{ menuPortal: (base) => ({ ...base, zIndex: 9999 }) }}
    />
  );
}

ProjectRolesSelect.defaultProps = {
  selectedValue: undefined,
};

export default ProjectRolesSelect;
