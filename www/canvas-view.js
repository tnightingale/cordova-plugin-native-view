var PluginAPI = require("./plugin-api-mixin"),
    enableTouch = require("./touch");

function View(container, type, debug) {
    this._initCanvas(container, type);
    this.pluginInit({
        width: this._canvas.width,
        height: this._canvas.height,
        type: type,
        debug: debug || false
    });
    this.once('load', this._onLoad.bind(this));
}
PluginAPI('CanvasView', 'View', View.prototype);

View.prototype._initCanvas = function _initCanvas(container, type) {
    this._container = container;
    this._type = type;
    this._canvas = document.createElement('canvas');
    this._c = this._canvas.getContext('2d');
    this._rect = this._container.getBoundingClientRect();

    this._canvas.width = this._rect.width * window.devicePixelRatio;
    this._canvas.height = this._rect.height * window.devicePixelRatio;
    this._canvas.style.width = this._rect.width + "px";
    this._canvas.style.height = this._rect.height + "px";

    enableTouch(this._canvas);

    this._container.appendChild(this._canvas);

    this._container.dataset.canvasViewLoaded = true;
};

View.prototype._onLoad = function _onLoad() {
    var worker = new Worker('canvas-view/worker.js'),
        dirty = false,
        c = this._c,
        id = this._id,
        imageData = c.createImageData(this._canvas.width, this._canvas.height);

    worker.addEventListener('message', function (e) {
        imageData = e.data.frame;

        var MARK = window.performance.now();
        c.putImageData(imageData, 0, 0);
        dirty = false;
        var END = window.performance.now();

        e.data.timing.draw = END - MARK;
        e.data.timing.total = END - e.data.START;

        // console.log('Inflate: ' + e.data.inflateTime + 'ms, Copy: ' + e.data.copyTime + 'ms, Draw: ' + drawTime + 'ms, Total: ' + time + 'ms.');
    });

    window.requestAnimationFrame(step);
    function step(timestamp) {
        if (!dirty) {
            dirty = true;
            worker.postMessage({
                id: id,
                timing: {start: window.performance.now()},
                frame: imageData
            }, [imageData.data.buffer]);
        }
        window.requestAnimationFrame(step);
    }
};

function initViews() {
    var containers = document.querySelectorAll("[data-canvas-view]"),
        i, container, type, debug;

    for (i = 0; i < containers.length; ++i) {
        container = containers[i];

        type = container.dataset.canvasView;
        delete container.dataset.canvasView;
        debug = container.dataset.debug || false;
        delete container.dataset.debug;

        new View(containers[i], type, debug);
    }
}

initViews();

module.exports = {
    View: View,
    init: initViews
};
