import React, {ReactElement, useState} from "react";
import {AsyncSelect, OptionsType, OptionType} from "@atlaskit/select";
import {I18n} from "@atlassian/wrm-react-i18n";

export type BaseFieldsSelectProps = {
    projectKey: string
    selectedValue?: OptionsType
    id: string
    onChange: (value: OptionsType) => void
    loadFields: (projectKey: string, query?: string) => Promise<OptionType[]>
}

export function BaseFieldSelect({
      id,
      onChange,
      selectedValue,
      projectKey,
      loadFields,
}: BaseFieldsSelectProps): ReactElement {
    const [currentValue, setCurrentValue] = useState<OptionsType | undefined>(
        selectedValue
    )

    const handleChange = (value: OptionsType): void => {
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
            loadOptions={(query?: string) => loadFields(projectKey, query)}
            noOptionsMessage={() => I18n.getText('common.concepts.no.matches')}
            isMulti
            menuPortalTarget={document.body}
            styles={{ menuPortal: (base) => ({ ...base, zIndex: 9999 }) }}
        />
    )
}