import contextPath from 'wrm/context-path';
import { IssueCreationSettings } from '../types';

export const loadChatIssueCreationSettings = async (chatId: string): Promise<IssueCreationSettings> => {
  return $.ajax({
    type: 'GET',
    context: this,
    data: { chatId },
    url: `${contextPath()}/rest/myteam/1.0/issueCreation/settings/chat`,
  });
};
