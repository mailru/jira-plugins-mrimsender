define('calendar/feedback-flag', ['jquery', 'underscore', 'backbone'], function($, _, Backbone) {
    var likeFlagLike = AJS.format(AJS.I18n.getText('ru.mail.jira.plugins.calendar.liker.thanksAndReview'), '<a href="https://marketplace.atlassian.com/plugins/ru.mail.jira.plugins.mailrucal/server/reviews" target="_blank" class="mailrucalendar-feedback-flag-close">', '</a>');
    var likeFlagOther = AJS.format(AJS.I18n.getText('ru.mail.jira.plugins.calendar.liker.badRateAndReview'), '<a href="https://github.com/mailru/jira-plugins-mailrucal/issues" target="_blank" class="mailrucalendar-feedback-flag-close">', '</a>');
    var flag;
    try {
        flag = require('aui/flag');
    } catch (e) {
    }

    //todo for testing
    Backbone.View.LikeFlag = Backbone.View.extend({
        events: {
            'click .mailrucalendar-feedback-flag-close': '_onClose',
            'change .mailrucalendar-feedback-flag-rating input[name=rating]': '_rate',
            'aui-flag-close': '_onClose'
        },
        initialize: function() {
            this.setElement(flag({
                type: 'info',
                title: AJS.I18n.getText('ru.mail.jira.plugins.calendar.liker.title'),
                body: '' +
                '<p class="mailru-calendar-feedback-flag-description">' + AJS.I18n.getText('ru.mail.jira.plugins.calendar.liker.rateUs') + '</p>' +
                '<div class="mailrucalendar-feedback-flag-rating">' +
                '   <input type="radio" id="star4" name="rating" value="4" /><label for="star4" class="aui-icon aui-icon-large aui-iconfont-star"></label>' +
                '   <input type="radio" id="star3" name="rating" value="3" /><label for="star3" class="aui-icon aui-icon-large aui-iconfont-star"></label>' +
                '   <input type="radio" id="star2" name="rating" value="2" /><label for="star2" class="aui-icon aui-icon-large aui-iconfont-star"></label>' +
                '   <input type="radio" id="star1" name="rating" value="1" /><label for="star1" class="aui-icon aui-icon-large aui-iconfont-star"></label>' +
                '</div>'
            }));

            this.$el.addClass('mailrucalendar-feedback-flag');
        },
        _onClose: function() {
            $.ajax({
                type: 'PUT',
                url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference/likeFlagShown/' + !this.$('.mailrucalendar-feedback-flag-rating').length
            });
            this.remove();
        },
        _rate: function(e) {
            var rating = $(e.target).val();
            if (rating == 4) {
                $.ajax({
                    type: 'POST',
                    contentType: 'application/json',
                    url: AJS.contextPath() + '/rest/plugins/1.0/available/ru.mail.jira.plugins.mailrucal-key/review',
                    data: JSON.stringify({ratingOnly: true, stars: rating, review: null})
                });
                this.$('.title strong').text(AJS.I18n.getText('ru.mail.jira.plugins.calendar.liker.thanksAndReview.title'));
                this.$('.mailrucalendar-feedback-flag-rating').hide();
                this.$('.mailru-calendar-feedback-flag-description').hide().html(likeFlagLike).fadeIn();
            } else {
                this.$('.title strong').text(AJS.I18n.getText('ru.mail.jira.plugins.calendar.liker.badRateAndReview.title'));
                this.$('.mailrucalendar-feedback-flag-rating').hide();
                this.$('.mailru-calendar-feedback-flag-description').hide().html(likeFlagOther).fadeIn();
            }
            _tmr.push({id: '2706504', type: 'reachGoal', goal: 'rating-' + rating});
        }
    });
    return Backbone.View.LikeFlag;
});
