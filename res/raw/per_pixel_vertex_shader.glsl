// Define the per-pixel lighting shader

// A constant representing the combined model/view/projection matrix
uniform mat4 u_MVPMatrix;
// A constant representing the combined model/view matrix
uniform mat4 u_MVMatrix;

// Per-vertex position information to be passed in
attribute vec4 a_Position;
// Per-vertex color information to be passed in
uniform vec4 u_Color;
// Per-vertex normal information to be passed in
attribute vec3 a_Normal;

// This will be passed into the fragment shader
varying vec3 v_Position;
// This will be passed into the fragment shader
varying vec4 v_Color;
// This will be passed into the fragment shader
varying vec3 v_Normal;

// The entry point for the vertex shader
void main() {
	// Transform the vertex into eye space
	v_Position = vec3(u_MVMatrix * a_Position);
	// Pass through the color
	v_Color = u_Color;
	// Transform the normal's orientation into eye space
	v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));
	// gl_Position is a special variable used to store the final point position
	// Multiply the vertex by the matrix to get the final point in normalized 
	// screen coordinates
	gl_Position = u_MVPMatrix * a_Position;
}
