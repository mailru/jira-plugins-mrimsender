import contextPath from 'wrm/context-path'
import axios from 'axios'
import urls from 'jira/util/urls'
import { IReminder } from './types'

export const getReminder = (id: number) => {
  return axios
    .get<IReminder>(
      `${contextPath()}/rest/myteam/1.0/reminder/${id}?atl_token=${urls.atl_token()}`
    )
    .then((res) => res.data)
}

export const addReminder = (data: Omit<IReminder, 'id'>) => {
  return axios
    .post<number>(
      `${contextPath()}/rest/myteam/1.0/reminder?atl_token=${urls.atl_token()}`,
      data
    )
    .then((res) => res.data)
}

export const deleteReminder = (id: number) => {
  return axios
    .delete(
      `${contextPath()}/rest/myteam/1.0/reminder/${id}?atl_token=${urls.atl_token()}`
    )
    .then((res) => res.data)
}

export const getIssueReminders = (issueKey: string) => {
  return axios
    .get<IReminder[]>(`${contextPath()}/rest/myteam/1.0/reminder`, {
      params: { issueKey },
    })
    .then((res) => res.data)
}
