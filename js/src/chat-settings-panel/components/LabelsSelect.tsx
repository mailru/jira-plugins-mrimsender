import React, { ReactElement, useLayoutEffect, useState } from 'react'
import { AsyncCreatableSelect, OptionsType, OptionType } from '@atlaskit/select'
import { loadLabelsSugestions } from '../../shared/api/CommonApiClient'

const createOption = (label: string) => ({
  label,
  value: label.toLowerCase().replace(/\s/g, ''),
})

type Props = {
  id: string
  className?: string
  defaultLabels?: Array<string>
  onChange: (value: OptionsType<OptionType>) => void
}

const loadLabels = async (query: string): Promise<Array<OptionType>> => {
  return new Promise((resolve, reject) => {
    loadLabelsSugestions(query)
      .then((response) => {
        resolve(response.data.suggestions.map((s) => createOption(s.label)))
      })
      .catch(reject)
  })
}

function LabelsSelect({
  id,
  className,
  defaultLabels,
  onChange,
}: Props): ReactElement {
  const [value, setValue] = useState<OptionsType<OptionType>>()
  const [options, setOptions] = useState<OptionsType<OptionType>>([])

  const handleChange = (newValue: OptionsType<OptionType>) => {
    setValue(newValue)
    onChange(newValue)
  }

  const handleCreate = (inputValue: string) => {
    const newOption = createOption(inputValue)

    const newOptions = options.slice()
    newOptions.push(newOption)

    const newValue = value ? value.slice() : []

    newValue.push(newOption)
    setValue(newValue)
    setOptions(options)
    onChange(newValue)
  }

  useLayoutEffect(() => {
    const newValues = new Set<string>(defaultLabels)
    if (defaultLabels) {
      const mappedOptions = Array.from(newValues).map(createOption)

      setValue(mappedOptions)
      onChange(mappedOptions)

      options.forEach(
        (l) => !newValues.has(String(l.value)) && newValues.add(String(l.value))
      )
    }

    loadLabels('')
      .then((labelOptions) => {
        labelOptions.forEach(
          (l) =>
            !newValues.has(String(l.value)) && newValues.add(String(l.value))
        )

        setOptions(Array.from(newValues).map(createOption))
      })
      .catch(() => {
        setOptions(Array.from(newValues).map(createOption))
      })
  }, [defaultLabels])

  return (
    <AsyncCreatableSelect
      className={className}
      isMulti
      onChange={handleChange}
      onCreateOption={handleCreate}
      options={options}
      inputId={id}
      value={value}
      cacheOptions
      defaultOptions={options}
      loadOptions={loadLabels}
    />
  )
}

LabelsSelect.defaultProps = {
  className: undefined,
  defaultLabels: undefined,
}

export default LabelsSelect
