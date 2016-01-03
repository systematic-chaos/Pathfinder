package corp.katet.pathfinder.cameraoverlay;

import java.io.IOException;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

// A basic Camera preview class
public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback {

	private Camera mCamera;
	private SurfaceHolder mHolder;

	private Activity mActivity;

	public CameraPreview(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public CameraPreview(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CameraPreview(Context context) {
		super(context);
	}

	public void init(Camera camera, Activity activity) {
		mCamera = camera;
		mActivity = activity;
		initSurfaceHolder();
	}

	@SuppressWarnings("deprecation")
	private void initSurfaceHolder() {
		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed
		mHolder = getHolder();
		mHolder.addCallback(this);
		// Deprecated setting, but required on Android versions prior to 3.0
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
	}

	@Override
	// The Surface has been created, now tell the camera where to draw the
	// preview
	public void surfaceCreated(SurfaceHolder holder) {
		initCamera(holder);
	}

	private void initCamera(SurfaceHolder holder) {
		try {
			if (mCamera != null) {
				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();
			}
		} catch (IOException e) {
			Log.d("Error setting camera preview", e.getMessage());
		}
	}

	@Override
	// Takes care of the events related to changes and rotation in the preview
	// The preview must be stopped before resizing or reformatting it
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// Preview surface does not exist
		if (mHolder.getSurface() == null) {
			return;
		}

		// Stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e) {
			// Ignore: tried to stop a non-existent preview
		}

		// Set preview size and make any resize, rotate or reformatting changes
		setCameraDisplayOrientation();
		setCameraDisplaySize(width, height);

		// Start preview with new settings
		try {
			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();
		} catch (Exception e) {
			Log.d(getClass().getName(),
					"Error starting camera preview: " + e.getMessage());
		}
	}

	@Override
	// Empty. The activity will take care of releasing the Camera preview
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	@SuppressLint("NewApi")
	private void setCameraDisplayOrientation() {
		int rotation = mActivity.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		int orientation = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			orientation = new Camera.CameraInfo().orientation;
		} else {
			switch (getResources().getConfiguration().orientation) {
			case Configuration.ORIENTATION_LANDSCAPE:
				orientation = Math.round(mCamera.getParameters()
						.getHorizontalViewAngle());
				break;
			case Configuration.ORIENTATION_PORTRAIT:
				orientation = Math.round(mCamera.getParameters()
						.getVerticalViewAngle());
			}
		}

		mCamera.setDisplayOrientation(Math.abs(orientation - degrees + 90) % 360);
	}

	private void setCameraDisplaySize(int width, int height) {
		if (height > width) {
			width = width ^ height;
			height = width ^ height;
			width = width ^ height;
		}
		Camera.Parameters parameters = mCamera.getParameters();
		List<Camera.Size> supportedPreviewSizes = parameters
				.getSupportedPreviewSizes();
		int sizeIndex;
		for (sizeIndex = 0; sizeIndex < supportedPreviewSizes.size()
				&& supportedPreviewSizes.get(sizeIndex).height <= height
				&& supportedPreviewSizes.get(sizeIndex).width <= width; sizeIndex++)
			;
		Camera.Size size = supportedPreviewSizes.get(sizeIndex - 1);
		parameters.setPreviewSize(size.width, size.height);
		mCamera.setParameters(parameters);
		requestLayout();
	}
}
