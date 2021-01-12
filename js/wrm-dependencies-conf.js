/**
 * External modules - are amd or es6 modules that exist outside this source code. e.g from JIRA or aui. These
 * modules will be required using window.require (which in JIRA land means almond).
 */

module.exports = {
  jquery: {
    dependency: 'com.atlassian.plugins.jquery:jquery',
    import: {
      var: "require('jquery')",
      amd: 'jquery',
    },
  },
  'wrm/context-path': {
    dependency: 'com.atlassian.plugins.atlassian-plugins-webresource-plugin:context-path',
    import: {
      amd: 'wrm/context-path',
      var: 'AJS.contextPath',
    },
  },
  'wrm/format': {
    dependency: 'com.atlassian.plugins.atlassian-plugins-webresource-plugin:format',
    import: {
      var: 'require("wrm/format")',
      amd: 'wrm/format',
    },
  },
  'jira/issues/search/legacyissue': {
    dependency: 'com.atlassian.jira.jira-issue-nav-plugin:issuenav-legacy',
    import: {
      var: "require('jira/issues/search/legacyissue')",
      amd: 'jira/issues/search/legacyissue',
    },
  },
};
