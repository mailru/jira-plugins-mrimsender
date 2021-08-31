import $ from 'jquery';
import contextPath from 'wrm/context-path';
import { ChatCreationData } from './ChatPanelStore';
import { AvatarProps } from '@atlaskit/avatar-group';

export type ChatInfoType = {
  link: string;
  name: string;
  members: AvatarProps[];
};

export class LoadingService {
  async loadChatCreationData(issueKey: string): Promise<ChatCreationData> {
    return $.ajax({
      type: 'GET',
      context: this,
      url: `${contextPath()}/rest/myteam/1.0/chats/chatCreationData/${issueKey}`,
    });
  }

  async loadChatPanelData(issueKey: string): Promise<ChatInfoType> {
    return $.ajax({
      type: 'GET',
      context: this,
      url: `${contextPath()}/rest/myteam/1.0/chats/chatData/${issueKey}`,
    });
  }

  async createChat(issueKey: string, name: string, memberIds: number[], about: string): Promise<ChatInfoType> {
    return $.ajax({
      type: 'POST',
      context: this,
      data: {
        name: name,
        memberIds: memberIds,
        about: about,
      },
      url: `${contextPath()}/rest/myteam/1.0/chats/createChat/${issueKey}`,
    });
  }
}
