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
  settings: IssueCreationSettings,
): Promise<IssueCreationSettings> => {
  return $.ajax({
    type: 'PUT',
    // context: this,
    contentType: 'application/json',
    data: JSON.stringify({ settings }),
    url: `${contextPath()}/rest/myteam/1.0/issueCreation/settings`,
  });
};

export const testSettings = async (settings: IssueCreationSettings): Promise<IssueCreationSettings> => {
  return $.ajax({
    type: 'PUT',
    // context: this,
    // dataType: 'application/json',
    // contentType: 'application/json; charset=utf-8',
    // dataType: 'json',
    contentType: 'application/json;charset=UTF-8',
    data: JSON.stringify({ field: 'field' }),
    url: `${contextPath()}/rest/myteam/1.0/issueCreation/settings/test`,
  });
};
