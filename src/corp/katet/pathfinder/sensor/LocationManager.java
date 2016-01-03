package corp.katet.pathfinder.sensor;

import java.util.Date;

import android.location.Location;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class LocationManager implements LocationListener {

	protected LocationListener mCallback;
	protected GoogleApiClient mGoogleApiClient;
	protected Location mCurrentLocation;
	protected LocationRequest mLocationRequest;
	protected Date mLastUpdateTime;

	// Bool to track whether the user has turned location updates on or off
	private boolean mRequestingLocationUpdates = false;

	public LocationManager(LocationListener handler,
			GoogleApiClient googleApiClient) {
		this(handler, googleApiClient, false);
	}

	public LocationManager(LocationListener handler,
			GoogleApiClient googleApiClient, boolean requestLocationUpdates) {
		mCallback = handler;
		mGoogleApiClient = googleApiClient;
		createLocationRequest();

		getLastLocation();

		if (requestLocationUpdates) {
			startLocationUpdates();
		}
	}

	protected void createLocationRequest() {
		mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval(10000);
		mLocationRequest.setFastestInterval(5000);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	}

	public Location getLastLocation() {
		if (mGoogleApiClient.isConnected()
				&& (mCurrentLocation = LocationServices.FusedLocationApi
						.getLastLocation(mGoogleApiClient)) != null) {
			onLocationChanged(mCurrentLocation);
		}
		return mCurrentLocation;
	}

	public Date getLastTimestamp() {
		return mCurrentLocation != null ? mLastUpdateTime : null;
	}

	public boolean startLocationUpdates() {
		if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
			LocationServices.FusedLocationApi.requestLocationUpdates(
					mGoogleApiClient, mLocationRequest, this);
			mRequestingLocationUpdates = true;
		}
		return mRequestingLocationUpdates;
	}

	public boolean stopLocationUpdates() {
		if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
			LocationServices.FusedLocationApi.removeLocationUpdates(
					mGoogleApiClient, this);
			mRequestingLocationUpdates = false;
		}
		return !mRequestingLocationUpdates;
	}

	public boolean isRequestingLocationUpdates() {
		return mRequestingLocationUpdates;
	}

	@Override
	public void onLocationChanged(Location location) {
		mCurrentLocation = location;
		mLastUpdateTime = new Date();
		mCallback.onLocationChanged(location);
	}
}
