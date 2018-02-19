/**
 * Run statistic counter - like Google Analytics.
 * Surely, it doesn't collect any personal data or private information.
 * All information you can check on top.mail.ru.
 */
export function collectTopMailCounterScript() {
    var _tmr = window._tmr || (window._tmr = []);
    _tmr.push({ id: '2706504', type: 'pageView', start: (new Date()).getTime() });
    (function(d, w, id) {
        if (d.getElementById(id)) {
            return;
        }
        var ts = d.createElement('script');
        ts.type = 'text/javascript';
        ts.async = true;
        ts.id = id;
        ts.src = (d.location.protocol == 'https:' ? 'https:' : 'http:') + '//top-fwz1.mail.ru/js/code.js';
        var f = function() {
            var s = d.getElementsByTagName('script')[0];
            s.parentNode.insertBefore(ts, s);
        };
        if (w.opera == '[object Opera]') {
            d.addEventListener('DOMContentLoaded', f, false);
        } else {
            f();
        }
    })(document, window, 'topmailru-code');
}
