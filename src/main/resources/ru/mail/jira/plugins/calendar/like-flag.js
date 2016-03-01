define('calendar/like-flag', ['jquery', 'aui/flag'], function($, flag) {
    var likeFlagQuestion = '' +
        '<p>Вам нравится наш плагин?</p>' +
        '<ul class="aui-nav-actions-list">' +
        '    <li><button id="mailrucalendar-like-flag-yes" class="aui-button aui-button-link"><span class="aui-icon aui-icon-small aui-iconfont-like"></span>Yes</button></li>' +
        '    <li><button id="mailrucalendar-like-flag-no" class="aui-button aui-button-link"><span class="aui-icon aui-icon-small aui-iconfont-like flip-vertical"></span>No</button></li>' +
        '</ul>';

    var likeFlagLike = '' +
        '<p>Оставьте отзыв на <a href="https://marketplace.atlassian.com/plugins/ru.mail.jira.plugins.mailrucal/server/reviews" target="_blank">Marketplace</a>.</p>';

    var likeFlagDislike = '' +
        '<p>Спасибо за отзыв. Напишите что нам нужно исправить. Или что-то в этом роде.</p>';

    return Backbone.View.extend({
        events: {
            'click #mailrucalendar-like-flag-yes': '_like',
            'click #mailrucalendar-like-flag-no': '_dislike',
            'aui-flag-close': '_onClose'
        },
        initialize: function() {
            this.setElement(flag({
                type: 'info',
                title: 'Оцените нашу работу',
                persistent: false,
                body: '<div class="mailrucalendar-like-flag-container"></div>'
            }));
            this.$likeMsg = this.$('.mailrucalendar-like-flag-container');

            this.$el.addClass('mailrucalendar-like-flag');
            this.$likeMsg.html(likeFlagQuestion);
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
        _like: function() {
            _tmr.push({id: "2706504", type: "reachGoal", goal: "like"});
            this.$likeMsg.html(likeFlagLike);
        },
        _dislike: function() {
            _tmr.push({id: "2706504", type: "reachGoal", goal: "dislike"});
            this.$likeMsg.html(likeFlagDislike);
        }
    });
});
