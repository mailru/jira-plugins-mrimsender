import dompurify from 'dompurify'
import { FieldParam } from './types'

export const sanitizer = dompurify.sanitize

export const collectFieldsData = (
  fieldParams: Array<FieldParam>
): Record<string, Array<string>> => {
  return fieldParams.reduce(
    (map: Record<string, Array<string>>, fieldParam) => {
      let value = map[fieldParam.field]
      const result = map

      if (value) {
        value = [...value, fieldParam.value]
      } else {
        value = [fieldParam.value]
      }
      result[fieldParam.field] = value
      return result
    },
    {}
  )
}
