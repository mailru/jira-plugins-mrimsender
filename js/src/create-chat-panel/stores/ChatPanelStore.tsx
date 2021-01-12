import { action, makeObservable, observable, runInAction } from 'mobx';
import { ChatMeta, LoadingService } from './LoadingService';

type ChatMember = {
  id: string;
  displayName: string;
  avatarUrl: string;
};

export type ChatCreationData = {
  name: string;
  description: string;
  members: ChatMember[];
};

export class ChatPanelStore {
  readonly issueKey: string;
  readonly loadingService: LoadingService;

  constructor(issueKey: string) {
    makeObservable(this);
    this.issueKey = issueKey;
    this.loadingService = new LoadingService();
    this.loadPanelData();
  }

  @observable
  dialogData: ChatCreationData | null = null;

  @observable
  hasErrors = false;

  @observable
  isCreateChatDialogOpen = false;

  @observable
  chatLink?: string;

  @action('loadPanelData')
  loadPanelData = () => {
    this.loadingService.loadChatPanelData(this.issueKey).then(
      (chatMeta) => {
        if (chatMeta != null && chatMeta.link != null) {
          this.chatLink = chatMeta.link;
        }
      },
      () => {
        this.hasErrors = true;
      },
    );
  };

  @action('loadDialogData')
  loadDialogData = async (): Promise<void> => {
    try {
      const chatCreationData = await this.loadingService.loadChatCreationData(this.issueKey);
      if (chatCreationData != null) {
        this.dialogData = chatCreationData;
      }
    } catch (e) {
      this.hasErrors = true;
    }
  };

  @action('openCreateChatDialog')
  openCreateChatDialog = () => {
    this.loadDialogData().then(
      action(() => {
        this.isCreateChatDialogOpen = true;
      }),
    );
  };

  @action('closeCreateChatDialog')
  closeCreateChatDialog = () => {
    this.isCreateChatDialogOpen = false;
  };

  @action('createChat')
  createChat = (name: string, members: number[]) => {
    this.loadingService.createChat(this.issueKey, name, members).then(
      this.setChatLink,
      action(() => (this.hasErrors = true)),
    );
  };

  @action('setChatLink')
  setChatLink = (chatMeta: ChatMeta) => {
    if (chatMeta != null && chatMeta.link != null) {
      this.chatLink = chatMeta.link;
    } else {
      this.hasErrors = true;
    }
  };
}
