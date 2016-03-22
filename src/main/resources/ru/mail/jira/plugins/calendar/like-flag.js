define('calendar/like-flag', ['jquery', 'underscore', 'backbone', 'aui/flag'], function($, _, Backbone, flag) {
    var likeFlagLike = '' +
        '<p>'
        + AJS.format(AJS.I18n.getText('ru.mail.jira.plugins.calendar.liker.thanksAndReview'), '<a href="https://marketplace.atlassian.com/plugins/ru.mail.jira.plugins.mailrucal/server/reviews" target="_blank" class="aui-button aui-button-link mailrucalendar-like-flag-close">', '</a>') +
        '</p>';

    return Backbone.View.extend({
        events: {
            'click .mailrucalendar-like-flag-close': '_onClose',
            'change .mailrucalendar-like-flag-rating input[name=rating]': '_rate',
            'aui-flag-close': '_onClose'
        },
        initialize: function() {
            this.setElement(flag({
                type: 'info',
                title: AJS.I18n.getText('ru.mail.jira.plugins.calendar.liker.title'),
                body: '' +
                '<div class="mailrucalendar-like-flag-container">' +
                '   <p>' + AJS.I18n.getText('ru.mail.jira.plugins.calendar.liker.rateUs') + '</p>' +
                '   <div class="mailrucalendar-like-flag-rating">' +
                '       <input type="radio" id="star4" name="rating" value="4" /><label for="star4" class="aui-icon aui-icon-large aui-iconfont-star"></label>' +
                '       <input type="radio" id="star3" name="rating" value="3" /><label for="star3" class="aui-icon aui-icon-large aui-iconfont-star"></label>' +
                '       <input type="radio" id="star2" name="rating" value="2" /><label for="star2" class="aui-icon aui-icon-large aui-iconfont-star"></label>' +
                '       <input type="radio" id="star1" name="rating" value="1" /><label for="star1" class="aui-icon aui-icon-large aui-iconfont-star"></label>' +
                '   </div>' +
                '</div>'
            }));

            this.$el.addClass('mailrucalendar-like-flag');
        },
        _onClose: function() {
            $.ajax({
                type: 'PUT',
                url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference/likeFlagShown/' + !this.$('.mailrucalendar-like-flag-rating').length
            });
            this.remove();
        },
        _rate: function(e) {
            var rating = $(e.target).val();
            $.ajax({
                type: 'POST',
                contentType: 'application/json',
                url: AJS.contextPath() + '/rest/plugins/1.0/available/ru.mail.jira.plugins.mailrucal-key/review',
                data: JSON.stringify({ratingOnly: true, stars: rating, review: null})
            });
            _tmr.push({id: '2706504', type: 'reachGoal', goal: 'rating-' + rating});
            this.$('.mailrucalendar-like-flag-container').hide().html(likeFlagLike).fadeIn();
        }
    });
});
