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

import java.net.UnknownHostException;

public class WebsocketServer extends CordovaPlugin {
    private static String TAG = WebsocketServer.class.getName();

    private Server server;
//    AsyncHttpServer server = new AsyncHttpServer();
    private View view;

    @Override
    public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
        super.initialize(cordova, webView);
        //WebSocketImpl.DEBUG = true;

        createView();

        try {
            final int port = 8887;
            this.server = new Server(port);
            this.server.addConnectionListener(new Server.ConnectionListener() {
                @Override
                public void onOpen(final WebSocket conn, ClientHandshake handshake) {
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

                @Override
                public void onClose(WebSocket conn, int code, String reason, boolean remote) {}

                @Override
                public void onMessage(final WebSocket conn, String message) {}

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
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        return false;
    }

    private float getRetinaFactor() {
        Activity activity = this.cordova.getActivity();
        Resources res = activity.getResources();
        DisplayMetrics metrics = res.getDisplayMetrics();
        return metrics.density;
    }

    public void createView() {
        WindowManager wm = cordova.getActivity().getWindowManager();
        Display display = wm.getDefaultDisplay();
        final Point windowDimensions = new Point();
        display.getSize(windowDimensions);

        float density = getRetinaFactor();
        final int width = (int) (500 * density); // windowDimensions.x;
        final int height = (int) (300 * density); //windowDimensions.y / 2;

        final MapView mapView = new MapView(webView.getContext(), new MapboxMapOptions()
                .accessToken("pk.eyJ1IjoiYWZmaW5pdHlicmlkZ2UiLCJhIjoicW4wNkNXdyJ9.KyFVX7DUqVXDdSOwYnLm5Q")
                .styleUrl(Style.MAPBOX_STREETS)
                .camera(new CameraPosition.Builder().target(new LatLng(0.0, 0.0)).zoom(5).build()));
        mapView.onResume();
        mapView.onCreate(null);
        view = mapView;

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        params.setMargins(0, height + 10, 0, 0);
        view.setLayoutParams(params);

        final FrameLayout layout = (FrameLayout) webView.getView().getParent();
        layout.addView(view);
    }

    @UiThread
    public void startRendering(final TimeAnimator.TimeListener onFrame) {
        final TimeAnimator animator = new TimeAnimator();
        animator.setTimeListener(onFrame);
        animator.start();
    }
}