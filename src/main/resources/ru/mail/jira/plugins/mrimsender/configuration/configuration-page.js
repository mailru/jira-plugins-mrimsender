require(['jquery'], function($) {
    AJS.toInit(function () {
        $(".tlgbot-additional-configurations-excluding-projects").auiSelect2({
            placeholder: AJS.I18n.getText('ru.mail.jira.plugins.tlgbot.additionalConfigurations.field.excludingProjects.placeholder'),
            allowClear: true
        });

        // mapping value of project inside hidden input tag
        $(document).on('change', '.tlgbot-additional-configurations-excluding-projects', function() {
            var fieldValue = $(this).auiSelect2('data');
            $('#mrimsender-create-issue-excluding-projects-ids').attr('value', fieldValue.length > 0 ? fieldValue.map(function(item) { return item.id }) : '');
        });
    });


});

