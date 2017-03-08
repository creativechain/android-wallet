package crea.wallet.lite.widget.qrscanner;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import me.dm7.barcodescanner.core.CameraPreview;

/**
 * Created by ander on 30/08/15.
 */
public class PreviewCamera extends CameraPreview {

    public static final String TAG = "PreviewCamera";

    private boolean isSurfaceCreated = false;
    private boolean setAutoFocus = false;
    public PreviewCamera(Context context) {
        super(context);
    }

    public PreviewCamera(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        super.surfaceCreated(surfaceHolder);
        isSurfaceCreated = true;
        Log.v(TAG, "Surface was created.");
        if(setAutoFocus) {
            setAutoFocus(true);
            Log.v(TAG, "Enabling autofocus...");
        }
    }

    @Override
    public void setAutoFocus(boolean state) {
        if (isSurfaceCreated) {
            super.setAutoFocus(state);
            return;
        }

        setAutoFocus = state;
    }
}
