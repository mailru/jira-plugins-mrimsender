import contextPath from 'wrm/context-path'
import axios from 'axios'
import urls from 'jira/util/urls'
import qs from 'query-string'

const addReminder = (data: { description?: string; date: Date }) => {
  return axios
    .post<number>(
      `${contextPath()}/rest/myteam/1.0/reminder?atl_token=${urls.atl_token()}`,
      qs.stringify(data)
    )
    .then((res) => res.data)
}

export default addReminder
