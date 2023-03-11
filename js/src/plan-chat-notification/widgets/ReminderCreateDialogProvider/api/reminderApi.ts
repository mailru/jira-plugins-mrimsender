import contextPath from 'wrm/context-path'
import axios from 'axios'
import urls from 'jira/util/urls'

const addReminder = (data: {
  description?: string
  date: Date
  issueKey: string
}) => {
  return axios
    .post<number>(
      `${contextPath()}/rest/myteam/1.0/reminder?atl_token=${urls.atl_token()}`,
      data
    )
    .then((res) => res.data)
}

export default addReminder
