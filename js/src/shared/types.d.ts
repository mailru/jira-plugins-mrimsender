export type IssueCreationSettings = {
  id: number;
  chatLink?: string;
  chatTitle?: string;
  canEdit?: boolean;
  chatId: string;
  enabled: boolean;
  projectKey: string;
  issueTypeId: string;
  issueTypeName?: string;
  tag: string;
  creationSuccessTemplate: string;
  issueSummaryTemplate: string;
  issueQuoteMessageTemplate: string;
  labels: Array<string>;
  additionalFields: Array<FieldParam>;
  creationByAllMembers: boolean;
  reporter: 'INITIATOR' | 'MESSAGE_AUTHOR';
  addReporterInWatchers: boolean;
};

export type FieldHtml = {
  id: string;
  label: string;
  editHtml: string;
  required: boolean;
};

export type FieldParam = {
  field: string;
  value: string;
};

export type LoadableDataState<T> = {
  isLoading: boolean;
  data?: T;
  error?: string;
};

export type ErrorData = {
  error?: string;
  fieldErrors?: { [name: string]: { messages: string[] } };
  status: number;
  timestamp: number;
};

export type FilterSubscription = {
  id?: number;
  filter?: JqlFilter;
  creator?: User;
  recipientsType?: string;
  chats?: string[];
  users?: User[];
  groups?: string[];
  scheduleMode?: string;
  scheduleDescription?: string;
  hours?: number;
  minutes?: number;
  weekDays?: string[];
  monthDay?: number;
  advanced?: string;
  lastRun?: string;
  nextRun?: string;
  type?: string;
  emailOnEmpty: boolean;
};

export type User = {
  userKey: string;
  displayName: string;
};

export type Group = {
  name: string;
};

export type JqlFilter = {
  id: number;
  name: string;
  owner: boolean;
};
