require(['jquery'], function($) {
    AJS.toInit(function () {
        $(".mrimsender-create-issue-excluding-projects").auiSelect2({
            placeholder:AJS.I18n.getText('ru.mail.jira.plugins.mrimsender.configuration.excludedProjects.placeholder'),
            allowClear: true
        });
    });
    // mapping value of project inside hidden input tag
    $(document).on('change', '.mrimsender-create-issue-excluding-projects', function() {
        var fieldValue = $(this).auiSelect2('data');
        $('#mrimsender-create-issue-excluding-projects-ids').attr('value', fieldValue.length > 0 ? fieldValue.map(function(item) { return item.id }) : '');
    });
});

