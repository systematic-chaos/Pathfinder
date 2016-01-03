package corp.katet.pathfinder.sensor;

import java.util.Hashtable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import android.content.Context;
import android.location.Location;

public class AltitudeManager {

	public static interface AltitudeListener {
		public void onAltitudeResolved(String tag, double altitude);
	}

	protected Context mParent;
	protected Hashtable<String, Double> mAltitude;

	public AltitudeManager(Context parent) {
		mParent = parent;
		mAltitude = new Hashtable<String, Double>();
	}

	public void queryAltitude(Location location, AltitudeListener handler,
			String tag) {
		queryAltitude(location.getLatitude(), location.getLongitude(), handler,
				tag);
	}

	public void queryAltitude(double latitude, double longitude,
			final AltitudeListener handler, final String tag) {
		StringBuilder url = new StringBuilder(
				"https://maps.googleapis.com/maps/api/elevation/json?");
		url.append("locations=" + latitude + "," + longitude);

		JsonObjectRequest jsObjRequest = (JsonObjectRequest) new JsonObjectRequest(
				Request.Method.GET, url.toString(), null,
				new Response.Listener<JSONObject>() {

					@Override
					public void onResponse(JSONObject response) {
						try {
							if (response.getString("status").equals("OK")) {
								JSONArray results = response
										.getJSONArray("results");
								double lastAltitude;
								for (int n = 0; n < results.length(); n++) {
									mAltitude.put(tag,
											lastAltitude = results
													.getJSONObject(n)
													.getDouble("elevation"));
									handler.onAltitudeResolved(tag,
											lastAltitude);
								}
							} else {
								throw new JSONException(response
										.getString("status"));
							}
						} catch (JSONException jsone) {
							handler.onAltitudeResolved(tag,
									Double.NEGATIVE_INFINITY);
						}
					}
				}, new Response.ErrorListener() {

					@Override
					public void onErrorResponse(VolleyError error) {
						handler.onAltitudeResolved(tag,
								Double.NEGATIVE_INFINITY);
					}
				}).setTag(tag);

		// Access the RequestQueue
		VolleyRequestQueue.getInstance(mParent).addToRequestQueue(jsObjRequest);
	}

	public void cancelQuery(String tag) {
		VolleyRequestQueue.getInstance(mParent).cancelRequest(tag);
	}

	public double getLastAltitude(String tag) {
		return mAltitude.contains(tag) ? mAltitude.get(tag) : 0.;
	}
}
