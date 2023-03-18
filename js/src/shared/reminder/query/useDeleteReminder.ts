import { deleteReminder } from '@shared/reminder'
import { IReminder } from '@shared/reminder/types'
import {
  useMutation,
  UseMutationResult,
  useQueryClient,
} from '@tanstack/react-query'
import { AxiosError } from 'axios'

import reminderKeys from './keys'

const useDeleteReminder = (): UseMutationResult<
  void,
  AxiosError,
  IReminder
> => {
  const queryClient = useQueryClient()

  return useMutation((reminder: IReminder) => deleteReminder(reminder.id), {
    onSuccess: (_, r) => {
      const previousGroups: IReminder[] =
        queryClient.getQueryData(reminderKeys.issueReminders(r.issueKey)) || []
      queryClient.setQueryData(reminderKeys.issueReminders(r.issueKey), [
        ...previousGroups.filter((r1) => r1.id !== r.id),
      ])
    },
  })
}

export default useDeleteReminder
