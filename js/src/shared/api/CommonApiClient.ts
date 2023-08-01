import axios, { AxiosResponse } from 'axios'
import qs from 'qs'
import contextPath from 'wrm/context-path'
import { FieldHtml, FieldParam, Group, JqlFilter, User } from '../types'
import { collectFieldsData } from '../utils'
import getCancelTokenHandler from './AxiosUtils'
import urls from 'jira/util/urls'
import {IUser} from "@atlascommunity/atlas-ui";

export type IssueTypeData = { name: string; id: string }

export type ProjectData = { name: string; key: string }

export const loadProjects = (): Promise<
  AxiosResponse<ReadonlyArray<ProjectData>>
> => {
  return axios.get(`${contextPath()}/rest/api/2/project`)
}

const cancelTokenHandler = getCancelTokenHandler()

export const loadProjectData = (
  projectKey: string
): Promise<
  AxiosResponse<{ id: string; issueTypes: ReadonlyArray<IssueTypeData> }>
> => {
  return axios.get(`${contextPath()}/rest/api/2/project/${projectKey}`)
}

export const loadLabelsSugestions = (
  query: string
): Promise<
  AxiosResponse<{ suggestions: ReadonlyArray<{ html: string; label: string }> }>
> => {
  return axios.get(`${contextPath()}/rest/api/1.0/labels/suggest`, {
    params: { query },
  })
}

export const loadIssueForm = (
  issueType: string,
  projectKey: string,
  fieldParams: Array<FieldParam>
): Promise<AxiosResponse<{ fields: ReadonlyArray<FieldHtml> }>> => {
  return new Promise((resolve, reject) => {
    loadProjectData(projectKey).then(({ data: project }) => {
      axios
        .post(
          `${contextPath()}/secure/QuickCreateIssue!default.jspa?decorator=none&atl_token=${urls.atl_token()}`,
          qs.stringify(
            {
              pid: project.id,
              issuetype: issueType,
              retainValues: fieldParams.length > 0,
              retainvalues: fieldParams.length > 0,
              fieldsToRetain: fieldParams.map((fieldParam) => fieldParam.field),
              ...collectFieldsData(fieldParams),
            },
            { indices: false }
          ),
          {
            cancelToken: cancelTokenHandler('loadIssueForm').token,
            headers: {
              Accept: 'application/json',
              'Content-Type': 'application/x-www-form-urlencoded',
            },
          }
        )
        .then(resolve)
        .catch((err) => {
          if (!axios.isCancel(err)) {
            reject(err)
          }
        })
    })
  })
}

export const loadJqlFilters = (
  query: string
): Promise<AxiosResponse<ReadonlyArray<JqlFilter>>> => {
  return axios.get(`${contextPath()}/rest/myteam/1.0/subscriptions/filters`, {
    params: { query },
  })
}

export const loadJiraUsers = (
  query: string
): Promise<AxiosResponse<ReadonlyArray<User>>> => {
  return axios.get(`${contextPath()}/rest/myteam/1.0/subscriptions/users`, {
    params: { query },
  })
}

export const loadJiraGroups = (
  query: string
): Promise<AxiosResponse<ReadonlyArray<Group>>> => {
  return axios.get(`${contextPath()}/rest/myteam/1.0/subscriptions/groups`, {
    params: { query },
  })
}



export const getUsersByQuery = (searchString = ''): Promise<IUser[]> =>
    axios
        .get(`${contextPath()}/rest/api/2/groupuserpicker`, {
            params: {
                query: searchString,
                showAvatar: true,
            },
        })
        .then((response) => response.data.users.users)

