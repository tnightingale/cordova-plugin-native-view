importScripts('pako_inflate.js'); 

var initialized = false,
    id, data;

onmessage = function (e) {
    id = e.data.id;

    if (data) {
        var START = self.performance.now();
        var typedArray = new Uint8ClampedArray(pako.inflate(data).buffer);
        var MARK = self.performance.now();
        e.data.frame.data.set(typedArray);
        var END = self.performance.now();

        e.data.timing.inflate = MARK - START;
        e.data.timing.copy = END - MARK;
        // console.log("Deflated length: " + data.byteLength + ", Inflated length: " + typedArray.length);
    }

    postMessage(e.data, [e.data.frame.data.buffer]);
};


// TODO: Read address from first message.
var connection = new WebSocket('ws://localhost:8887');

connection.binaryType = 'arraybuffer';

connection.onopen = function () {
    connection.send(JSON.stringify({id: id}));
};

// Log errors
connection.onerror = function (error) {
    console.log('WebSocket Error ' + error);
};

// Log messages from the server
connection.onmessage = function (e) {
    if (e.data instanceof ArrayBuffer) {
        data = e.data;
    }
    else {
        try {
            var json = JSON.parse(e.data);
            console.log('Message:', json);
        } catch (error) {
            console.error(error);
        }
    }
};
