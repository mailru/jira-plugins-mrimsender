require(['jquery', 'jira/dialog/dialog-register', 'jira/dialog/form-dialog', 'jira/dialog/dialog-util'], function($, DialogRegister, FormDialog, DialogUtil) {
    AJS.toInit(function() {
        DialogRegister.calendarEditSchedule = new FormDialog({
            id: "calendar-edit-schedule-dialog",
            trigger: ".calendar-schedule-edit",
            handleRedirect:true,
            ajaxOptions: DialogUtil.getDefaultAjaxOptions,
            onSuccessfulSubmit : DialogUtil.storeCurrentIssueIdOnSucessfulSubmit,
            delayShowUntil: DialogUtil.BeforeShowIssueDialogHandler.execute,
            isIssueDialog: true,
            widthClass: "large"
        });

        DialogRegister.calendarDeleteSchedule = new FormDialog({
            id: "calendar-delete-schedule-dialog",
            trigger: ".calendar-schedule-delete",
            handleRedirect:true,
            ajaxOptions: DialogUtil.getDefaultAjaxOptions,
            onSuccessfulSubmit : DialogUtil.storeCurrentIssueIdOnSucessfulSubmit,
            delayShowUntil: DialogUtil.BeforeShowIssueDialogHandler.execute,
            isIssueDialog: true,
            widthClass: "large"
        });

        DialogRegister.calendarCreateSchedule = new FormDialog({
            id: "calendar-create-schedule-dialog",
            trigger: ".calendar-schedule-create",
            handleRedirect:true,
            ajaxOptions: DialogUtil.getDefaultAjaxOptions,
            onSuccessfulSubmit : DialogUtil.storeCurrentIssueIdOnSucessfulSubmit,
            delayShowUntil: DialogUtil.BeforeShowIssueDialogHandler.execute,
            isIssueDialog: true,
            widthClass: "large"
        });
    });
});