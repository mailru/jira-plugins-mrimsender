define(
    'calendar/preferences',
    [],
    function() {
        var InMemoryStorage = function() {
            this._data = {};
        };

        InMemoryStorage.prototype.setItem = function(key, value) {
            this._data[key] = value ? value.toString() : "";
        };

        InMemoryStorage.prototype.getItem = function(key) {
            return this._data[key];
        };

        InMemoryStorage.prototype.removeItem = function(key) {
            return this._data[key] = null;
        };

        var Preferences = function() {
            this._fallBackStorage = new InMemoryStorage();
            if (localStorage) {
                this._storage = localStorage;
            } else {
                this._storage = this._fallBackStorage;
            }
        };

        Preferences.prototype.setItem = function(key, value) {
            try {
                this._storage.setItem(key, value);
            } catch (err) {
                console.error("unable to update preference \"" + key + "\": \n", err)
            }
            this._fallBackStorage.setItem(key, value);
        };

        Preferences.prototype.getItem = function(key) {
            return this._fallBackStorage.getItem(key) || this._storage.getItem(key);
        };

        return new Preferences();
    }
);