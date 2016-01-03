// A constant representing the combined model/view/projection matrix
uniform mat4 u_MVPMatrix;

// A constant representing the combined model/view matrix
uniform mat4 u_MVMatrix;

// The position of the light in eye space
uniform vec3 u_LightPos;

// Per-vertex position information we will pass in
attribute vec4 a_Position;

// Per-vertex color information we will pass in
uniform vec4 u_Color;

// Per-vertex normal information we will pass in
attribute vec3 a_Normal;

// This will be passed into the fragment shader
varying vec4 v_Color;

// The entry point for our vertex shader
void main() {
	// Transform the vertex into eye space
	vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);
	
	// Transform the normal's orientation into eye space
	vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));
	
	// Will be used for attenuation
	float distance = length(u_LightPos - modelViewVertex);
	
	// Get a lighting direction vector from the light to the vertex
	vec3 lightVector = normalize(u_LightPos - modelViewVertex);
	
	// Calculate the dot product of the light vector and vertex normal
	// If the normal and light vector are pointing the same direction, 
	// then it will get max illumination
	float diffuse = max(dot(modelViewNormal, lightVector), 0.1);
	
	// Attenuate the light based on distance
	diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));
	
	// Multiply the color by the illumination level
	// It will be interpolated across the triangle
	v_Color = vec4(vec3(u_Color) * (diffuse + 0.3), u_Color[3]);
	
	// gl_Position is a special variable used to store the final position
	// Multiply the vertex by the matrix to get the final point in 
	// normalized screen coordinates
	gl_Position = u_MVPMatrix * a_Position;
}
