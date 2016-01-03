package corp.katet.pathfinder.cameraoverlay;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import static android.opengl.GLES20.*;

public abstract class Polygon {

	protected float coords[]; // In counterclockwise order

	// Set color with red, green, blue and alpha (opacity) values
	protected volatile float color[];

	// Order to draw vertices
	protected short drawOrder[];

	private ShortBuffer drawListBuffer;

	protected int drawMode;

	protected int vertexCount;

	protected void initialize() {
		drawMode = GL_TRIANGLES;
	}

	protected void initialize(short[] polygonDrawOrder) {
		drawMode = GL_TRIANGLE_STRIP;
		drawOrder = polygonDrawOrder;

		// Initialize byte buffer for the draw list
		drawListBuffer = ByteBuffer.allocateDirect(
		// (# of coordinate values * 2 bytes per short)
				drawOrder.length * 2).order(ByteOrder.nativeOrder())
				.asShortBuffer();
		drawListBuffer.put(drawOrder).position(0);
	}

	// Pass in the calculated transformation matrix
	protected void draw() {
		// Draw the polygon
		switch (drawMode) {
		case GL_TRIANGLE_STRIP:
			glDrawElements(drawMode, drawOrder.length, GL_UNSIGNED_SHORT,
					drawListBuffer);
			break;
		case GL_TRIANGLES:
		default:
			vertexCount = coords.length;
			glDrawArrays(drawMode, 0, vertexCount);
		}
	}

	public float[] getColor() {
		return color;
	}

	public void setColor(float[] color) {
		this.color = color;
	}

	public float[] getCoords() {
		return coords;
	}

	public float[] calculateNormals() {
		float[] surfaceNormal;
		float[] vertexNormal = new float[3];
		float[][] triangleCoords = new float[3][3];
		switch (drawMode) {
		case GL_TRIANGLE_STRIP:
			surfaceNormal = new float[(drawOrder.length - 2) * 9];
			for (int i = 0; i < drawOrder.length - 2; i++) {
				for (int j = 0; j < 3; j++) {
					System.arraycopy(coords, drawOrder[i + j] * 3,
							triangleCoords[j], 0, 3);
				}
				vertexNormal = calculateSurfaceNormal(triangleCoords);
				for (int j = 0; j < 3; j++) {
					System.arraycopy(vertexNormal, 0, surfaceNormal, i * 9 + j
							* 3, 3);
				}
			}
			break;
		case GL_TRIANGLES:
		default:
			surfaceNormal = new float[coords.length];
			for (int i = 0; i < coords.length;) {
				for (int j = 0; j < 3; j++) {
					System.arraycopy(coords, i + j * 3, triangleCoords[j], 0, 3);
				}
				vertexNormal = calculateSurfaceNormal(triangleCoords);
				for (int j = 0; j < 3; j++, i += 3) {
					System.arraycopy(vertexNormal, 0, surfaceNormal, i, 3);
				}
			}
		}
		return surfaceNormal;
	}

	private float[] calculateSurfaceNormal(float[][] triangle) {
		float[] normal = new float[3];
		float[] u = new float[3];
		float[] v = new float[3];

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				u[j] = triangle[1][j] - triangle[0][j];
				v[j] = triangle[2][j] - triangle[0][j];
			}
		}

		normal[0] = u[1] * v[2] - u[2] * v[1];
		normal[1] = u[2] * v[0] - u[0] * v[2];
		normal[2] = u[0] * v[1] - u[1] * v[0];

		return normalizeVector(normal);
	}

	private float[] normalizeVector(float[] vector) {
		float module = 0f;
		for (int i = 0; i < vector.length; i++) {
			module += Math.pow(vector[i], 2.);
		}
		module = (float) Math.sqrt(module);
		if (module > 0) {
			for (int i = 0; i < vector.length; i++) {
				vector[i] /= module;
			}
		}
		return vector;
	}
}
