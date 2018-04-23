/* eslint-disable flowtype/require-valid-file-annotation */
import {ajaxGet, ajaxPost, getBaseUrl} from '../common/ajs-helpers';


export class JiraService {
    static getAutoCompleteData() {
        return ajaxGet(`${getBaseUrl()}/rest/api/2/jql/autocompletedata`);
    }

   static validateQuery(query) {
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
