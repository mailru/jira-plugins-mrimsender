import axios, { AxiosResponse } from 'axios';
import contextPath from 'wrm/context-path';
import { IssueCreationSettings } from '../types';

export const loadChatIssueCreationSettings = async (chatId: string): Promise<AxiosResponse<IssueCreationSettings>> => {
  return axios.get(`${contextPath()}/rest/myteam/1.0/issueCreation/settings/chats/${chatId}`);
};

export const loadChatIssueCreationSettingsById = async (id: number): Promise<AxiosResponse<IssueCreationSettings>> => {
  return axios.get(`${contextPath()}/rest/myteam/1.0/issueCreation/settings/${id}`);
};

export const updateChatIssueCreationSettings = async (
  id: number,
  settings: IssueCreationSettings,
): Promise<AxiosResponse<IssueCreationSettings>> => {
  return axios.put(`${contextPath()}/rest/myteam/1.0/issueCreation/settings/${id}`, settings);
};

export const loadProjectChatIssueCreationSettings = async (
  projectId: string,
): Promise<AxiosResponse<Array<IssueCreationSettings>>> => {
  return axios.get(`${contextPath()}/rest/myteam/1.0/issueCreation/projects/${projectId}/settings`);
};
