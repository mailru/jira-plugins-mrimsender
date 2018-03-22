import {ajaxGet, ajaxPost, getBaseUrl} from '../common/ajs-helpers';


export class JiraService {
    getAutoCompleteData() {
        return ajaxGet(`${getBaseUrl()}/rest/api/2/jql/autocompletedata`);
    }

    validateQuery(query) {
        return ajaxPost(
            `${getBaseUrl()}/rest/api/2/search`,
            {
                jql: query,
                validateQuery: true,
                maxResults: 0
            }
        );
    }
}
