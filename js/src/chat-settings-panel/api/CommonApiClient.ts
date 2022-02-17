import axios, { AxiosResponse } from 'axios';
import contextPath from 'wrm/context-path';

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
