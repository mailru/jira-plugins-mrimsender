import axios, { AxiosResponse } from 'axios';
import contextPath from 'wrm/context-path';
import { FieldHtml, IssueCreationSettings } from '../types';

export const loadChatIssueCreationSettings = async (chatId: string): Promise<AxiosResponse<IssueCreationSettings>> => {
  return axios.get(`${contextPath()}/rest/myteam/1.0/issueCreation/settings/chats/${chatId}`);
};

export const getIssueCreationRequiredFields = async (
  projectKey: string,
  issueTypeId: string,
): Promise<AxiosResponse<ReadonlyArray<FieldHtml>>> => {
  return axios.get(`${contextPath()}/rest/myteam/1.0/issueCreation/fields/required`, {
    params: { projectKey, issueTypeId },
  });
};

export const updateChatIssueCreationSettings = async (
  id: number,
  settings: IssueCreationSettings,
): Promise<AxiosResponse<IssueCreationSettings>> => {
  return axios.put(`${contextPath()}/rest/myteam/1.0/issueCreation/settings/${id}`, settings);
};
