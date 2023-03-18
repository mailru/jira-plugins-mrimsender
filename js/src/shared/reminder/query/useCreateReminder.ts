import {
  useMutation,
  UseMutationResult,
  useQueryClient,
} from '@tanstack/react-query'
import { AxiosError } from 'axios'
import { addReminder, getReminder } from '..'
import { IReminder } from '../types'
import reminderKeys from './keys'

type Params = Omit<IReminder, 'id'>

const useCreateReminder = (): UseMutationResult<number, AxiosError, Params> => {
  const queryClient = useQueryClient()

  return useMutation((data) => addReminder(data), {
    onSuccess: (id) => {
      getReminder(id).then((r) => {
        const previousGroups: Array<IReminder> =
          queryClient.getQueryData([reminderKeys.all]) || []
        queryClient.setQueryData(
          [reminderKeys.all],
          // @ts-ignore
          [...previousGroups, r].sort((a, b) => a.date - b.date)
        )
      })
    },
  })
}

export default useCreateReminder
