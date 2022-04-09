import axios, { AxiosResponse } from 'axios';
import contextPath from 'wrm/context-path';
import { IssueCreationSettings, IssueCreationSettingsDefault } from '../types';

export const loadChatIssueCreationSettings = async (
  chatId: string,
): Promise<AxiosResponse<Array<IssueCreationSettings>>> => {
  return axios.get(`${contextPath()}/rest/myteam/1.0/issueCreation/settings/chats/${chatId}`);
};

export const loadChatIssueCreationSettingsById = async (id: number): Promise<AxiosResponse<IssueCreationSettings>> => {
  return axios.get(`${contextPath()}/rest/myteam/1.0/issueCreation/settings/${id}`);
};

export const loadChatIssueDefaultSettings = async (): Promise<AxiosResponse<IssueCreationSettingsDefault>> => {
  return axios.get(`${contextPath()}/rest/myteam/1.0/issueCreation/settings/default`);
};

export const createChatIssueCreationSettings = async (
  settings: IssueCreationSettings,
): Promise<AxiosResponse<IssueCreationSettings>> => {
  return axios.post(`${contextPath()}/rest/myteam/1.0/issueCreation/settings`, settings);
};

export const updateChatIssueCreationSettings = async (
  id: number,
  settings: IssueCreationSettings,
): Promise<AxiosResponse<IssueCreationSettings>> => {
  return axios.put(`${contextPath()}/rest/myteam/1.0/issueCreation/settings/${id}`, settings);
};

export const deleteChatIssueCreationSettings = async (id: number): Promise<AxiosResponse<IssueCreationSettings>> => {
  return axios.delete(`${contextPath()}/rest/myteam/1.0/issueCreation/settings/${id}`);
};

export const loadProjectChatIssueCreationSettings = async (
  projectKey: string,
): Promise<AxiosResponse<Array<IssueCreationSettings>>> => {
  return axios.get(`${contextPath()}/rest/myteam/1.0/issueCreation/settings/projects/${projectKey}`);
};
