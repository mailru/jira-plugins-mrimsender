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
  labels: Array<string>;
  additionalFields: Array<FieldParam>;
  reporter: 'INITIATOR' | 'MESSAGE_AUTHOR';
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
