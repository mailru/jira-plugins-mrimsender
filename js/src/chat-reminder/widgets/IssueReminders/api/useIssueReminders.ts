import { getIssueReminders } from '@shared/reminder'
import { useQuery } from '@tanstack/react-query'

import issueReminderKeys from './keys'

const useIssueReminders = (issueKey: string) => {
  return useQuery([issueReminderKeys.all], () => getIssueReminders(issueKey))
}

export default useIssueReminders
