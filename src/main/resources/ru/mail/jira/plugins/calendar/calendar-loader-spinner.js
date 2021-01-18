define('calendar/calendar-loader-spinner', function() {
    var loadingIndicatorDelay = function (ms) {
        return $.Deferred(function(dfrd) {
            setTimeout(dfrd.resolve, ms);
        });
    }
    var spinning = false;
    var spinnerDelayDeferred = loadingIndicatorDelay(300);
    return {
        toggleSpinner: function(){
            var calendarHideDiv = $("#calendar-hide-div");
            if(!spinning) {
                spinnerDelayDeferred.then(function () {
                    if (calendarHideDiv.length === 0) {
                        $("#calendar-full-calendar").find(".fc-view-harness").append('<div id="calendar-hide-div"><p>' + AJS.I18n.getText('ru.mail.jira.plugins.calendar.loading') + '</p><aui-spinner size="large"></aui-spinner><div class="calendar-hide-div-background"></div></div>');
                    } else {
                        calendarHideDiv.show();
                    }
                }, function () {
                    calendarHideDiv.hide();
                });
            } else {
                calendarHideDiv.hide();
                if(spinnerDelayDeferred !== undefined)
                    spinnerDelayDeferred.reject();
            }
            spinning = !spinning;
        }
    };
});