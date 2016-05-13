module.exports = function (canvas) {
    var c = canvas.getContext('2d'),
        ongoingTouches = [];

    canvas.addEventListener("touchstart", handleStart, false);
    canvas.addEventListener("touchend", handleEnd, false);
    canvas.addEventListener("touchcancel", handleCancel, false);
    canvas.addEventListener("touchmove", handleMove, false);

    function handleStart(e) {
        e.preventDefault();

        console.log("touchstart.");

        var touches = e.changedTouches;

        for (var i = 0; i < touches.length; i++) {
            console.log("touchstart:" + i + "...");
            var touch = copyTouch(touches[i]);
            ongoingTouches.push(touch);
            var color = colorForTouch(touch);
            c.beginPath();
            c.arc(touch.pageX, touch.pageY, 4, 0, 2 * Math.PI, false);  // a circle at the start
            c.fillStyle = color;
            c.fill();
            console.log("touchstart:" + i + ".");
        }
    }

    function handleEnd(e) {
        e.preventDefault();

        console.log("touchend");

        var touches = e.changedTouches;

        for (var i = 0; i < touches.length; i++) {
            var touch = scaleTouch(touches[i]);
            var color = colorForTouch(touch);
            var idx = ongoingTouchIndexById(touch.identifier);

            if (idx >= 0) {
                c.lineWidth = 4;
                c.fillStyle = color;
                c.beginPath();
                c.moveTo(ongoingTouches[idx].pageX, ongoingTouches[idx].pageY);
                c.lineTo(touch.pageX, touch.pageY);
                c.fillRect(touch.pageX - 4, touch.pageY - 4, 8, 8);  // and a square at the end
                ongoingTouches.splice(idx, 1);  // remove it; we're done
            } else {
                console.log("can't figure out which touch to end");
            }
        }
    }

    function handleCancel(e) {
        e.preventDefault();

        console.log("touchcancel.");

        var touches = e.changedTouches;

        for (var i = 0; i < touches.length; i++) {
            ongoingTouches.splice(i, 1);  // remove it; we're done
        }
    }

    function handleMove(e) {
        e.preventDefault();

        var touches = e.changedTouches;

        for (var i = 0; i < touches.length; i++) {
            var touch = copyTouch(touches[i]);
            var color = colorForTouch(touch);
            var idx = ongoingTouchIndexById(touch.identifier);

            if (idx >= 0) {
                console.log("continuing touch "+idx);
                c.beginPath();
                console.log("c.moveTo(" + ongoingTouches[idx].pageX + ", " + ongoingTouches[idx].pageY + ");");
                c.moveTo(ongoingTouches[idx].pageX, ongoingTouches[idx].pageY);
                console.log("c.lineTo(" + touch.pageX + ", " + touch.pageY + ");");
                c.lineTo(touch.pageX, touch.pageY);
                c.lineWidth = 4;
                c.strokeStyle = color;
                c.stroke();

                ongoingTouches.splice(idx, 1, touch);  // swap in the new touch record
                console.log(".");
            } else {
                console.log("can't figure out which touch to continue");
            }
        }
    }

    function colorForTouch(touch) {
        var r = touch.identifier % 16;
        var g = Math.floor(touch.identifier / 3) % 16;
        var b = Math.floor(touch.identifier / 7) % 16;
        r = r.toString(16); // make it a hex digit
        g = g.toString(16); // make it a hex digit
        b = b.toString(16); // make it a hex digit
        var color = "#" + r + g + b;
        console.log("color for touch with identifier " + touch.identifier + " = " + color);
        return color;
    }

    function copyTouch(touch) {
        return scaleTouch({ identifier: touch.identifier, pageX: touch.pageX, pageY: touch.pageY });
    }

    function scaleTouch(touch) {
        touch.pageX = touch.pageX * window.devicePixelRatio;
        touch.pageY = touch.pageY * window.devicePixelRatio;
        return touch;
    }

    function ongoingTouchIndexById(idToFind) {
        for (var i = 0; i < ongoingTouches.length; i++) {
            var id = ongoingTouches[i].identifier;

            if (id == idToFind) {
                return i;
            }
        }
        return -1;    // not found
    }
};
