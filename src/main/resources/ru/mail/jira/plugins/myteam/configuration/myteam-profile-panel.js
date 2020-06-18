require(['jquery', 'wrm/context-path'], function($, contextPath) {
    AJS.toInit(function () {
        var dialog = new AJS.Dialog({
            width: 540,
            id: 'mrimsender-dialog'
        });
        dialog.addHeader(AJS.I18n.getText('ru.mail.jira.plugins.mrimsender.title'));
        dialog.addPanel(null, $('#mrimsender-dialog-form').removeClass('hidden').submit(function (e) {
            e.preventDefault();
            dialog.get('button:0')[0].item.click();
        }));

        dialog.addButton(AJS.I18n.getText('common.forms.update'), function (dialog) {
            $('#mrimsender-dialog-error').addClass('hidden');
            dialog.updateHeight();

            var mrimLogin = $('#mrimsender-dialog-mrim-login').val().trim();
            var enabled = $('#mrimsender-dialog-enabled').val();

            $.ajax({
                type: 'POST',
                url: contextPath() + '/rest/myteam/1.0/preferences',
                data: {
                    atl_token: atl_token(),
                    mrim_login: mrimLogin,
                    enabled: enabled
                },
                success: function (data) {
                    $('#mrimsender-notification').removeClass('hidden');
                    $('#mrimsender-mrim-login').text(mrimLogin);
                    $('#mrimsender-enabled').data('enabled', enabled).text(enabled == 'true' ? AJS.I18n.getText('ru.mail.jira.plugins.mrimsender.profilePanel.notifications.enabled') : AJS.I18n.getText('ru.mail.jira.plugins.mrimsender.profilePanel.notifications.disabled'));
                    dialog.hide();
                },
                error: function showErrorMsg(request, status, error) {
                    $('#mrimsender-dialog-error').removeClass('hidden').find('p').text(request.responseText);
                    dialog.updateHeight();
                }
            });
        });

        dialog.addCancel(AJS.I18n.getText('common.forms.cancel'), function (dialog) {
            dialog.hide();
        });

        $('#mrimsender-edit').click(function (e) {
            e.preventDefault();
            $('#mrimsender-notification').addClass('hidden');
            $('#mrimsender-dialog-error').addClass('hidden');
            $('#mrimsender-dialog-mrim-login').val($('#mrimsender-mrim-login').text());
            $('#mrimsender-dialog-enabled').val($('#mrimsender-enabled').data('enabled').toString());
            dialog.show().updateHeight();
        })
    });
});
