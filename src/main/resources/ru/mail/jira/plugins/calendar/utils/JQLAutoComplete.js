define('calendar/jql-auto-complete-utils', ['jquery', 'underscore', 'backbone', 'jira/util/forms', 'jira/jql/jql-parser', 'jira/autocomplete/jql-autocomplete'], function($, _, Backbone, Forms, JQLParser, JQLAutoComplete) {
    var autoCompleteData;
    AJS.toInit(function() {
        $.ajax({
            type: 'GET',
            url: AJS.contextPath() + '/rest/api/2/jql/autocompletedata',
            success: function(data) {
                autoCompleteData = data;
            }
        });
    });

    return {
        initialize: function(fieldId, errorId) {
            console.log(fieldId);
            var field = AJS.$('#' + fieldId);
            var hasFocus = field.length > 0 && field[0] === document.activeElement;

            var jqlAutoComplete = JQLAutoComplete({
                fieldID: fieldId, //'jqltext',
                errorID: errorId, //'jqlerrormsg'
                parser: JQLAutoComplete.MyParser(autoCompleteData.jqlReservedWords || []),
                queryDelay: 0.65,
                jqlFieldNames: autoCompleteData.visibleFieldNames || [],
                jqlFunctionNames: autoCompleteData.visibleFunctionNames || [],
                minQueryLength: 0,
                allowArrowCarousel: true,
                autoSelectFirst: false,
                maxHeight: '195'
            });

            var jqlField = $('#' + fieldId);
            jqlField.unbind("keypress", Forms.submitOnEnter).keypress(function(e) {
                if (jqlAutoComplete.dropdownController === null || !jqlAutoComplete.dropdownController.displayed || jqlAutoComplete.selectedIndex < 0)
                    return true;
            });

            jqlAutoComplete.buildResponseContainer();
            if (jqlField !== undefined)
                jqlAutoComplete.parse(jqlField.text());
            jqlAutoComplete.updateColumnLineCount();

            $('.atlassian-autocomplete .suggestions').css('top', '68px');

            jqlField.click(function(){
                jqlAutoComplete.dropdownController.hideDropdown();
            });

            if (hasFocus) {
                field.select();
            }

            // keep reference around
            field.data('JqlAutoComplete', jqlAutoComplete);

        }
    }
});