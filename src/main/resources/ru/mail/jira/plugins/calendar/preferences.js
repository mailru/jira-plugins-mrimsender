define(
    'calendar/preferences',
    [],
    function() {
        var InMemoryStorage = function() {
            this._data = {};
        };

        InMemoryStorage.prototype.setItem = function(key, value) {
            this._data[key] = value;
        };

        InMemoryStorage.prototype.getItem = function(key) {
            return this._data[key];
        };

        InMemoryStorage.prototype.removeItem = function(key) {
            return this._data[key] = null;
        };

        var Preferences = function() {
            if (localStorage) {
                this._storage = localStorage;
            } else {
                this._storage = new InMemoryStorage();
            }
        };

        Preferences.prototype.set = function(key, value) {
            this._storage.setItem(key, value);
        };

        Preferences.prototype.get = function(key) {
            return this._storage.getItem(key);
        };

        return new Preferences();
    }
);