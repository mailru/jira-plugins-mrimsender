import $ from 'jquery'
import contextPath from 'wrm/context-path'
import { OptionData } from '@atlaskit/user-picker/types'
import { ChatCreationData, ChatInfoType } from './types'

export default class LoadingService {
  async loadChatCreationData(issueKey: string): Promise<ChatCreationData> {
    return $.ajax({
      type: 'GET',
      context: this,
      url: `${contextPath()}/rest/myteam/1.0/chats/chatCreationData/${issueKey}`,
    })
  }

  async loadChatPanelData(issueKey: string): Promise<ChatInfoType> {
    return $.ajax({
      type: 'GET',
      context: this,
      url: `${contextPath()}/rest/myteam/1.0/chats/chatData/${issueKey}`,
    })
  }

  async createChat(
    issueKey: string,
    name: string,
    memberIds: number[]
  ): Promise<ChatInfoType> {
    return $.ajax({
      type: 'POST',
      context: this,
      data: {
        name,
        memberIds,
      },
      url: `${contextPath()}/rest/myteam/1.0/chats/createChat/${issueKey}`,
    })
  }

  async loadUsers(searchText?: string): Promise<OptionData | OptionData[]> {
    return $.ajax({
      type: 'GET',
      context: this,
      data: {
        searchText,
      },
      url: `${contextPath()}/rest/myteam/1.0/chats/chatCreationData/users`,
    })
  }
}
