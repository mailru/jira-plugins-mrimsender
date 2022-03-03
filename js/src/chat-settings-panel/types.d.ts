export type IssueCreationSettings = {
  id: number;
  chatId: string;
  enabled: boolean;
  projectKey: string;
  issueTypeId: string;
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
