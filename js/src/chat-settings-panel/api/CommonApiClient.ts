import axios, { AxiosResponse } from 'axios';
import qs from 'qs';
import contextPath from 'wrm/context-path';
import { collectFieldsData } from '../shared/utils';
import { FieldHtml, FieldParam } from '../types';
import { getCancelTokenHandler } from './AxiosUtils';

export type IssueTypeData = { name: string; id: string };

export type ProjectData = { name: string; key: string };

export const loadProjects = (): Promise<AxiosResponse<ReadonlyArray<ProjectData>>> => {
  return axios.get(`${contextPath()}/rest/api/2/project`);
};

const cancelTokenHandler = getCancelTokenHandler();

export const loadProjectData = (
  projectKey: string,
): Promise<AxiosResponse<{ id: string; issueTypes: ReadonlyArray<IssueTypeData> }>> => {
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
  projectKey: string,
  fieldParams: Array<FieldParam>,
): Promise<AxiosResponse<{ fields: ReadonlyArray<FieldHtml> }>> => {
  return new Promise((resolve, reject) => {
    loadProjectData(projectKey).then(({ data: project }) => {
      axios
        .post(
          `${contextPath()}/secure/QuickCreateIssue!default.jspa?decorator=none`,
          qs.stringify(
            {
              pid: project.id,
              issuetype: issueType,
              retainValues: fieldParams.length > 0,
              retainvalues: fieldParams.length > 0,
              fieldsToRetain: fieldParams.map((fieldParam) => fieldParam.field),
              ...collectFieldsData(fieldParams),
            },
            { indices: false, encode: false },
          ),
          {
            cancelToken: cancelTokenHandler('loadIssueForm').token,
            headers: {
              Accept: 'application/json',
              'Content-Type': 'application/x-www-form-urlencoded',
            },
          },
        )
        .then(resolve)
        .catch((err) => {
          if (!axios.isCancel(err)) {
            reject(err);
          }
        });
    });
  });
};
