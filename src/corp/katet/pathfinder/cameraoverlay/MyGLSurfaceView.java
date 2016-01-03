package corp.katet.pathfinder.cameraoverlay;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import corp.katet.pathfinder.R;
import corp.katet.pathfinder.TextResourceReader;
import android.content.Context;
import android.graphics.PixelFormat;
import static android.opengl.GLES20.*;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;

public class MyGLSurfaceView extends GLSurfaceView {

	private MyGLRenderer mRenderer;

	public MyGLSurfaceView(Context context, AttributeSet attrs, int defStyle) {
		this(context, attrs);
	}

	public MyGLSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public MyGLSurfaceView(Context context) {
		super(context);
		init(context);
	}

	private void init(Context context) {
		// Create an OpenGL ES 2.0 context
		setEGLContextClientVersion(2);

		setZOrderOnTop(true);

		setEGLConfigChooser(8, 8, 8, 8, 16, 0);

		getHolder().setFormat(PixelFormat.RGBA_8888);

		mRenderer = new MyGLRenderer(context, new Arrow());

		// Set the Renderer for drawing on the GLSurfFeView
		setRenderer(mRenderer);

		// Render the view only when there is a change in the drawing data
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

	private static final float X_ANGLE_SCALE = 80f;

	public float getAngleX() {
		return mRenderer.getAngleX() / X_ANGLE_SCALE;
	}

	public void setAngleX(float angleX) {
		mRenderer.setAngleX(angleX * X_ANGLE_SCALE);
		requestRender();
	}

	public float getAngleZ() {
		return mRenderer.getAngleZ();
	}

	public void setAngleZ(float angleZ) {
		if (Math.floor(angleZ) == Math.floor(mRenderer.getAngleZ()))
			return;
		mRenderer.setAngleZ(angleZ);
		requestRender();
	}

	public float[] getColor() {
		return mRenderer.getColor();
	}

	public void setColor(float[] color) {
		mRenderer.setColor(color);
	}

	public static class MyGLRenderer implements GLSurfaceView.Renderer {

		private Context mContext;
		private Polygon mPolygon;

		public MyGLRenderer(Context context, Polygon polygon) {
			mContext = context;
			mPolygon = polygon;
		}

		public void onSurfaceCreated(GL10 unused, EGLConfig config) {
			// Set the background frame color
			glClearColor(0f, 0f, 0f, 0f);

			// Disable culling to avoid removing back faces
			glDisable(GL_CULL_FACE);

			// Enable depth testing
			glEnable(GL_DEPTH_TEST);

			// Initialize an arrow
			initialize(mPolygon.getCoords(), mPolygon.calculateNormals());

			// Set the camera position (View matrix)
			Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 4f, 0f, 0f, 0f, 0f, 1f,
					0f);

			String vertexShaderCode = TextResourceReader
					.readTextFileFromResource(mContext,
							R.raw.per_pixel_vertex_shader);
			String fragmentShaderCode = TextResourceReader
					.readTextFileFromResource(mContext,
							R.raw.per_pixel_fragment_shader);
			int vertexShader = compileShader(GL_VERTEX_SHADER, vertexShaderCode);
			int fragmentShader = compileShader(GL_FRAGMENT_SHADER,
					fragmentShaderCode);
			mProgram = createAndLinkProgram(vertexShader, fragmentShader,
					new String[] { "a_Position", "u_Color", "a_Normal" });

			String pointVertexShaderCode = TextResourceReader
					.readTextFileFromResource(mContext,
							R.raw.point_vertex_shader);
			;
			String pointFragmentShaderCode = TextResourceReader
					.readTextFileFromResource(mContext,
							R.raw.point_fragment_shader);
			;
			int pointVertexShader = compileShader(GL_VERTEX_SHADER,
					pointVertexShaderCode);
			int pointFragmentShader = compileShader(GL_FRAGMENT_SHADER,
					pointFragmentShaderCode);
			mPointProgram = createAndLinkProgram(pointVertexShader,
					pointFragmentShader, new String[] { "a_Position" });
		}

		// Store the model data in a float buffer
		private FloatBuffer vertexBuffer;
		private FloatBuffer normalBuffer;
		private float[] colorBuffer;

		private int mProgram;
		private int mPointProgram;

		// Number of coordinates per vertex in this array
		private static final int POSITION_COMPONENT_COUNT = 3;
		private static final int NORMAL_COMPONENT_COUNT = 3;
		private static final int COLOR_COMPONENT_COUNT = 4;
		private static final int BYTES_PER_FLOAT = 4;

		// This matrix member variable provides a hook to manipulate
		// the coordinates of the objects that use this vertex shader
		// Used to access and set the transformation matrix
		private int MVPMatrixHandle;

		// Used to access and set the modelview matrix
		private int MVMatrixHandle;

		// Used to access and set the light position
		private int lightPosHandle;

		// Used to access and set the model position information
		private int positionHandle;

		// Used to access and set the model color information
		private int colorHandle;

		// Used to access and set the model normal information
		private int normalHandle;

		// Directional source of light position
		// Used to hold a light centered on the origin in model space
		// We need a 4th coordinate so we can get translations to work when
		// we multiply this by our transformation matrices
		private float[] lightPosInModelSpace = { 0.61f, 0.64f, -0.47f, 1f };

		// Used to hold the current position of the light in world space
		// (after transformation via model matrix)
		private final float[] lightPosInWorldSpace = new float[4];

		// Used to hold the transformed position of the light in eye space
		// (after transformation via modelview matrix)
		private final float[] lightPosInEyeSpace = new float[4];

		// Store the model matrix
		// This matrix is used to move models from object space (where each
		// model can be thought of being located at the center of the
		// universe) to world space
		private float[] mModelMatrix = new float[16];

		// Store the view matrix
		// This can be thought of as our camera
		// This matrix transforms world space to eye space; it positions
		// things relative to our eye
		private final float[] mViewMatrix = new float[16];

		// Store the projection matrix
		// This is used to project the scene onto a 2D viewport
		private final float[] mProjectionMatrix = new float[16];

		// Allocate storage for the final combined matrix
		// This will be passed into the shader program
		// mMVPMatrix is an abbreviation for "Model View Projection Matrix"
		private final float[] mMVPMatrix = new float[16];

		// Store a copy of the model matrix specifically for the light position
		private float[] lightModelMatrix = new float[16];

		protected void initialize(float[] polygonCoords, float[] polygonNormals) {
			// Initialize vertex byte buffer for shape coordinates
			vertexBuffer = ByteBuffer.allocateDirect(
			// Number of coordinate values * 4 bytes per float * 2 vectors
			// coords.length * 4 * 2);
					polygonCoords.length * BYTES_PER_FLOAT)
			// Use the device hardware's native byte order
					.order(ByteOrder.nativeOrder())
					// Create a floating point buffer from the ByteBuffer
					.asFloatBuffer();
			// Add the coordinates to the FloatBuffer
			vertexBuffer.put(polygonCoords).position(0);

			normalBuffer = ByteBuffer
					.allocateDirect(polygonNormals.length * BYTES_PER_FLOAT)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();
			normalBuffer.put(polygonNormals).position(0);

			colorBuffer = new float[COLOR_COMPONENT_COUNT];
		}

		public void onDrawFrame(GL10 gl) {
			// Redraw background color
			glClearColor(0f, 0f, 0f, 0f);
			glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);

			drawPolygon();

			drawLight();
		}

		public void onSurfaceChanged(GL10 unused, int width, int height) {
			glViewport(0, 0, width, height);

			float ratio = (float) width / height;

			// This projection matrix is applied to object coordinates
			// in the onDrawFrame() method
			Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f,
					7f);
		}

		private void drawPolygon() {
			glUseProgram(mProgram);

			// Get handle to shape's transformation matrix
			MVPMatrixHandle = glGetUniformLocation(mProgram, "u_MVPMatrix");
			MVMatrixHandle = glGetUniformLocation(mProgram, "u_MVMatrix");

			// Get handle to vertex shader's a_Position and a_Normal members
			positionHandle = glGetAttribLocation(mProgram, "a_Position");
			normalHandle = glGetAttribLocation(mProgram, "a_Normal");

			// Get handle to fragment's shader u_LightPos member
			lightPosHandle = glGetUniformLocation(mProgram, "u_LightPos");

			// Get handle to fragment's shader u_Color member
			colorHandle = glGetUniformLocation(mProgram, "u_Color");

			// Calculate position of the light
			Matrix.setIdentityM(lightModelMatrix, 0);
			Matrix.translateM(lightModelMatrix, 0, 0f, 0f, 2.5f);
			Matrix.multiplyMV(lightPosInWorldSpace, 0, lightModelMatrix, 0,
					lightPosInModelSpace, 0);
			Matrix.multiplyMV(lightPosInEyeSpace, 0, mViewMatrix, 0,
					lightPosInWorldSpace, 0);

			// Prepare the arrow coordinate data
			vertexBuffer.position(0);
			glVertexAttribPointer(positionHandle, POSITION_COMPONENT_COUNT,
					GL_FLOAT, false, 0, vertexBuffer);

			// Enable a handle to the arrow vertices
			glEnableVertexAttribArray(positionHandle);

			// Set the buffer to read the first normal coordinate
			vertexBuffer.position(0);

			// Prepare the normal vectors data
			glVertexAttribPointer(normalHandle, NORMAL_COMPONENT_COUNT,
					GL_FLOAT, false, 0, vertexBuffer);

			// Enable a handle to the arrow normal vectors
			glEnableVertexAttribArray(normalHandle);

			// Set position for coloring the arrow
			glUniform4fv(colorHandle, 1, colorBuffer, 0);

			// Set color for drawing the arrow
			System.arraycopy(mPolygon.getColor(), 0, colorBuffer, 0,
					COLOR_COMPONENT_COUNT);
			glUniform4fv(colorHandle, 1, colorBuffer, 0);

			// Create a rotation transformation for the arrow and
			// combine the rotation matrix with the projection and camera view
			// Note that the mMVPMatrix factor *must be first* in order
			// for the matrix multiplication product to be correct
			Matrix.setIdentityM(mModelMatrix, 0);
			float[] rotation = computeRotation(mAngleX, mAngleZ);
			Matrix.setRotateM(mModelMatrix, 0, rotation[0], rotation[1],
					rotation[2], rotation[3]);

			// Multiply the view matrix by the model matrix, and store the
			// result in the MVP matrix (which currently contains model * view)
			Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

			// Pass in the modelview matrix
			glUniformMatrix4fv(MVMatrixHandle, 1, false, mMVPMatrix, 0);

			// Multiply the modelview matrix by the projection matrix, and
			// store the result in the MVP matrix (which now contains
			// model * view * projection)
			Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix,
					0);

			// Pass in the combined matrix
			glUniformMatrix4fv(MVPMatrixHandle, 1, false, mMVPMatrix, 0);

			// Pass in the light position in eye space
			glUniform3f(lightPosHandle, lightPosInEyeSpace[0],
					lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

			// Draw arrow shape
			mPolygon.draw();
		}

		private void drawLight() {
			glUseProgram(mPointProgram);

			final int pointMVPMatrix = glGetUniformLocation(mPointProgram,
					"u_MVPMatrix");
			final int pointPosition = glGetAttribLocation(pointMVPMatrix,
					"a_Position");

			// Pass in the position
			glVertexAttrib3f(pointPosition, lightPosInModelSpace[0],
					lightPosInModelSpace[1], lightPosInModelSpace[2]);

			// Since it is not using a buffer, disable vertex arrays for this
			// attribute
			glDisableVertexAttribArray(pointPosition);

			// Pass in the transformation matrix
			Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, lightModelMatrix,
					0);
			Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix,
					0);
			glUniformMatrix4fv(pointMVPMatrix, 1, false, mMVPMatrix, 0);

			// Draw the light point
			glDrawArrays(GL_POINTS, 0, 1);
		}

		public volatile float mAngleZ = -0.01f;

		public float getAngleZ() {
			return mAngleZ;
		}

		public void setAngleZ(float angleZ) {
			mAngleZ = angleZ;
		}

		public volatile float mAngleX = -0.05f;

		public float getAngleX() {
			return mAngleX;
		}

		public void setAngleX(float angleX) {
			mAngleX = angleX;
		}

		private float[] computeRotation(float angleX, float angleZ) {
			float angle, factorX, factorZ;
			if (Math.abs(angleX) > Math.abs(angleZ)) {
				angle = angleX;
				factorX = 1f;
				factorZ = angleZ / angleX;
			} else {
				angle = angleZ;
				factorZ = 1f;
				factorX = angleX / angleZ;
			}
			float[] result = { angle, factorX, 0, factorZ };
			return result;
		}

		public float[] getColor() {
			return mPolygon.getColor();
		}

		public void setColor(float[] color) {
			mPolygon.setColor(color);
		}

		private static final String TAG = MyGLSurfaceView.class.getName() + "."
				+ MyGLRenderer.class.getSimpleName();

		/**
		 * Helper function to compile a shader
		 * 
		 * @param shaderType
		 *            The shader type
		 * @param shaderSource
		 *            The shader source code
		 * @return An OpenGL handle to the shader
		 */
		private int compileShader(final int shaderType,
				final String shaderSource) {
			int shaderHandle = glCreateShader(shaderType);

			if (shaderHandle != 0) {

				// Pass in the shader source
				glShaderSource(shaderHandle, shaderSource);

				// Compile the shader
				glCompileShader(shaderHandle);

				// Get the compilation status
				final int[] compileStatus = new int[1];
				glGetShaderiv(shaderHandle, GL_COMPILE_STATUS, compileStatus, 0);

				// If the compilation failed, delete the shader
				if (compileStatus[0] == 0) {
					Log.e(TAG, "Error compiling shader: "
							+ glGetShaderInfoLog(shaderHandle));
					glDeleteShader(shaderHandle);
					shaderHandle = 0;
					throw new RuntimeException("Error creating shader!");
				}
			}

			return shaderHandle;
		}

		/**
		 * Helper function to compile and link a program
		 * 
		 * @param vertexShaderHandle
		 *            An OpenGL handle to an already-compiled vertex shader
		 * @param fragmentShaderHandle
		 *            An OpenGL handle to an already-compiled fragment shader
		 * @param attributes
		 *            Attributes that need to be bound to the program
		 * @return An OpenGL handle to the program
		 */
		private int createAndLinkProgram(final int vertexShaderHandle,
				final int fragmentShaderHandle, final String[] attributes) {
			int programHandle = glCreateProgram();

			if (programHandle != 0) {
				// Bind the vertex shader to the program
				glAttachShader(programHandle, vertexShaderHandle);

				// Bind the fragment shader to the program
				glAttachShader(programHandle, fragmentShaderHandle);

				// Bind attributes
				if (attributes != null) {
					final int size = attributes.length;
					for (int i = 0; i < size; i++) {
						glBindAttribLocation(programHandle, i, attributes[i]);
					}
				}

				// Link the two shaders together into a program
				glLinkProgram(programHandle);

				// Get the link status
				final int[] linkStatus = new int[1];
				glGetProgramiv(programHandle, GL_LINK_STATUS, linkStatus, 0);

				// If the link failed, delete the program
				if (linkStatus[0] == 0) {
					Log.e(TAG, "Error compiling program: "
							+ glGetProgramInfoLog(programHandle));
					glDeleteProgram(programHandle);
					programHandle = 0;
					throw new RuntimeException("Error creating program!");
				}
			}

			return programHandle;
		}
	}
}
