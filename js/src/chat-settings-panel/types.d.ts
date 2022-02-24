export type IssueCreationSettings = {
  id: number;
  chatId: string;
  enabled: boolean;
  projectKey: string;
  issueTypeId: string;
  tag: string;
  labels: Array<string>;
};

export type FieldHtml = {
  id: string;
  label: string;
  editHtml: string;
  required: boolean;
};
