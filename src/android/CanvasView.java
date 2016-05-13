package com.affinitybridge.websocketserver;

import android.animation.TimeAnimator;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Point;
import android.support.annotation.UiThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.koushikdutta.async.callback.CompletedCallback;
//import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONArray;
import org.json.JSONException;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONObject;

import java.net.UnknownHostException;
import java.util.HashMap;

public class CanvasView extends CordovaPlugin {
    private static String TAG = CanvasView.class.getName();

    private static int PORT = 8887;

    private static String ACTION_CREATE_VIEW = "createView";

    private Server server;
//    AsyncHttpServer server = new AsyncHttpServer();

    private HashMap<String, View> views = new HashMap<String, View>();

    private HashMap<Long, String> canvases = new HashMap<Long, String>();

    private long ids = 0;

    @Override
    public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
        super.initialize(cordova, webView);
        createTestView();
        //WebSocketImpl.DEBUG = true;

        try {
            this.server = new Server(PORT);
            this.server.addConnectionListener(new Server.ConnectionListener() {
                @Override
                public void onOpen(final WebSocket conn, ClientHandshake handshake) {
                    Log.i(TAG, "Client connected.");
                }

                @Override
                public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                    Log.i(TAG, "Client disconnected.");
                }

                @Override
                public void onMessage(final WebSocket conn, String message) {
                    try {
                        JSONObject params = new JSONObject(message);
                        long id = params.getLong("id");
                        startRendering(conn, id);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON message: " + e.getMessage());
                    }
                }

                @Override
                public void onError(WebSocket conn, Exception e) {}
            });
            this.server.start();
        } catch (UnknownHostException e) {
            Log.e(TAG, e.getMessage());
        }

//        server.websocket("/", new AsyncHttpServer.WebSocketRequestCallback() {
//            @Override
//            public void onConnected(final WebSocket webSocket, AsyncHttpServerRequest request) {
//                final Snapper snapper = SnapshotUtil.getSnapper(view);
//                FrameManager.setSnapper(snapper);
//                cordova.getActivity().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        startRendering(new TimeAnimator.TimeListener() {
//                            @Override
//                            public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
//                                FrameManager.getViewSnapshot(snapper, webSocket);
//                            }
//                        });
//                    }
//                });
//            }
//        });
//
//        server.listen(8887);
    }

    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals(ACTION_CREATE_VIEW)) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject response = createView(args.getJSONObject(0));
                        callbackContext.success(response);
                    } catch (JSONException e) {
                        callbackContext.error(e.getMessage());
                    } catch (Exception e) {
                        callbackContext.error(e.getMessage());
                    }
                }
            });
            return true;
        }
        return false;
    }

    private void startRendering(final WebSocket conn, long id) {
        String type = canvases.get(id);
        View view = views.get(type);

        final Snapper snapper = SnapshotUtil.getSnapper(view);
        FrameManager.setSnapper(snapper);
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startRendering(new TimeAnimator.TimeListener() {
                    @Override
                    public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
                        FrameManager.getViewSnapshot(snapper, conn);
                    }
                });
            }
        });
    }

    public void registerView(String name, View view) {
        views.put(name, view);
    }

    protected void createTestView() {
        final MapView mapView = new MapView(webView.getContext(), new MapboxMapOptions()
                .accessToken("pk.eyJ1IjoiYWZmaW5pdHlicmlkZ2UiLCJhIjoicW4wNkNXdyJ9.KyFVX7DUqVXDdSOwYnLm5Q")
                .styleUrl(Style.MAPBOX_STREETS)
                .camera(new CameraPosition.Builder().target(new LatLng(0.0, 0.0)).zoom(5).build()));
        mapView.onResume();
        mapView.onCreate(null);
        registerView("test", mapView);
    }

    @UiThread
    public JSONObject createView(JSONObject options) throws JSONException, Exception {
        final int width = options.getInt("width");
        final int height = options.getInt("height");
        final String type = options.getString("type");
        final boolean debug = options.optBoolean("debug", false);
        final long id = ids++;

        if (!views.containsKey(type)) {
            throw new Exception("Unknown Canvas View type: " + type);
        }
        View view = views.get(type);
        canvases.put(id, type);

        if (debug && view.getParent() == null) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
            params.setMargins(0, height + 10, 0, 0);
            view.setLayoutParams(params);

            final FrameLayout layout = (FrameLayout) webView.getView().getParent();
            layout.addView(view);
        }

        return new JSONObject()
                .put("id", id)
                .put("port", PORT);
    }

    @UiThread
    public void startRendering(final TimeAnimator.TimeListener onFrame) {
        final TimeAnimator animator = new TimeAnimator();
        animator.setTimeListener(onFrame);
        animator.start();
    }
}