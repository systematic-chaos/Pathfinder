package corp.katet.pathfinder;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.GregorianCalendar;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;

import corp.katet.pathfinder.R;
import corp.katet.pathfinder.cameraoverlay.CameraFragment;
import corp.katet.pathfinder.map.RoutedMapFragment;
import corp.katet.pathfinder.map.PlaceLocator.PlaceLocatorListener;
import corp.katet.pathfinder.sensor.AltitudeManager;
import corp.katet.pathfinder.sensor.AltitudeManager.AltitudeListener;
import corp.katet.pathfinder.sensor.LocationManager;
import corp.katet.pathfinder.sensor.OrientationManager;
import corp.katet.pathfinder.sensor.OrientationManager.OrientationListener;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

public class PathfinderActivity extends FragmentActivity implements
		GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener, LocationListener,
		OrientationListener, AltitudeListener, PlaceLocatorListener {

	protected CameraFragment mCamera;
	protected RoutedMapFragment mMap;

	protected GoogleApiClient mGoogleApiClient;
	protected LocationManager mLocationManager;
	protected OrientationManager mOrientationManager;
	protected AltitudeManager mAltitudeManager;
	protected Location mFromLocation, mToLocation;
	protected Date mLastUpdateTime;
	protected float mLastOrientation = 0.f;

	// Request code to use when launching the resolution activity
	private static final int REQUEST_RESOLVE_ERROR = 1001;
	// Unique tag for the error dialog fragment
	private static final String DIALOG_ERROR = "dialog_error";
	// Bool to track whether the app is already resolving an error
	protected boolean mResolvingError = false;

	private static final String STATE_RESOLVING_ERROR = "resolving_error";
	protected static final String FROM_LOCATION_KEY = "from_location";
	protected static final String TO_LOCATION_KEY = "to_location";
	protected static final String LAST_UPDATED_TIME_STRING_KEY = "last_updated_time";
	protected static final String ORIENTATION_KEY = "orientation";

	protected static final String FROM_TAG = "from";
	protected static final String TO_TAG = "to";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		buildGoogleApiClient();
		mLocationManager = new LocationManager(this, mGoogleApiClient);
		mOrientationManager = new OrientationManager(this, this);
		mAltitudeManager = new AltitudeManager(this);

		setContentView(R.layout.main);

		mCamera = (CameraFragment) getSupportFragmentManager()
				.findFragmentById(R.id.preview_fragment);
		mMap = (RoutedMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map_fragment);

		updateValuesFromBundle(savedInstanceState);
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (!mResolvingError) {
			mGoogleApiClient.connect();
		}
	}

	@Override
	protected void onStop() {
		mGoogleApiClient.disconnect();

		super.onStop();
	}

	@Override
	protected void onResume() {
		super.onResume();

		GoogleApiAvailability gaa = GoogleApiAvailability.getInstance();
		int resultCode = gaa.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			gaa.showErrorNotification(this, resultCode);
		}

		mLocationManager.startLocationUpdates();
		mOrientationManager.startOrientationUpdates();
	}

	@Override
	protected void onPause() {
		mLocationManager.stopLocationUpdates();
		mOrientationManager.stopOrientationUpdates();
		mAltitudeManager.cancelQuery(FROM_TAG);
		mAltitudeManager.cancelQuery(TO_TAG);

		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
		if (mFromLocation != null) {
			outState.putParcelable(FROM_LOCATION_KEY, mFromLocation);
		}
		if (mToLocation != null) {
			outState.putParcelable(TO_LOCATION_KEY, mToLocation);
		}
		if (mLastUpdateTime != null) {
			outState.putString(LAST_UPDATED_TIME_STRING_KEY, DateFormat
					.getInstance().format(mLastUpdateTime));
		}
		outState.putFloat(ORIENTATION_KEY, mLastOrientation);

		if (mMap != null) {
			mMap.onSaveInstanceState(outState);
		}
	}

	private void updateValuesFromBundle(Bundle inState) {
		if (inState == null)
			return;

		mResolvingError = inState.getBoolean(STATE_RESOLVING_ERROR, false);

		if (inState.containsKey(FROM_LOCATION_KEY)) {
			onLocationChanged((Location) inState
					.getParcelable(FROM_LOCATION_KEY));
		}
		if (inState.containsKey(TO_LOCATION_KEY)) {
			setToLocation((Location) inState.getParcelable(TO_LOCATION_KEY));
		}
		if (inState.containsKey(LAST_UPDATED_TIME_STRING_KEY)) {
			try {
				mLastUpdateTime = DateFormat.getInstance().parse(
						inState.getString(LAST_UPDATED_TIME_STRING_KEY));
			} catch (ParseException pe) {
				// Since this string was created by the very own DateFormat
				// class, this exception is unlikely to be thrown
			}
		}

		onOrientationChanged(mLastOrientation = inState
				.getFloat(ORIENTATION_KEY));

		mMap.restoreInstanceState(inState);
	}

	// Create a GoogleApiClient instance
	protected synchronized void buildGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API).addApi(Places.GEO_DATA_API)
				.addApi(Places.PLACE_DETECTION_API).build();
	}

	@Override
	public void onPlaceLocated(Place placeLocated) {
		mToLocation = new Location(Places.GeoDataApi.toString());
		mToLocation.setLatitude(placeLocated.getLatLng().latitude);
		mToLocation.setLongitude(placeLocated.getLatLng().longitude);
		mToLocation.setTime(GregorianCalendar.getInstance(
				placeLocated.getLocale()).getTimeInMillis());
		mToLocation.setAccuracy(1);

		mAltitudeManager.queryAltitude(mToLocation, this, TO_TAG);
	}

	@Override
	public void onLocationChanged(Location location) {
		mLastUpdateTime = mLocationManager.getLastTimestamp();
		setFromLocation(location);
	}

	public void setFromLocation(Location location) {
		if (!(mFromLocation = location).hasAltitude()) {
			mAltitudeManager.queryAltitude(mFromLocation, this, FROM_TAG);
		}

		if (mMap != null) {
			mMap.setFromLocation(new LatLng(location.getLatitude(), location
					.getLongitude()));
		}
	}

	public void setToLocation(Location location) {
		if (!(mToLocation = location).hasAltitude()) {
			mAltitudeManager.queryAltitude(mToLocation, this, TO_TAG);
		}

		if (mMap != null) {
			mMap.setToLocation(new LatLng(location.getLatitude(), location
					.getLongitude()));
		}
	}

	@Override
	public void onOrientationChanged(float azimuth) {
		mLastOrientation = azimuth;

		if (mCamera != null) {
			mCamera.setOrientation(azimuth);
		}

		if (mMap != null) {
			mMap.setOrientation(azimuth);
		}
	}

	@Override
	public void onAltitudeResolved(String tag, double altitude) {
		if (tag.equals(FROM_TAG)) {
			mFromLocation.setAltitude(altitude);
			if (mCamera != null) {
				mCamera.setFromLocation(mFromLocation);
			}
		}
		if (tag.equals(TO_TAG)) {
			mToLocation.setAltitude(altitude);
			if (mCamera != null) {
				mCamera.setToLocation(mToLocation);
			}
		}
	}

	// Connected to Google Play Services!
	// The good stuff goes here
	@Override
	public void onConnected(Bundle connectionHint) {
		mLocationManager.startLocationUpdates();

		mMap.initAutocomplete(mGoogleApiClient);
	}

	// The connection has been interrupted
	// Disable any UI components that depend on Google APIs
	// until onConnected is called()
	@Override
	public void onConnectionSuspended(int cause) {
	}

	// This callback is important for handling errors that
	// may occur while attempting to connect with Google
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		if (mResolvingError) {
			// Already attempting to resolve an error
			return;
		} else if (result.hasResolution()) {
			try {
				mResolvingError = true;
				result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
			} catch (SendIntentException sie) {
				// There was an error with the resolution intent
				// Try again
				mGoogleApiClient.connect();
			}
		} else {
			// Show dialog using GooglePlayServicesUtil.getErrorDialog()
			showErrorDialog(result.getErrorCode());
			mResolvingError = true;
		}
	}

	// Creates a dialog for an error message
	private void showErrorDialog(int errorCode) {
		// Create a fragment for the error dialog
		ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
		// Pass the error that should be displayed
		Bundle args = new Bundle();
		args.putInt(DIALOG_ERROR, errorCode);
		dialogFragment.setArguments(args);
		dialogFragment.show(getSupportFragmentManager(), "errordialog");
	}

	// Called from ErrorDialogFragment when the dialog is dismissed
	public void onDialogDismissed() {
		mResolvingError = false;
	}

	// A fragment to display an error dialog
	private class ErrorDialogFragment extends DialogFragment {

		public ErrorDialogFragment() {
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Get the error code and retrieve the appropriate dialog
			int errorCode = this.getArguments().getInt(DIALOG_ERROR);
			return GooglePlayServicesUtil.getErrorDialog(errorCode,
					this.getActivity(), REQUEST_RESOLVE_ERROR);
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			((PathfinderActivity) getActivity()).onDialogDismissed();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_RESOLVE_ERROR) {
			mResolvingError = false;
			if (resultCode == RESULT_OK) {
				// Make sure the app is not already connected or attempting to
				// connect
				if (!mGoogleApiClient.isConnecting()
						&& !mGoogleApiClient.isConnected()) {
					mGoogleApiClient.connect();
				}
			}
		}
	}
}
