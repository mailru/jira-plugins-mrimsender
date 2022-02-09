import contextPath from 'wrm/context-path';
import { IssueCreationSettings } from '../types';

export const loadChatIssueCreationSettings = async (chatId: string): Promise<IssueCreationSettings> => {
  return $.ajax({
    type: 'GET',
    context: this,
    data: { chatId },
    url: `${contextPath()}/rest/myteam/1.0/issueCreation/settings`,
  });
};

export const updateChatIssueCreationSettings = async (
  id: number,
  settings: IssueCreationSettings,
): Promise<IssueCreationSettings> => {
  return $.ajax({
    type: 'PUT',
    contentType: 'application/json',
    data: JSON.stringify(settings),
    url: `${contextPath()}/rest/myteam/1.0/issueCreation/settings/${id}`,
  });
};
