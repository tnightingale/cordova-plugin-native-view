package com.tnightingale.cordova.nativeview;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class SnapshotUtil {

    public static Snapper getSnapper(View rootView) {
        List<TextureView> views = getAllTextureViews(rootView);
        return new Snapper(views);
    }

    /*
     * The classic way of taking a screenshot (above) doesn't work with TextureView, this fixes it:
     * http://stackoverflow.com/questions/19704060/screen-capture-textureview-is-black-using-drawingcache
     */
    public static List<TextureView> getAllTextureViews(View view)
    {
        List<TextureView> tilingViews = new ArrayList<TextureView>();
        if (view instanceof TextureView) {
            tilingViews.add((TextureView)view);
        }  else if(view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup)view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                tilingViews.addAll(getAllTextureViews(viewGroup.getChildAt(i)));
            }
        }

        return tilingViews;
    }
}

class Snapper {
    private List<TextureView> tilingViews;
    private Canvas canvas;
    private Bitmap bitmap;

    protected Snapper(List<TextureView> views) {
        this.tilingViews = views;
        this.bitmap = Bitmap.createBitmap(tilingViews.get(0).getWidth(), tilingViews.get(0).getHeight(), Bitmap.Config.ARGB_8888);
        this.canvas = new Canvas(bitmap);
    }

    public int getByteCount() {
        return bitmap.getByteCount();
    }

    public Bitmap snap() {
        // Add the SurfaceView bit (see getAllTextureViews() below)
        if (tilingViews.size() > 0) {
            for (TextureView TextureView : tilingViews) {
                Bitmap b = TextureView.getBitmap(TextureView.getWidth(), TextureView.getHeight());
                int[] location = new int[2];
                TextureView.getLocationInWindow(location);
                int[] location2 = new int[2];
                TextureView.getLocationOnScreen(location2);
                canvas.drawBitmap(b, 0, 0, null);
            }
        }
//        return Bitmap.createBitmap(bitmap);
        return bitmap;
    }
}
