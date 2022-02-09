import React, { ReactElement, useLayoutEffect, useState } from 'react';
import contextPath from 'wrm/context-path';
import { AsyncSelect, OptionType } from '@atlaskit/select';

type IssueTypeData = { name: string; id: string };

type Props = {
  id: string;
  className?: string;
  projectKey: string;
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
  const [projects, setProjects] = useState<Array<OptionType>>([]);
  const [defaultValue, setDefaultValue] = useState<OptionType>();

  useLayoutEffect(() => {
    loadProjectOptions(projectKey).then(({ issueTypes }) => {
      const options = mapIssueTypesToOptions(issueTypes);
      setProjects(options);

      const selectedValues = options.filter((o) => o.value == defaultIssueTypeId);
      console.log(selectedValues);

      if (selectedValues.length) {
        setDefaultValue(selectedValues[0]);
      }
    });
  }, [defaultIssueTypeId]);

  return (
    <AsyncSelect
      className={className}
      onChange={onChange}
      inputId={id}
      cacheOptions
      defaultValue={defaultValue}
      defaultOptions={projects}
      loadOptions={filterOptions(projects)}
    />
  );
};

export default IssueTypeSelect;
