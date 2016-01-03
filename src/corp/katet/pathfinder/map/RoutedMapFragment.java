package corp.katet.pathfinder.map;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.LatLng;

import corp.katet.pathfinder.R;
import corp.katet.pathfinder.map.PlaceLocator;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;

public class RoutedMapFragment extends Fragment implements
		PlaceLocator.PlaceLocatorListener {

	private RoutedMap mMap;
	private PlaceLocator mLocator;
	private int renderCounter = 0;

	private static final int RENDER_CYCLE = 40;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.located_map, container, false);
		view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT));

		mMap = (RoutedMap) view.findViewById(R.id.map);
		mMap.onCreate(savedInstanceState);
		mMap.create(savedInstanceState);

		mLocator = (PlaceLocator) view.findViewById(R.id.place_locator);

		return view;
	}

	public void setOrientation(float azimuth) {
		if (renderCounter++ % RENDER_CYCLE == 0) {
			mMap.setOrientation(azimuth);
			renderCounter %= RENDER_CYCLE;
		}
	}

	public void setFromLocation(LatLng location) {
		mMap.setFromLocation(location);
	}

	public void setToLocation(LatLng location) {
		mMap.setToLocation(location);
	}

	public void initAutocomplete(GoogleApiClient googleApiClient) {
		mLocator.initAutocomplete(googleApiClient, this,
				(ProgressBar) getView().findViewById(R.id.location_progress));
	}

	@Override
	public void onPlaceLocated(Place place) {
		setToLocation(place.getLatLng());
		if (getActivity() instanceof PlaceLocator.PlaceLocatorListener) {
			((PlaceLocator.PlaceLocatorListener) getActivity())
					.onPlaceLocated(place);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mMap.saveInstanceState(outState);
		mLocator.saveInstanceState(outState);
	}

	public void restoreInstanceState(Bundle savedInstanceState) {
		mLocator.restoreInstanceState(savedInstanceState);
	}

	@Override
	public void onResume() {
		mMap.onResume();
		super.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mMap.onDestroy();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mMap.onLowMemory();
	}
}
