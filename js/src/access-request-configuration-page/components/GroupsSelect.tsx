import React, { ReactElement, useState } from 'react'
import { AsyncSelect, OptionsType, OptionType } from '@atlaskit/select'
import { I18n } from '@atlassian/wrm-react-i18n'
import { loadJiraGroups } from '../../shared/api/AccessRequestApiClient'

type Props = {
  id: string
  selectedValue?: OptionsType
  onChange: (value: OptionsType) => void
}

export const createGroupOption = (group: string): OptionType => {
  return {
    label: group,
    value: group,
  }
}

const loadGroups = async (query?: string): Promise<OptionType[]> => {
  return new Promise((resolve, reject) => {
    loadJiraGroups(query === undefined ? '' : query)
      .then((response) => {
        resolve(response.data.map(({ name }) => createGroupOption(name)))
      })
      .catch(reject)
  })
}

function GroupsSelect({ id, onChange, selectedValue }: Props): ReactElement {
  const [currentValue, setCurrentValue] = useState<OptionsType | undefined>(
    selectedValue
  )

  const handleChange = (value: OptionsType) => {
    setCurrentValue(value)
    onChange(value)
  }

  return (
    <AsyncSelect
      defaultOptions
      onChange={handleChange}
      inputId={id}
      value={currentValue}
      cacheOptions
      loadOptions={loadGroups}
      noOptionsMessage={() => I18n.getText('common.concepts.no.matches')}
      isMulti
      menuPortalTarget={document.body}
      styles={{ menuPortal: (base) => ({ ...base, zIndex: 9999 }) }}
    />
  )
}

GroupsSelect.defaultProps = {
  selectedValue: undefined,
}

export default GroupsSelect
