import React, { ReactElement, useLayoutEffect, useState } from 'react'
import Select from '@atlaskit/select'
import { I18n } from '@atlassian/wrm-react-i18n'

type Props = {
  id: string
  selectedValue?: string
  onChange: (value: { label: any; value: string } | null) => void
  placeholder?: string
  isClearable?: boolean
}

export const recipientsTypeOptions = [
  {
    label: I18n.getText('common.words.user'),
    value: 'USER',
  },
  {
    label: I18n.getText('common.words.group'),
    value: 'GROUP',
  },
  {
    label: I18n.getText('ru.mail.jira.plugins.myteam.createChat.panel'),
    value: 'CHAT',
  },
]

function RecipientsSelect({
  id,
  selectedValue,
  onChange,
  placeholder,
  isClearable,
}: Props): ReactElement {
  const [currentValue, setCurrentValue] = useState<{
    label: any
    value: string
  } | null>()

  const handleChange = (value: { label: any; value: string } | null): void => {
    setCurrentValue(value)
    onChange(value)
  }

  useLayoutEffect(() => {
    if (selectedValue) {
      setCurrentValue(
        recipientsTypeOptions.find(({ value }) => value === selectedValue)
      )
    }
  }, [selectedValue])

  return (
    <Select
      inputId={id}
      value={currentValue}
      onChange={handleChange}
      options={recipientsTypeOptions}
      placeholder={placeholder}
      isClearable={isClearable}
      menuPortalTarget={document.body}
      styles={{
        menuPortal: (base) => ({ ...base, zIndex: 9999 }),
      }}
    />
  )
}

RecipientsSelect.defaultProps = {
  selectedValue: undefined,
  placeholder: undefined,
  isClearable: false,
}

export default RecipientsSelect
