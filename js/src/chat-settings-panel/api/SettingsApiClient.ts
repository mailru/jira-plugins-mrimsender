import axios, { AxiosResponse } from 'axios';
import contextPath from 'wrm/context-path';
import { IssueCreationSettings } from '../types';

export const loadChatIssueCreationSettings = async (chatId: string): Promise<AxiosResponse<IssueCreationSettings>> => {
  return axios.get(`${contextPath()}/rest/myteam/1.0/issueCreation/settings`, { params: { chatId } });
};

export const updateChatIssueCreationSettings = async (
  id: number,
  settings: IssueCreationSettings,
): Promise<AxiosResponse<IssueCreationSettings>> => {
  return axios.put(`${contextPath()}/rest/myteam/1.0/issueCreation/settings/${id}`, settings);
};
