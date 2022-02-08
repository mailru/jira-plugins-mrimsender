import React, { Fragment, ReactElement, useLayoutEffect, useState } from 'react';
import contextPath from 'wrm/context-path';
import { AsyncSelect, OptionType } from '@atlaskit/select';

type ProjectData = { name: string; key: string };

type Props = {
  title?: string;
  onChange: (value: OptionType | null) => void;
};

const loadProjectOptions = async (): Promise<ReadonlyArray<ProjectData>> => {
  return $.ajax({
    type: 'GET',
    context: this,
    url: `${contextPath()}/rest/api/2/project`,
  });
};

const mapProjectsToOptions = (projects: ReadonlyArray<ProjectData>): Array<OptionType> => {
  return projects.map((project) => {
    return { label: `${project.name} (${project.key})`, value: project.key };
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

const ProjectSelect = ({ title, onChange }: Props): ReactElement => {
  const [projects, setProjects] = useState<Array<OptionType>>([]);

  useLayoutEffect(() => {
    loadProjectOptions().then((data) => {
      setProjects(mapProjectsToOptions(data));
    });
  }, []);

  return (
    <Fragment>
      <label htmlFor="project-select">{title}</label>
      <AsyncSelect
        onChange={onChange}
        inputId="project-select"
        cacheOptions
        defaultOptions={projects}
        loadOptions={filterOptions(projects)}
      />
    </Fragment>
  );
};

export default ProjectSelect;
