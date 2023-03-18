import { deleteReminder } from '@shared/reminder'
import { IReminder } from '@shared/reminder/types'
import {
  useMutation,
  UseMutationResult,
  useQueryClient,
} from '@tanstack/react-query'
import { AxiosError } from 'axios'

import reminderKeys from './keys'

const useDeleteReminder = (): UseMutationResult<void, AxiosError, number> => {
  const queryClient = useQueryClient()

  return useMutation((id: number) => deleteReminder(id), {
    onSuccess: (_, id) => {
      const previousGroups: IReminder[] =
        queryClient.getQueryData([reminderKeys.all]) || []
      queryClient.setQueryData(
        [reminderKeys.all],
        [...previousGroups.filter((g) => g.id !== id)]
      )
    },
  })
}

export default useDeleteReminder
