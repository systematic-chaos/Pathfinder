package corp.katet.pathfinder.cameraoverlay;

import java.util.List;

import com.google.android.gms.maps.model.LatLng;

import corp.katet.pathfinder.EarthGeometry;
import corp.katet.pathfinder.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;

public class CameraFragment extends Fragment {

	private Camera mCamera;
	private CameraPreview mCameraPreview;
	private Location mFromLocation, mToLocation;
	private float mAzimuth;

	private MyGLSurfaceView mGLSurfaceView;

	private View mView;

	private static final String STATE_FROM_LOCATION = "FROM_LOCATION";
	private static final String STATE_TO_LOCATION = "TO_LOCATION";

	static final float[] SHORT_COLOR = { 0f, 1f, 0f, 1f };
	static final float[] MEDIUM_COLOR = { 0f, 0f, 1f, 1f };
	static final float[] LONG_COLOR = { 1f, 0f, 0f, 1f };

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mView = inflater.inflate(R.layout.positional_camera, container, false);
		mView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT));

		restoreInstanceState(savedInstanceState);
		findPath();

		return mView;
	}

	@Override
	public void onResume() {
		super.onResume();
		initCameraPreview();
	}

	// ALWAYS remember to release the camera when you are finished
	@Override
	public void onPause() {
		releaseCamera();
		super.onPause();
	}

	// Check if this device has a camera
	private boolean cameraAvailable(Context context) {
		return context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA);
	}

	private Camera getCameraInstance() {
		Camera c = null;
		// Check whether camera exists
		if (cameraAvailable(getActivity())) {
			// Attempt to get a Camera instance
			try {
				c = Camera.open();
			} catch (Exception e) {
				// Camera is not available (in use)
			}
		}
		// Returns null if camera is unavailable
		return c;
	}

	private void initCameraPreview() {
		mGLSurfaceView = (MyGLSurfaceView) mView
				.findViewById(R.id.gl_surface_view);

		mCamera = getCameraInstance();

		// Camera may be in use by another activity of the system or not
		// available at all
		if (mCamera != null) {
			enableAutofocus(mCamera);
			mCameraPreview = (CameraPreview) mView
					.findViewById(R.id.camera_preview);
			mCameraPreview.init(mCamera, getActivity());
		}
	}

	@SuppressLint("InlinedApi")
	private void enableAutofocus(Camera c) {
		// Get Camera parameters
		Camera.Parameters params = c.getParameters();

		List<String> focusModes = params.getSupportedFocusModes();
		// Continuous video focus mode is supported
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
				&& focusModes
						.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
			params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			// Set Camera parameters
			c.setParameters(params);
		}
	}

	@Override
	public void onDestroyView() {
		mCameraPreview = null;
		mGLSurfaceView = null;
		mView = null;
		super.onDestroyView();
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCameraPreview.getHolder().removeCallback(mCameraPreview);
			mCamera.release();
			mCamera = null;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (mFromLocation != null) {
			outState.putParcelable(STATE_FROM_LOCATION, mFromLocation);
		}
		if (mToLocation != null) {
			outState.putParcelable(STATE_TO_LOCATION, mToLocation);
		}
	}

	public void restoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			setFromLocation((Location) savedInstanceState
					.getParcelable(STATE_FROM_LOCATION));
			setToLocation((Location) savedInstanceState
					.getParcelable(STATE_TO_LOCATION));
		}
	}

	public void setFromLocation(Location location) {
		if ((mFromLocation = location).hasAltitude()) {
			setFromPosition(location.getLatitude(), location.getLongitude(),
					location.getAltitude());
		} else {
			setFromPosition(location.getLatitude(), location.getLongitude());
		}

		if (mFromLocation != null && mToLocation != null) {
			findPath();
		}
	}

	private void setFromPosition(String position) {
		if (mView != null) {
			((TextView) mView.findViewById(R.id.from_position_text))
					.setText(position);
		}
	}

	private void setFromPosition(double latitude, double longitude) {
		setFromPosition(String.format("%.4f,  %.4f", latitude, longitude));
	}

	private void setFromPosition(double latitude, double longitude,
			double altitude) {
		setFromPosition(String.format("%.4f, %.4f    %.4f m", latitude,
				longitude, altitude));
	}

	public void setToLocation(Location location) {
		if ((mToLocation = location).hasAltitude()) {
			setToPosition(location.getLatitude(), location.getLongitude(),
					location.getAltitude());
		} else {
			setToPosition(location.getLatitude(), location.getLongitude());
		}

		if (mFromLocation != null && mToLocation != null) {
			findPath();
		}
	}

	private void setToPosition(String position) {
		if (mView != null) {
			((TextView) mView.findViewById(R.id.to_position_text))
					.setText(position);
		}
	}

	private void setToPosition(double latitude, double longitude) {
		setToPosition(String.format("%.4f,  %.4f", latitude, longitude));
	}

	private void setToPosition(double latitude, double longitude,
			double altitude) {
		setToPosition(String.format("%.4f, %.4f    %.4f m", latitude,
				longitude, altitude));
	}

	public void setOrientation(float azimuth) {
		setOrientation(String.format("%.4fº", mAzimuth = azimuth));

		updateAzimuth();
	}

	private void setOrientation(String orientation) {
		if (mView != null) {
			((TextView) mView.findViewById(R.id.orientation_text))
					.setText(orientation);
		}
	}

	private void setDistance(float distance) {
		setDistance(String.format("%.3f km", distance / 1000.));

		float[] color;
		if (distance <= EarthGeometry.SHORT_DISTANCE)
			color = SHORT_COLOR;
		else if (distance <= EarthGeometry.LONG_DISTANCE)
			color = MEDIUM_COLOR;
		else
			color = LONG_COLOR;

		mGLSurfaceView.setColor(color);
	}

	private void setDistance(String distance) {
		if (mView != null) {
			((TextView) mView.findViewById(R.id.distance_text))
					.setText(distance);
		}
	}

	private void updateAzimuth() {
		if (mGLSurfaceView != null) {
			mGLSurfaceView.setAngleZ(mAzimuth - (float) mBearing);
		}
	}

	private void setBearing(float bearing) {
		setBearing(String.format("%.4fº", bearing));

		updateAzimuth();
	}

	private void setBearing(String bearing) {
		if (mView != null) {
			((TextView) mView.findViewById(R.id.bearing_text)).setText(bearing);
		}
	}

	private void setVerticalBearing(float vBearing) {
		setVerticalBearing(String.format("%.4fº", vBearing));

		mGLSurfaceView.setAngleX(vBearing);
	}

	private void setVerticalBearing(String vBearing) {
		if (mView != null) {
			((TextView) mView.findViewById(R.id.vertical_bearing_text))
					.setText(vBearing);
		}
	}

	private double mHaversine, mDistance, mBearing = 0.;

	private void findPath() {
		if (mFromLocation != null && mFromLocation.hasAccuracy()
				&& mToLocation != null && mToLocation.hasAccuracy()) {
			LatLng fromLoc = new LatLng(mFromLocation.getLatitude(),
					mFromLocation.getLongitude());
			LatLng toLoc = new LatLng(mToLocation.getLatitude(),
					mToLocation.getLongitude());
			mHaversine = EarthGeometry.computeHaversine(fromLoc, toLoc);
			mBearing = EarthGeometry.computeBearing(fromLoc, toLoc);
			if (mFromLocation.hasAltitude() && mToLocation.hasAltitude()) {
				double height = EarthGeometry.computeHeightInc(mFromLocation,
						mToLocation);
				mDistance = EarthGeometry.computeDistance(mHaversine, height);
				double verticalBearing = EarthGeometry.computeVerticalBearing(
						mDistance, mHaversine, height);
				setVerticalBearing((float) verticalBearing);
			} else {
				mDistance = mHaversine;
			}
			setDistance((float) mDistance);
			setBearing((float) mBearing);
		}
	}
}
