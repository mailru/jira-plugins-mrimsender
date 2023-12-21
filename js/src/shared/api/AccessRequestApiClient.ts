import axios, { AxiosResponse } from 'axios'
import contextPath from 'wrm/context-path'
import {
  AccessRequest,
  AccessRequestConfiguration,
  Field,
  Group,
  ProjectRole,
  User,
} from '../types'

export const getAccessRequest = (issueKey: string): Promise<AccessRequest> =>
  axios
    .get(`${contextPath()}/rest/myteam/1.0/accessRequest`, {
      params: { issueKey },
    })
    .then((response) => response.data)

export const sendAccessRequest = (
  issueKey: string,
  accessRequest: AccessRequest
): Promise<any> =>
  axios
    .post(`${contextPath()}/rest/myteam/1.0/accessRequest`, accessRequest, {
      params: { issueKey },
    })
    .then((response) => response.data)

export const getAccessRequestConfiguration = (
  projectKey: string
): Promise<AccessRequestConfiguration> =>
  axios
    .get(
      `${contextPath()}/rest/myteam/1.0/accessRequest/configuration/${projectKey}`
    )
    .then((response) => response.data)

export const createAccessRequestConfiguration = (
  configuration: AccessRequestConfiguration
) =>
  axios
    .post(
      `${contextPath()}/rest/myteam/1.0/accessRequest/configuration/${
        configuration.projectKey
      }`,
      configuration
    )
    .then((response) => response.data)

export const updateAccessRequestConfiguration = (
  configuration: AccessRequestConfiguration,
  configurationId: number
) =>
  axios
    .put(
      `${contextPath()}/rest/myteam/1.0/accessRequest/configuration/${
        configuration.projectKey
      }/${configurationId}`,
      configuration
    )
    .then((response) => response.data)

export const deleteAccessRequestConfiguration = (
  projectKey: string,
  configurationId: number
): Promise<any> =>
  axios
    .delete(
      `${contextPath()}/rest/myteam/1.0/accessRequest/configuration/${projectKey}/${configurationId}`
    )
    .then((response) => response.data)

export const loadJiraUsers = (
  query: string
): Promise<AxiosResponse<ReadonlyArray<User>>> => {
  return axios.get(
    `${contextPath()}/rest/myteam/1.0/accessRequest/configuration/users`,
    {
      params: { query },
    }
  )
}

export const loadJiraGroups = (
  query: string
): Promise<AxiosResponse<ReadonlyArray<Group>>> => {
  return axios.get(
    `${contextPath()}/rest/myteam/1.0/accessRequest/configuration/groups`,
    {
      params: { query },
    }
  )
}

export const loadProjectRoles = (
  projectKey: string,
  query: string
): Promise<AxiosResponse<ReadonlyArray<ProjectRole>>> => {
  return axios.get(
    `${contextPath()}/rest/myteam/1.0/accessRequest/configuration/projectRoles`,
    {
      params: { projectKey, query },
    }
  )
}

export const loadUserFields = (
  projectKey: string,
  query: string
): Promise<AxiosResponse<ReadonlyArray<Field>>> => {
  return axios.get(
    `${contextPath()}/rest/myteam/1.0/accessRequest/configuration/userFields`,
    {
      params: { projectKey, query },
    }
  )
}
