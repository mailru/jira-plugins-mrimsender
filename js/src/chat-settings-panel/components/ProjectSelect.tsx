import React, { ReactElement, useLayoutEffect, useState } from 'react'
import { AsyncSelect, OptionType, SelectProps } from '@atlaskit/select'
import { loadProjects, ProjectData } from '../../shared/api/CommonApiClient'

type Props = SelectProps<OptionType> & {
  className?: string
  defaultProjectKey?: string
  id: string
  onChange: (value: OptionType | null) => void
}

const mapProjectsToOptions = (
  projects: ReadonlyArray<ProjectData>
): Array<OptionType> => {
  return projects.map((project) => {
    return {
      label: `${project.name} (${project.key})`,
      value: project.key,
      id: project.id,
    }
  })
}

const filterOptions = (options: Array<OptionType>) => {
  return (inputValue: string) =>
    new Promise((resolve) => {
      resolve(
        options.filter(
          (o) =>
            !inputValue ||
            inputValue.length === 0 ||
            o.label
              .toLocaleLowerCase()
              .indexOf(inputValue.toLocaleLowerCase()) !== -1 ||
            String(o.value)
              .toLocaleLowerCase()
              .indexOf(inputValue.toLocaleLowerCase()) !== -1
        )
      )
    })
}

function ProjectSelect({
  id,
  className,
  defaultProjectKey,
  onChange,
}: Props): ReactElement {
  const [projects, setProjects] = useState<Array<OptionType>>([])
  const [value, setValue] = useState<OptionType | null>()

  const updateValue = (selectValue: OptionType | null): void => {
    setValue(selectValue)
    onChange(selectValue)
  }

  useLayoutEffect(() => {
    loadProjects().then(({ data }) => {
      const options = mapProjectsToOptions(data)
      setProjects(options)

      const selectedValues = options.filter(
        (o) => o.value === defaultProjectKey
      )

      if (selectedValues.length) {
        updateValue(selectedValues[0])
      }

      setProjects(mapProjectsToOptions(data))
    })
  }, [defaultProjectKey])

  return (
    <>
      <AsyncSelect
        className={className}
        onChange={updateValue}
        inputId={id}
        value={value}
        cacheOptions
        defaultOptions={projects}
        loadOptions={filterOptions(projects)}
      />
      {/* support Insight fields */}
      <input value={value?.id} name="pid" type="hidden" />
    </>
  )
}

ProjectSelect.defaultProps = {
  className: undefined,
  defaultProjectKey: undefined,
}

export default ProjectSelect
