import React, { ReactElement, useLayoutEffect, useState } from 'react';
import { AsyncSelect, OptionType, SelectProps } from '@atlaskit/select';
import { loadProjects, ProjectData } from '../api/CommonApiClient';

type Props = SelectProps<OptionType> & {
  className?: string;
  defaultProjectKey?: string;
  id: string;
  onChange: (value: OptionType | null) => void;
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

const ProjectSelect = ({ id, className, defaultProjectKey, onChange }: Props): ReactElement => {
  const [projects, setProjects] = useState<Array<OptionType>>([]);
  const [value, setValue] = useState<OptionType | null>();

  const updateValue = (value: OptionType | null): void => {
    setValue(value);
    onChange(value);
  };

  useLayoutEffect(() => {
    loadProjects().then(({ data }) => {
      const options = mapProjectsToOptions(data);
      setProjects(options);

      const selectedValues = options.filter((o) => o.value == defaultProjectKey);

      if (selectedValues.length) {
        updateValue(selectedValues[0]);
      }

      setProjects(mapProjectsToOptions(data));
    });
  }, [defaultProjectKey]);

  return (
    <AsyncSelect
      className={className}
      onChange={updateValue}
      inputId={id}
      value={value}
      cacheOptions
      defaultOptions={projects}
      loadOptions={filterOptions(projects)}
    />
  );
};

export default ProjectSelect;
