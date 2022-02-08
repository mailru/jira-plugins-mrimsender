import React, { Fragment, ReactElement, useLayoutEffect, useState } from 'react';
import contextPath from 'wrm/context-path';
import { AsyncSelect, OptionType } from '@atlaskit/select';

type IssueTypeData = { name: string; id: string };

type Props = {
  title?: string;
  projectKey: string;
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

const ProjectSelect = ({ projectKey, title, onChange }: Props): ReactElement => {
  const [projects, setProjects] = useState<Array<OptionType>>([]);

  useLayoutEffect(() => {
    loadProjectOptions(projectKey).then(({ issueTypes }) => {
      setProjects(mapIssueTypesToOptions(issueTypes));
    });
  }, []);

  return (
    <Fragment>
      <label htmlFor="issuetype-select">{title}</label>
      <AsyncSelect
        onChange={onChange}
        inputId="issuetype-select"
        cacheOptions
        defaultOptions={projects}
        loadOptions={filterOptions(projects)}
      />
    </Fragment>
  );
};

export default ProjectSelect;
