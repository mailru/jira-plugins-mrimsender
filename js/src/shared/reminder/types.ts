export interface IReminder {
  id: number
  issueKey: string
  userEmail: string
  date: Date
  description?: string
}
