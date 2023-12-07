import {OptionsType, OptionType} from "@atlaskit/select";
import {Field} from "@shared/types";
import {loadAccessPermissionFields} from "@shared/api/AccessRequestApiClient";
import {BaseFieldSelect} from "./BaseFieldSelect";

type Props = {
    projectKey: string
    selectedValue?: OptionsType
    id: string
    onChange: (value: OptionsType) => void
}

export const createAccessPermissionFieldOption = (role: Field): OptionType => {
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
      loadAccessPermissionFields(projectKey, query === undefined ? '' : query)
          .then((response) => {
              resolve(response.data.map(createAccessPermissionFieldOption))
          })
          .catch(reject)
  })
}

const AccessPermissionFieldsSelect = ({...rest}: Props) => {
    return (
        <BaseFieldSelect {...rest} loadFields={loadFields}/>
    )
}

export default AccessPermissionFieldsSelect

