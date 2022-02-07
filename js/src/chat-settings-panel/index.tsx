import AJS from 'AJS';

// jira-way to load
AJS.toInit(() => {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const { init } = require('./init');
  init();
});
