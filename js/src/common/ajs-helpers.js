// eslint-disable-next-line import/no-extraneous-dependencies
import AJS from 'AJS';


function parseJson(json) {
    try {
        return JSON.parse(json);
    } catch (e) {
        console.warn(e);
        return json;
    }
}

export function getContextPath() {
    if (AJS.gadget) {
        return AJS.gadget.getBaseUrl();
    } else {
        return AJS.contextPath();
    }
}

export function escapeHtml(html) {
    if (!html) {
        return null;
    }
    return AJS.escapeHtml(html);
}

export function getPluginBaseUrl() {
    return getBaseUrl() + '/rest/mailrucalendar/latest';
}

export function getBaseUrl() {
    return AJS.contextPath();
}

export function ajaxGet(url) {
    return ajaxPromise(url, 'GET');
}

export function ajaxDelete(url) {
    return ajaxPromise(url, 'DELETE');
}

export function ajaxPost(url, data) {
    return ajaxPromise(url, 'POST', null, data);
}

export function ajaxPut(url, data) {
    return ajaxPromise(url, 'PUT', null, data);
}

export function ajaxPromise(url, method, params, data) {
    return new Promise((resolve, reject) => {
        return AJS.$
            .ajax({
                url: url,
                type: method,
                contentType: method !== 'GET' ? 'application/json' : undefined,
                dataType: 'json',
                data: method !== 'GET' && method !== 'DELETE' ? JSON.stringify(data) : undefined
            })
            .then(
                data => {
                    resolve(data);
                },
                error => {
                    reject({
                        response: {
                            ...error,
                            data: error.responseText ?
                                parseJson(error.responseText) :
                                {
                                    error: `${error.status}: ${error.statusText}`
                                }

                        },
                        message: `${error.status}: ${error.statusText}`
                    });
                }
            );
    });
}
