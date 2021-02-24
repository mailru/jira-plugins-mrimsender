import $ from 'jquery';
import contextPath from 'wrm/context-path';
import { ChatCreationData } from './ChatPanelStore';

export type ChatInfoType = {
  link: string;
  name: string;
  members:ChatMember[];
};

export type ChatMember = {
  id:number,
  displayName:string,
  avatarUrl:string,
}

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

  async createChat(issueKey: string, name: string, memberIds: number[]): Promise<ChatInfoType> {
    return $.ajax({
      type: 'POST',
      context: this,
      data: {
        name: name,
        memberIds: memberIds,
      },
      url: `${contextPath()}/rest/myteam/1.0/chats/createChat/${issueKey}`,
    });
  }
}
