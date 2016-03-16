define('calendar/like-flag', ['jquery', 'underscore', 'backbone', 'aui/flag'], function($, _, Backbone, flag) {
    var likeFlagLike = '' +
        '<p>Может еще оставите отзыв?</p>' +
        '<ul class="aui-nav-actions-list">' +
        '   <li>' +
        '      <button id="mailrucalendar-like-flag-no-review" class="aui-button aui-button-link mailrucalendar-like-flag-close">No, thanks</button>' +
        '   </li>' +
        '   <li>' +
        '      <a href="https://marketplace.atlassian.com/plugins/ru.mail.jira.plugins.mailrucal/server/reviews" target="_blank" class="aui-button aui-button-link mailrucalendar-like-flag-close">Ok, sure</a>' +
        '   </li>' +
        '</ul>';

    var likeFlagDislike = '' +
        '<p>Расскажите, что не так?</p>' +
        '<ul class="aui-nav-actions-list">' +
        '   <li>' +
        '      <button id="mailrucalendar-like-flag-no-review" class="aui-button aui-button-link mailrucalendar-like-flag-close">No, thanks</button>' +
        '   </li>' +
        '   <li>' +
        '      <a href="https://github.com/mailru/jira-plugins-mailrucal/issues" target="_blank" class="aui-button aui-button-link mailrucalendar-like-flag-close">Ok, sure</a>' +
        '   </li>' +
        '</ul>';

    return Backbone.View.extend({
        events: {
            'click #mailrucalendar-like-flag-yes': '_like',
            'click #mailrucalendar-like-flag-no': '_dislike',
            'click .mailrucalendar-like-flag-close': '_onClose',
            'change .mailrucalendar-like-flag-rating input[name=rating]': '_rate',
            'aui-flag-close': '_onClose'
        },
        initialize: function() {
            this.setElement(flag({
                type: 'info',
                title: 'Нравится Mail.Ru Calendar?',
                body: '' +
                '<div class="mailrucalendar-like-flag-container">' +
                '   <p>Оцените нас на Atlassian Marketplace:</p>' +
                '   <div class="mailrucalendar-like-flag-rating">' +
                '       <input type="radio" id="star4" name="rating" value="4" /><label for="star4"></label>' +
                '       <input type="radio" id="star3" name="rating" value="3" /><label for="star3"></label>' +
                '       <input type="radio" id="star2" name="rating" value="2" /><label for="star2"></label>' +
                '       <input type="radio" id="star1" name="rating" value="1" /><label for="star1"></label>' +
                '   </div>' +
                '</div>' +
                '<div class="mailrucalendar-like-flag-buttons">' +
                '</div>'
            }));

            this.$el.addClass('mailrucalendar-like-flag');
            this.$ratingContainer = this.$('.mailrucalendar-like-flag-buttons');
        },
        _onClose: function() {
            if (this.$('#mailrucalendar-like-flag-yes').length)
                _tmr.push({id: "2706504", type: "reachGoal", goal: "like-close"});
            $.ajax({
                type: 'PUT',
                url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference/likeFlagShown'
            });
            this.remove();
        },
        _rate: function(e) {
            var rating = $(e.target).val();
            if (rating >= 3)
                this._like();
            else
                this._dislike();

            $.ajax({
                type: 'POST',
                contentType: 'application/json',
                url: AJS.contextPath() + '/rest/plugins/1.0/available/ru.mail.jira.plugins.mailrucal-key/review',
                data: JSON.stringify({ratingOnly: true, stars: rating, review: null})
            });
        },
        _like: function() {
            _tmr.push({id: "2706504", type: "reachGoal", goal: "like"});
            this.$('.mailrucalendar-like-flag-buttons').html($(likeFlagLike)).hide().fadeIn();
        },
        _dislike: function() {
            _tmr.push({id: "2706504", type: "reachGoal", goal: "dislike"});
            this.$('.mailrucalendar-like-flag-buttons').html($(likeFlagDislike)).hide().fadeIn();
        }
    });
});
