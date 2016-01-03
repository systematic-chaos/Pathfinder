package corp.katet.pathfinder.map;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import corp.katet.pathfinder.R;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ProgressBar;

public class PlaceLocator extends AutoCompleteTextView {

	// / Global variables to hold the Location Service client and the UI widgets
	private Activity mActivity;
	private GoogleApiClient mGoogleApiClient;
	private PlaceLocatorListener mListener;
	private ProgressBar mProgress;

	private static final String STATE_HINT = "HINT";

	public PlaceLocator(Context context) {
		super(context);
	}

	public PlaceLocator(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PlaceLocator(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void initAutocomplete(GoogleApiClient googleApiClient,
			PlaceLocatorListener listener, ProgressBar progress) {
		if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
			mActivity = (Activity) getContext();
			mGoogleApiClient = googleApiClient;
			mListener = listener;
			mProgress = progress;
			setupAutoCompView();
		}
	}

	private void setupAutoCompView() {
		final PlacesAutoCompleteAdapter placesAdapter = new PlacesAutoCompleteAdapter(
				mActivity, R.layout.simple_dropdown_item_1line);
		setThreshold(4);

		/*
		 * The TextWatcher implementation calls back the Google Places service *
		 * to update the autoCompView's completion list, after the lapse of time
		 * * defined. It uses a Timer to schedule calls to the service
		 */
		final TextWatcher textWatcher = new TextWatcher() {
			private final int threshold = 4;
			private final int delay = 1000;

			private final Timer timer = new Timer();

			private TimerTask lastTimer = null;

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (count == 1) {
					dismissDropDown();
					setAdapter(null);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				// Cancel the last timer no matter what if text changed
				if (lastTimer != null) {
					lastTimer.cancel();
				}

				// We check against threshold twice: first to schedule the task
				if (s.length() > threshold) {
					lastTimer = new TimerTask() {

						@Override
						public void run() {
							final String placeName = getText().toString();

							/*
							 * We check against threshold twice: second to see *
							 * if we should still bother calling the geocoder *
							 * after the timeout
							 */
							if (placeName.length() >= threshold) {

								/*
								 * Use the text as it exists when called, *
								 * instead of the s
								 */
								mActivity.runOnUiThread(new Runnable() {

									public void run() {
										/*
										 * Perform the operation on the * UI
										 * thread
										 */
										setAdapter(placesAdapter);
										placesAdapter.getFilter().filter(
												placeName,
												new Filter.FilterListener() {

													@Override
													public void onFilterComplete(
															int count) {
														showDropDown();
													}
												});
									}
								});
							}
						}
					};

					/*
					 * Only call the Google Places service if a certain * amount
					 * of time has elapsed
					 */
					timer.schedule(lastTimer, delay);
				}
			}
		};
		addTextChangedListener(textWatcher);

		/*
		 * Add a listener to ensure that whenever one of the auto-fill items *
		 * is clicked, we jump right away
		 */
		setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,
					int position, long id) {
				((InputMethodManager) mActivity
						.getSystemService(Context.INPUT_METHOD_SERVICE))
						.hideSoftInputFromWindow(getWindowToken(), 0);
				placesAdapter.clear();
				dismissDropDown();
				clearFocus();
				mProgress.setVisibility(View.VISIBLE);

				/*
				 * Display on the map the information retrieved from the Google
				 * * Places autocomplete service based on the contents of the *
				 * AutoCompleteTextView that has been just updated
				 */
				Places.GeoDataApi
						.getPlaceById(
								mGoogleApiClient,
								((ReducedPlace) adapterView
										.getItemAtPosition(position)).getId())
						.setResultCallback(new ResultCallback<PlaceBuffer>() {
							@Override
							public void onResult(PlaceBuffer places) {
								if (places.getStatus().isSuccess()) {
									mListener.onPlaceLocated(((Place) places
											.get(0)).freeze());
								}
								places.release();
								mProgress.setVisibility(View.GONE);
							}
						});
			}
		});

		// Select all text if clicked when focus changed, do nothing otherwise
		setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				selectAll();
			}
		});
	}

	private ArrayList<ReducedPlace> autocomplete(String input) {
		final ArrayList<ReducedPlace> resultList = new ArrayList<ReducedPlace>();

		PendingResult<AutocompletePredictionBuffer> result = Places.GeoDataApi
				.getAutocompletePredictions(mGoogleApiClient, input,
						new LatLngBounds(new LatLng(-90, -180), new LatLng(90,
								180)), AutocompleteFilter.create(null));
		result.setResultCallback(new ResultCallback<AutocompletePredictionBuffer>() {
			@Override
			public void onResult(AutocompletePredictionBuffer predictedPlaces) {
				if (predictedPlaces.getStatus().isSuccess()) {
					for (AutocompletePrediction autocompletedPlace : predictedPlaces) {
						resultList.add(new ReducedPlace(autocompletedPlace
								.getDescription(), autocompletedPlace
								.getPlaceId()));
					}
				}
				predictedPlaces.release();
				((InputMethodManager) mActivity
						.getSystemService(Context.INPUT_METHOD_SERVICE))
						.hideSoftInputFromWindow(getWindowToken(), 0);
				showDropDown();
			}
		});

		return resultList;
	}

	private class PlacesAutoCompleteAdapter extends ArrayAdapter<ReducedPlace>
			implements Filterable {

		private ArrayList<ReducedPlace> resultList;

		public PlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
		}

		@Override
		public int getCount() {
			return resultList.size();
		}

		@Override
		public ReducedPlace getItem(int index) {
			return resultList.get(index);
		}

		@Override
		public Filter getFilter() {
			Filter filter = new Filter() {
				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					FilterResults filterResults = new FilterResults();
					if (constraint != null) {
						// Retrieve the autocomplete results
						resultList = autocomplete(constraint.toString());

						// Assign the data to the FilterResults
						filterResults.values = resultList;
						filterResults.count = resultList.size();
					}
					return filterResults;
				}

				@Override
				protected void publishResults(CharSequence constraint,
						FilterResults results) {
					if (results != null && results.count > 0) {
						notifyDataSetChanged();
					} else {
						notifyDataSetInvalidated();
					}
				}
			};
			return filter;
		}
	}

	private class ReducedPlace {
		private String description;
		private String id;

		public ReducedPlace(String description, String id) {
			setDescription(description);
			setId(id);
		}

		public String getDescription() {
			return description.toString();
		}

		public void setDescription(String description) {
			this.description = description.toString();
		}

		public String getId() {
			return id.toString();
		}

		public void setId(String id) {
			this.id = id.toString();
		}

		@Override
		public String toString() {
			return getDescription();
		}
	}

	public static interface PlaceLocatorListener {
		public void onPlaceLocated(Place placeLocated);
	}

	public void restoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_HINT)) {
			setText(savedInstanceState.getString(STATE_HINT));
		}
	}

	public void saveInstanceState(Bundle outState) {
		outState.putString(STATE_HINT, getText().toString());
	}
}
