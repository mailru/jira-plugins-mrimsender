require(['jquery'], function($) {
    AJS.toInit(function () {
        $(".mrimsender-create-issue-excluding-projects").auiSelect2({
            placeholder:AJS.I18n.getText('ru.mail.jira.plugins.myteam.configuration.excludedProjects.placeholder'),
            allowClear: true
        });
        $(".myteam-chat-creation-enabled-projects").auiSelect2({
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
    $(document).on('change', '.myteam-chat-creation-enabled-projects', function() {
        var fieldValue = $(this).auiSelect2('data');
        $('#myteam-chat-creation-enabled-project-ids').attr('value', fieldValue.length > 0 ? fieldValue.map(function(item) { return item.id }) : '');
    });
});

