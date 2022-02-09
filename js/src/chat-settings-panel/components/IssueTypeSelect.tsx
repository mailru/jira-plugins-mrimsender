import React, { ReactElement, useLayoutEffect, useState } from 'react';
import contextPath from 'wrm/context-path';
import { AsyncSelect, OptionType, SelectProps } from '@atlaskit/select';
import { usePrevious } from '../shared/hooks';

type IssueTypeData = { name: string; id: string };

type Props = SelectProps<OptionType> & {
  className?: string;
  projectKey?: string;
  defaultIssueTypeId?: string;
  onChange: (value: OptionType | null) => void;
};

const loadProjectOptions = async (projectKey: string): Promise<{ issueTypes: ReadonlyArray<IssueTypeData> }> => {
  return $.ajax({
    type: 'GET',
    context: this,
    url: `${contextPath()}/rest/api/2/project/${projectKey}`,
  });
};

const mapIssueTypesToOptions = (projects: ReadonlyArray<IssueTypeData>): Array<OptionType> => {
  return projects.map((issueType) => {
    return { label: issueType.name, value: issueType.id };
  });
};

const filterOptions = (options: Array<OptionType>) => {
  return (inputValue: string) =>
    new Promise(async (resolve) => {
      resolve(
        options.filter(
          (o) =>
            !inputValue ||
            inputValue.length == 0 ||
            o.label.toLocaleLowerCase().indexOf(inputValue.toLocaleLowerCase()) != -1 ||
            String(o.value).toLocaleLowerCase().indexOf(inputValue.toLocaleLowerCase()) != -1,
        ),
      );
    });
};

const IssueTypeSelect = ({ id, projectKey, className, defaultIssueTypeId, onChange }: Props): ReactElement => {
  const [issueTypes, setIssueTypes] = useState<Array<OptionType>>([]);
  const [value, setValue] = useState<OptionType | null>();

  const prevProps = usePrevious<Pick<Props, 'defaultIssueTypeId' | 'projectKey'>>({
    defaultIssueTypeId,
    projectKey,
  });

  const handleChange = (value: OptionType | null) => {
    setValue(value);
    onChange(value);
  };

  useLayoutEffect(() => {
    if (projectKey) {
      loadProjectOptions(projectKey).then(({ issueTypes }) => {
        const options = mapIssueTypesToOptions(issueTypes);
        setIssueTypes(options);

        if (prevProps?.projectKey && prevProps.projectKey != projectKey) {
          // reset value on project change
          handleChange(null);
        } else {
          // on first init
          const selectedValues = options.filter((o) => o.value == defaultIssueTypeId);
          handleChange(selectedValues[0]);
        }
      });
    }
  }, [defaultIssueTypeId, projectKey]);

  return (
    <AsyncSelect
      className={className}
      onChange={handleChange}
      inputId={id}
      cacheOptions
      value={value}
      defaultOptions={issueTypes}
      loadOptions={filterOptions(issueTypes)}
    />
  );
};

export default IssueTypeSelect;
