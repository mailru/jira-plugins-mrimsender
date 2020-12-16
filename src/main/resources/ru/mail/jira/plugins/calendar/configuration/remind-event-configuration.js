require(['jquery'], function($){
    AJS.toInit(function() {
        $.getJSON(AJS.contextPath() + '/rest/mailrucalendar/1.0/configuration/remindEvent', function(eventId) {
            $('#eventId').find('option[value='+ eventId +']').attr('selected', 'selected');
        });

        $('#save').on('click', function() {
            $.ajax({
                type: 'POST',
                url: AJS.contextPath() + '/rest/mailrucalendar/1.0/configuration/remindEvent/' + $('#eventId').val()
            });
        });
    });
});


