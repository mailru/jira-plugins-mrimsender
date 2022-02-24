import axios, { AxiosResponse } from 'axios';
import contextPath from 'wrm/context-path';
import { FieldHtml } from '../types';

export type IssueTypeData = { name: string; id: string };

export type ProjectData = { name: string; key: string };

export const loadProjects = (): Promise<AxiosResponse<ReadonlyArray<ProjectData>>> => {
  return axios.get(`${contextPath()}/rest/api/2/project`);
};

export const loadProjectData = (
  projectKey: string,
): Promise<AxiosResponse<{ issueTypes: ReadonlyArray<IssueTypeData> }>> => {
  return axios.get(`${contextPath()}/rest/api/2/project/${projectKey}`);
};

export const loadLabelsSugestions = (
  query: string,
): Promise<AxiosResponse<{ suggestions: ReadonlyArray<{ html: string; label: string }> }>> => {
  return axios.get(`${contextPath()}/rest/api/1.0/labels/suggest`, {
    params: { query },
  });
};

export const loadIssueForm = (
  issueType: string,
  projectId: string,
): Promise<AxiosResponse<{ fields: ReadonlyArray<FieldHtml> }>> => {
  return axios.post(
    `${contextPath()}/secure/QuickCreateIssue!default.jspa?decorator=none`,
    {
      pid: projectId,
      issuetype: issueType,
    },
    {
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    },
  );
};
