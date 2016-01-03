package corp.katet.pathfinder;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

public class EarthGeometry {

	public static final double EARTH_RADIUS = 6371000;

	public static final int SHORT_DISTANCE = 100;
	public static final int LONG_DISTANCE = 10000;

	public static double computeHaversine(LatLng fromLocation, LatLng toLocation) {
		double lat1 = Math.toRadians(fromLocation.latitude);
		double lat2 = Math.toRadians(toLocation.latitude);
		double incLat = Math.toRadians(toLocation.latitude
				- fromLocation.latitude);
		double incLng = Math.toRadians(toLocation.longitude
				- fromLocation.longitude);

		double a = Math.pow(Math.sin(incLat / 2.), 2.) + Math.cos(lat1)
				* Math.cos(lat2) * Math.pow(Math.sin(incLng / 2.), 2.);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1. - a));

		double d = EARTH_RADIUS * c;

		return d;
	}

	public static double computeBearing(LatLng fromLocation, LatLng toLocation) {
		double lat1 = Math.toRadians(fromLocation.latitude);
		double lat2 = Math.toRadians(toLocation.latitude);
		double lng1 = Math.toRadians(fromLocation.longitude);
		double lng2 = Math.toRadians(toLocation.longitude);

		double y = Math.sin(lng2 - lng1) * Math.cos(lat2);
		double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
				* Math.cos(lat2) * Math.cos(lng2 - lng1);
		double brng = Math.toDegrees(Math.atan2(y, x));

		return brng;
	}

	public static double computeDistance(Location fromLocation,
			Location toLocation) {
		LatLng fromLoc = new LatLng(fromLocation.getLatitude(),
				fromLocation.getLongitude());
		LatLng toLoc = new LatLng(toLocation.getLatitude(),
				toLocation.getLongitude());

		double haversine = computeHaversine(fromLoc, toLoc);
		double incHeight = toLocation.getAltitude()
				- fromLocation.getAltitude();
		double distance = computeDistance(haversine, incHeight);

		return distance;
	}

	public static double computeDistance(double haversine, double incHeight) {
		return Math.sqrt(Math.pow(haversine, 2.) + Math.pow(incHeight, 2.));
	}

	public static double computeVerticalBearing(Location fromLocation,
			Location toLocation) {
		return computeVerticalBearing(
				computeDistance(fromLocation, toLocation),
				computeHaversine(
						new LatLng(fromLocation.getLatitude(), fromLocation
								.getLongitude()),
						new LatLng(toLocation.getLatitude(), toLocation
								.getLongitude())),
				computeHeightInc(fromLocation, toLocation));
	}

	public static double computeVerticalBearing(double distance,
			double haversine, double height) {
		double vBearing = Math.acos((Math.pow(distance, 2.)
				+ Math.pow(haversine, 2.) - Math.pow(height, 2.))
				/ (2. * distance * haversine));
		if (height < 0) {
			vBearing = -vBearing;
		}
		return vBearing;
	}

	public static double computeHeightInc(Location fromLoc, Location toLoc) {
		return computeHeightInc(fromLoc.getAltitude(), toLoc.getAltitude());
	}

	public static double computeHeightInc(double fromAltitude, double toAltitude) {
		return toAltitude - fromAltitude;
	}
}
