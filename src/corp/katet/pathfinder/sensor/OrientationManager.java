package corp.katet.pathfinder.sensor;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.view.Surface;

public class OrientationManager {

	public static interface OrientationListener {
		public void onOrientationChanged(float azimuth);
	}

	// Degree
	protected float mAzimuth = 0.f;

	protected Activity mParent;
	protected OrientationListener mCallback;

	protected SensorManager mSensorManager = null;
	private Sensor mRotationVector;
	private float[] orientation;
	private float[] rMat;
	private Sensor mOrientation;

	protected SensorEventListener mSensorEventListener;

	// Bool to track whether the user has turned location updates on or off
	protected boolean mRequestingOrientationUpdates = false;

	public OrientationManager(Activity parent, OrientationListener handler) {
		this(parent, handler, false);
	}

	public OrientationManager(Activity parent, OrientationListener handler,
			boolean requestOrientationUpdates) {
		mParent = parent;
		mCallback = handler;
		mSensorManager = (SensorManager) mParent
				.getSystemService(Context.SENSOR_SERVICE);

		setupSensorListener();

		// Check Android version
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			setupRotationVectorSensor();
		} else {
			setupLegacyOrientationSensor();
		}

		if (requestOrientationUpdates) {
			startOrientationUpdates();
		}
	}

	public boolean startOrientationUpdates() {
		if (!mRequestingOrientationUpdates) {
			mRequestingOrientationUpdates = mSensorManager
					.registerListener(
							mSensorEventListener,
							Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD ? mRotationVector
									: mOrientation,
							SensorManager.SENSOR_DELAY_NORMAL);
		}
		return mRequestingOrientationUpdates;
	}

	public boolean stopOrientationUpdates() {
		if (mRequestingOrientationUpdates) {
			mSensorManager.unregisterListener(mSensorEventListener);
			mRequestingOrientationUpdates = false;
		}
		return !mRequestingOrientationUpdates;
	}

	public boolean isRequestingOrientationUpdates() {
		return mRequestingOrientationUpdates;
	}

	public float getOrientation() {
		return mAzimuth;
	}

	protected void setupSensorListener() {
		mSensorEventListener = new SensorEventListener() {

			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}

			@SuppressLint("NewApi")
			@SuppressWarnings("deprecation")
			@Override
			public void onSensorChanged(SensorEvent event) {
				float[] auxMat = new float[9];
				switch (event.sensor.getType()) {
				case Sensor.TYPE_ROTATION_VECTOR:
					// Calculate the rotation matrix
					SensorManager.getRotationMatrixFromVector(auxMat,
							event.values);
					// Remap coordinate system depending on the device's
					// rotation
					switch (mParent.getWindowManager().getDefaultDisplay()
							.getRotation()) {
					case Surface.ROTATION_0:
						SensorManager.remapCoordinateSystem(auxMat,
								SensorManager.AXIS_X, SensorManager.AXIS_Y,
								rMat);
						break;
					case Surface.ROTATION_90:
						SensorManager.remapCoordinateSystem(auxMat,
								SensorManager.AXIS_Y,
								SensorManager.AXIS_MINUS_X, rMat);
						break;
					case Surface.ROTATION_180:
						SensorManager.remapCoordinateSystem(auxMat,
								SensorManager.AXIS_MINUS_X,
								SensorManager.AXIS_MINUS_Y, rMat);
						break;
					case Surface.ROTATION_270:
						SensorManager.remapCoordinateSystem(auxMat,
								SensorManager.AXIS_MINUS_Y,
								SensorManager.AXIS_X, rMat);
						break;
					}
					// Get the azimuth value (orientation[0]) in degrees
					mAzimuth = (float) (Math.toDegrees(SensorManager
							.getOrientation(rMat, orientation)[0]) + 360) % 360;
					break;

				case Sensor.TYPE_ORIENTATION:
					mAzimuth = (event.values[0] + 360) % 360;
					break;
				}

				mCallback.onOrientationChanged(mAzimuth);
			}
		};
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private void setupRotationVectorSensor() {
		orientation = new float[3];
		rMat = new float[9];

		mRotationVector = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
	}

	@SuppressWarnings("deprecation")
	private void setupLegacyOrientationSensor() {
		mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
	}
}
