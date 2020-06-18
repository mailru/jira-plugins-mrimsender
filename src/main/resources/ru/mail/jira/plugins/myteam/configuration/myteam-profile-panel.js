require(['jquery', 'wrm/context-path'], function($, contextPath) {
    AJS.toInit(function () {
        var dialog = new AJS.Dialog({
            width: 540,
            id: 'myteam-dialog'
        });
        dialog.addHeader(AJS.I18n.getText('ru.mail.jira.plugins.mrimsender.title'));
        dialog.addPanel(null, $('#myteam-dialog-form').removeClass('hidden').submit(function (e) {
            e.preventDefault();
            dialog.get('button:0')[0].item.click();
        }));

        dialog.addButton(AJS.I18n.getText('common.forms.update'), function (dialog) {
            $('#myteam-dialog-error').addClass('hidden');
            dialog.updateHeight();

            var mrimLogin = $('#myteam-dialog-mrim-login').val().trim();
            var enabled = $('#myteam-dialog-enabled').val();

            $.ajax({
                type: 'POST',
                url: contextPath() + '/rest/myteam/1.0/preferences',
                data: {
                    atl_token: atl_token(),
                    mrim_login: mrimLogin,
                    enabled: enabled
                },
                success: function (data) {
                    $('#myteam-notification').removeClass('hidden');
                    $('#myteam-mrim-login').text(mrimLogin);
                    $('#myteam-enabled').data('enabled', enabled).text(enabled == 'true' ? AJS.I18n.getText('ru.mail.jira.plugins.mrimsender.profilePanel.notifications.enabled') : AJS.I18n.getText('ru.mail.jira.plugins.mrimsender.profilePanel.notifications.disabled'));
                    dialog.hide();
                },
                error: function showErrorMsg(request, status, error) {
                    $('#myteam-dialog-error').removeClass('hidden').find('p').text(request.responseText);
                    dialog.updateHeight();
                }
            });
        });

        dialog.addCancel(AJS.I18n.getText('common.forms.cancel'), function (dialog) {
            dialog.hide();
        });

        $('#myteam-edit').click(function (e) {
            e.preventDefault();
            $('#myteam-notification').addClass('hidden');
            $('#myteam-dialog-error').addClass('hidden');
            $('#myteam-dialog-mrim-login').val($('#myteam-mrim-login').text());
            $('#myteam-dialog-enabled').val($('#myteam-enabled').data('enabled').toString());
            dialog.show().updateHeight();
        })
    });
});
