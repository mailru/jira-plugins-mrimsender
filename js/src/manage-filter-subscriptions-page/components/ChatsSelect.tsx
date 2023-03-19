import React, { ReactElement, useLayoutEffect, useState } from 'react'
import { CreatableSelect, OptionsType } from '@atlaskit/select'

type Props = {
  id: string
  selectedValue?: OptionsType
  onChange: (value: OptionsType) => void
}

export const createChatOption = (chatId: string) => ({
  label: chatId,
  value: chatId,
})

function ChatsSelect({ id, selectedValue, onChange }: Props): ReactElement {
  const [currentValue, setCurrentValue] = useState<OptionsType>()
  const [options, setOptions] = useState<OptionsType>([])

  const handleChange = (value: OptionsType) => {
    setCurrentValue(value)
    onChange(value)
  }

  const handleCreate = (inputValue: string) => {
    const newOption = createChatOption(inputValue)

    const newOptions = options.slice()
    newOptions.push(newOption)

    const newValue = currentValue ? currentValue.slice() : []

    newValue.push(newOption)
    setCurrentValue(newValue)
    onChange(newValue)
    setOptions(newOptions)
  }

  useLayoutEffect(() => {
    if (selectedValue) {
      const newValues = new Set<string>(
        selectedValue.map(({ value }) => value.toString())
      )
      setCurrentValue(selectedValue)

      options.forEach(
        (l) => !newValues.has(String(l.value)) && newValues.add(String(l.value))
      )
    }
  }, [selectedValue])

  return (
    <CreatableSelect
      isMulti
      onChange={handleChange}
      onCreateOption={handleCreate}
      options={options}
      inputId={id}
      value={currentValue}
      cacheOptions
      placeholder="VK Teams Chat Id"
    />
  )
}

ChatsSelect.defaultProps = {
  selectedValue: undefined,
}

export default ChatsSelect
