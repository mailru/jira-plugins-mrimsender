import { getIssueReminders } from '@shared/reminder'
import { useQuery } from '@tanstack/react-query'

import reminderKeys from './keys'

const useIssueReminders = (issueKey: string) => {
  return useQuery(reminderKeys.issueReminders(issueKey), () =>
    getIssueReminders(issueKey)
  )
}

export default useIssueReminders
