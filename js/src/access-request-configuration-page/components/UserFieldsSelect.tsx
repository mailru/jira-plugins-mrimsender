import React, { ReactElement, useState } from 'react'
import { AsyncSelect, OptionsType, OptionType } from '@atlaskit/select'
import { I18n } from '@atlassian/wrm-react-i18n'
import { Field } from '../../shared/types'
import { loadUserFields } from '../../shared/api/AccessRequestApiClient'
import {BaseFieldsSelectProps, BaseFieldSelect} from "./BaseFieldSelect";

type Props = Omit<BaseFieldsSelectProps, "loadFields">

export const createUserFieldOption = (role: Field): OptionType => {
  return {
    label: role.name,
    value: role.id,
  }
}

const loadFields = async (
  projectKey: string,
  query?: string
): Promise<OptionType[]> => {
  return new Promise((resolve, reject) => {
    loadUserFields(projectKey, query === undefined ? '' : query)
      .then((response) => {
        resolve(response.data.map(createUserFieldOption))
      })
      .catch(reject)
  })
}

function UserFieldsSelect({
  ...rest
}: Props): ReactElement {
  return (
      <BaseFieldSelect {...rest} loadFields={loadFields}/>
  )
}

UserFieldsSelect.defaultProps = {
  selectedValue: undefined,
}

export default UserFieldsSelect
