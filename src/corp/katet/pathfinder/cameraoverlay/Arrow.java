package corp.katet.pathfinder.cameraoverlay;

public class Arrow extends Polygon {

	private float arrowCoords[] = { 0.0f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.0f,
			-0.2f, 0.8f, 0.5f, -0.5f, 0.5f, 0.0f, -0.2f, 0.5f };

	private short arrowDrawOrder[] = { 4, 0, 1, 2, 4, 3, 0, 2 };

	// Set color with red, green, blue and alpha (opacity) values
	private float arrowColor[] = { 0.63671875f, 0.76953125f, 0.22265625f, 1f };

	public Arrow() {
		coords = arrowCoords;
		color = arrowColor;
		initialize(arrowDrawOrder);
	}
}
