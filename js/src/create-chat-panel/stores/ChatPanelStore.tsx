import { action, makeObservable, observable } from 'mobx';
import { ChatInfoType, LoadingService } from './LoadingService';

type ChatMember = {
  id: string;
  name: string;
  src: string;
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

  @observable.ref
  error: Error | null = null;

  @observable
  isLoading = false;

  @observable
  isDialogDataLoading = false;

  @observable
  isCreateChatDialogOpen = false;

  @observable
  chatAlreadyExist = false;

  @observable.ref
  chatInfo: ChatInfoType | null = null;

  @action('loadPanelData')
  loadPanelData = () => {
    this.isLoading = true;
    this.loadingService
      .loadChatPanelData(this.issueKey)
      .then(
        action((chatInfo: ChatInfoType) => {
          if (chatInfo != null) {
            this.chatAlreadyExist = true;
            this.chatInfo = chatInfo;
          }
        }),
        action((reason: JQueryXHR) => {
          this.error = new Error(
            `An error in loadChatPanelData status=${reason.status} statusText=${reason.statusText}`,
          );
        }),
      )
      .finally(action(() => (this.isLoading = false)));
  };

  @action('loadDialogData')
  loadDialogData = async (): Promise<void> => {
    try {
      const chatCreationData = await this.loadingService.loadChatCreationData(this.issueKey);
      if (chatCreationData != null) {
        this.dialogData = chatCreationData;
      }
    } catch (e) {
      this.error = new Error(`An error in loadDialogData status=${e.status} statusText=${e.statusText}`);
    }
  };

  @action('openCreateChatDialog')
  openCreateChatDialog = () => {
    this.isDialogDataLoading = true;
    this.loadDialogData()
      .then(
        action(() => {
          this.isCreateChatDialogOpen = true;
        }),
      )
      .finally(
        action(() => {
          this.isDialogDataLoading = false;
        }),
      );
  };

  @action('closeCreateChatDialog')
  closeCreateChatDialog = () => {
    this.isCreateChatDialogOpen = false;
  };

  @action('createChat')
  createChat = (name: string, members: number[], about: string) => {
    this.loadingService.createChat(this.issueKey, name, members, about).then(
      this.setChatInfo,
      action((reason: JQueryXHR) => {
        this.error = new Error(`An error in createChat status=${reason.status} statusText=${reason.statusText}`);
      }),
    );
  };

  @action('setChatInfo')
  setChatInfo = (chatInfo: ChatInfoType) => {
    if (chatInfo != null) {
      this.chatAlreadyExist = true;
      this.chatInfo = chatInfo;
    } else {
      this.error = new Error('chat info is empty inside setChatInfo method');
    }
  };
}
