var cordova = require("cordova"),
    exec = require("cordova/exec"),
    Events = require('./events-mixin');
    Mixin = require('./mixin');

module.exports = function (service, type, target) {
    var _service = service;
    var _type = type;

    target = Events(_service, target);

    return Mixin({
        pluginInit: function (options) {
            var onLoad = _onLoad.bind(this),
                onError = _onError.bind(this);

            this.createStickyChannel("load");

            exec(onLoad, onError, service, "create" + type, [options]);

            function _onError(error) {
                try {
                    this.error(error);
                } catch (e) {
                    console.error(error);
                    this.fire("error", e);
                }
            }

            function _onLoad(resp) {
                this._id = resp.id;
                this.loaded = true;
                this.fire("load", this);
            }
        },

        registerCallback: function (name, success, fail) {
            var callbackId = [_service, _type, name, cordova.callbackId++].join('.');

            success = success ||  function () { console.log(callbackId + "() success!", arguments); };
            fail = fail ||  function () { console.log(callbackId + "() fail :(", arguments); };

            cordova.callbacks[callbackId] = {success: success, fail: fail};
            return callbackId;
        },

        error: function (err) {
            var error = new Error(_service + "Error (" + _type + ":" + this._id + "): " + err);
            throw error;
        },

        execAfterLoad: function () {
            var args = Array.prototype.slice.call(arguments),
                once = this.once.bind(this),
                onLoad = function () {
                    return this._exec.apply(this, args);
                }.bind(this);

            return new Promise(function (resolve, reject) {
                once('load', function (obj) {
                    onLoad().then(resolve, reject);
                });
            });
        },

        exec: function (callback, method, args) {
            args = [this._id].concat(args || []);
            callback = callback || function (err, response) {};
            return new Promise(function (resolve, reject) {
                exec(
                    function onSuccess(response) {
                        callback(null, response);
                        resolve(response);
                    },
                    function onError(error) {
                        callback(error);
                        reject(error);
                    },
                    _service, method, args
                );
            });
        }
    })(target);
};
