export type IssueCreationSettings = {
  id: number;
  chatId: string;
  enabled: boolean;
  projectKey: string;
  issueTypeId: string;
  tag: string;
  labels: Array<string>;
};
