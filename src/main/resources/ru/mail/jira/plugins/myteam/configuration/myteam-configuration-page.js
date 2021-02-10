require(['jquery'], function($) {
    AJS.toInit(function () {

        const checkbox = document.getElementById('setTokenViaFile');
        checkbox.addEventListener('change', (event) => {
            if (event.currentTarget.checked) {
                document.getElementById('setTokenViaFile').value="true";
                document.getElementById('myteam-config-page-token-field').style.display = 'none';
                document.getElementById('myteam-config-page-token-file-field').style.display = 'block';
            } else {
                document.getElementById('setTokenViaFile').value="false";
                document.getElementById('myteam-config-page-token-field').style.display = 'block';
                document.getElementById('myteam-config-page-token-file-field').style.display = 'none';
            }
        });

        $(".mrimsender-create-issue-excluding-projects").auiSelect2({
            placeholder:AJS.I18n.getText('ru.mail.jira.plugins.myteam.configuration.excludedProjects.placeholder'),
            allowClear: true
        });
        $(".myteam-chat-creation-disabled-projects").auiSelect2({
            placeholder:AJS.I18n.getText('ru.mail.jira.plugins.myteam.configuration.chatCreationProjects.placeholder'),
            allowClear: true
        });
    });
    // mapping value of project inside hidden input tag
    $(document).on('change', '.mrimsender-create-issue-excluding-projects', function() {
        var fieldValue = $(this).auiSelect2('data');
        $('#mrimsender-create-issue-excluding-projects-ids').attr('value', fieldValue.length > 0 ? fieldValue.map(function(item) { return item.id }) : '');
    });

    // mapping value of project inside hidden input tag
    $(document).on('change', '.myteam-chat-creation-disabled-projects', function() {
        var fieldValue = $(this).auiSelect2('data');
        $('#myteam-chat-creation-disabled-project-ids').attr('value', fieldValue.length > 0 ? fieldValue.map(function(item) { return item.id }) : '');
    });
});

