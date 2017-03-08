package crea.wallet.lite.widget.qrscanner;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import me.dm7.barcodescanner.core.CameraUtils;
import me.dm7.barcodescanner.core.ViewFinderView;

/**
 * Created by ander on 31/08/15.
 */
public abstract class AbstractScannerView extends FrameLayout implements PreviewCallback {
    private Camera mCamera;
    private PreviewCamera mPreview;
    private ViewFinderView mViewFinderView;
    private Rect mFramingRectInPreview;

    public AbstractScannerView(Context context) {
        super(context);
        this.setupLayout();
    }

    public AbstractScannerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.setupLayout();
    }

    public void setupLayout() {
        this.mPreview = new PreviewCamera(this.getContext());
        this.mViewFinderView = new ViewFinderView(this.getContext());
        RelativeLayout relativeLayout = new RelativeLayout(this.getContext());
        relativeLayout.setGravity(17);
        relativeLayout.setBackgroundColor(Color.WHITE);
        relativeLayout.addView(this.mPreview);
        this.addView(relativeLayout);
        this.addView(this.mViewFinderView);
    }

    public void startCamera(int cameraId) {
        this.startCamera(CameraUtils.getCameraInstance(cameraId));
    }

    public void startCamera(Camera camera) {
        this.mCamera = camera;
        if(this.mCamera != null) {
            this.mViewFinderView.setupViewFinder();
            this.mPreview.setCamera(this.mCamera, this);
            this.mPreview.initCameraPreview();
        }

    }

    public void startCamera() {
        this.startCamera(CameraUtils.getCameraInstance());
    }

    public void stopCamera() {
        if(this.mCamera != null) {
            this.mPreview.stopCameraPreview();
            this.mPreview.setCamera(null, null);
            this.mCamera.release();
            this.mCamera = null;
        }

    }

    public synchronized Rect getFramingRectInPreview(int previewWidth, int previewHeight) {
        if(this.mFramingRectInPreview == null) {
            Rect framingRect = this.mViewFinderView.getFramingRect();
            int viewFinderViewWidth = this.mViewFinderView.getWidth();
            int viewFinderViewHeight = this.mViewFinderView.getHeight();
            if(framingRect == null || viewFinderViewWidth == 0 || viewFinderViewHeight == 0) {
                return null;
            }

            Rect rect = new Rect(framingRect);
            rect.left = rect.left * previewWidth / viewFinderViewWidth;
            rect.right = rect.right * previewWidth / viewFinderViewWidth;
            rect.top = rect.top * previewHeight / viewFinderViewHeight;
            rect.bottom = rect.bottom * previewHeight / viewFinderViewHeight;
            this.mFramingRectInPreview = rect;
        }

        return this.mFramingRectInPreview;
    }

    public void setFlash(boolean flag) {
        if(this.mCamera != null && CameraUtils.isFlashSupported(this.mCamera)) {
            Camera.Parameters parameters = this.mCamera.getParameters();
            if(flag) {
                if(parameters.getFlashMode().equals("torch")) {
                    return;
                }

                parameters.setFlashMode("torch");
            } else {
                if(parameters.getFlashMode().equals("off")) {
                    return;
                }

                parameters.setFlashMode("off");
            }

            this.mCamera.setParameters(parameters);
        }

    }

    public boolean getFlash() {
        if(this.mCamera != null && CameraUtils.isFlashSupported(this.mCamera)) {
            Camera.Parameters parameters = this.mCamera.getParameters();
            return parameters.getFlashMode().equals("torch");
        } else {
            return false;
        }
    }

    public void toggleFlash() {
        if(this.mCamera != null && CameraUtils.isFlashSupported(this.mCamera)) {
            Camera.Parameters parameters = this.mCamera.getParameters();
            if(parameters.getFlashMode().equals("torch")) {
                parameters.setFlashMode("off");
            } else {
                parameters.setFlashMode("torch");
            }

            this.mCamera.setParameters(parameters);
        }

    }

    public void setAutoFocus(boolean state) {
        if(this.mPreview != null) {
            this.mPreview.setAutoFocus(state);
        }

    }
}
