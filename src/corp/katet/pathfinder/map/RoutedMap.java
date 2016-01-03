package corp.katet.pathfinder.map;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import corp.katet.pathfinder.EarthGeometry;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.AttributeSet;

public class RoutedMap extends MapView implements OnMapReadyCallback {

	private GoogleMap mMap;

	private LatLng mFromLocation = null, mToLocation = null;
	private float mFromOrientation = 0.f;

	private static final String STATE_ORIENTATION = "ORIENTATION";
	private static final String STATE_FROMCOORDS = "FROMCOORDS";
	private static final String STATE_TOCOORDS = "TOCOORDS";

	public RoutedMap(Context context) {
		super(context);
	}

	public RoutedMap(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RoutedMap(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public RoutedMap(Context context, GoogleMapOptions options) {
		super(context, options);
	}

	public void create(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			mFromOrientation = savedInstanceState.getFloat(STATE_ORIENTATION);
			mFromLocation = savedInstanceState.getParcelable(STATE_FROMCOORDS);
			mToLocation = savedInstanceState.getParcelable(STATE_TOCOORDS);
		}

		getMapAsync(this);
	}

	public void saveInstanceState(Bundle outState) {
		outState.putParcelable(STATE_FROMCOORDS, mFromLocation);
		outState.putParcelable(STATE_TOCOORDS, mToLocation);
		outState.putFloat(STATE_ORIENTATION, mFromOrientation);
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;

		if (mFromLocation != null || mToLocation != null) {
			drawMapOverlay();
		}
	}

	public void setOrientation(float azimuth) {
		if (Math.floor(azimuth) == Math.floor(mFromOrientation))
			return;
		mFromOrientation = azimuth;
		if (mMap != null && mFromLocation != null) {
			drawMapOverlay();
		}
	}

	public void setFromLocation(LatLng location) {
		if (location.equals(mFromLocation))
			return;
		mFromLocation = new LatLng(location.latitude, location.longitude);
		if (mMap != null) {
			drawMapOverlay();
		}
	}

	public void setToLocation(LatLng location) {
		mToLocation = new LatLng(location.latitude, location.longitude);
		if (mMap != null) {
			drawMapOverlay();
		}
	}

	private void drawMapOverlay() {
		mMap.clear();

		if (mFromLocation != null) {
			// Flat markers will rotate when the map is rotated,
			// and change perspective when the map is tilted
			mMap.addMarker(new MarkerOptions()
					.icon(BitmapDescriptorFactory.fromAsset("geo.png"))
					.position(mFromLocation).flat(true)
					.rotation(mFromOrientation));
			if (mToLocation == null) {
				mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
						mFromLocation, 13));
			}
		}
		if (mToLocation != null) {
			mMap.addMarker(new MarkerOptions().icon(
					BitmapDescriptorFactory.fromAsset("marker.png")).position(
					mToLocation));
			if (mFromLocation == null) {
				mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
						mToLocation, 13));
			}
		}
		if (mFromLocation != null && mToLocation != null) {
			double haversine = EarthGeometry.computeHaversine(mFromLocation,
					mToLocation);
			PolylineOptions polyline = new PolylineOptions().geodesic(true)
					.add(mFromLocation).add(mToLocation).width(2);
			int color;
			if (haversine < EarthGeometry.SHORT_DISTANCE)
				color = Color.GREEN;
			else if (haversine < EarthGeometry.LONG_DISTANCE)
				color = Color.BLUE;
			else
				color = Color.RED;
			mMap.addPolyline(polyline.color(color));

			mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
					computeLocationBounds(mFromLocation, mToLocation), 50));
		}
	}

	public static LatLngBounds computeLocationBounds(LatLng loc1, LatLng loc2) {
		double north, east, south, west;
		if (loc1.latitude < loc2.latitude) {
			west = loc1.latitude;
			east = loc2.latitude;
		} else {
			west = loc2.latitude;
			east = loc1.latitude;
		}
		if (loc1.longitude < loc2.longitude) {
			south = loc1.longitude;
			north = loc2.longitude;
		} else {
			south = loc2.longitude;
			north = loc1.longitude;
		}
		return new LatLngBounds(new LatLng(west, south),
				new LatLng(east, north));
	}
}
