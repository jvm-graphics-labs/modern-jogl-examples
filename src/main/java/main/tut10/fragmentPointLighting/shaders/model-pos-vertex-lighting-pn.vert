#version 330

// Attribute
#define POSITION    0
#define COLOR       1
#define NORMAL      2

layout(location = POSITION) in vec3 position;
layout(location = NORMAL) in vec3 normal;

smooth out vec4 interpColor;

uniform vec3 modelSpaceLightPos;
uniform vec4 lightIntensity;
uniform vec4 ambientIntensity;

uniform mat4 modelToCameraMatrix;

uniform Projection
{
    mat4 cameraToClipMatrix;
};

void main()
{
    gl_Position = cameraToClipMatrix * (modelToCameraMatrix * vec4(position, 1.0));

    vec3 dirToLight = normalize(modelSpaceLightPos - position);
	
    float cosAngIncidence = dot(normal, dirToLight);
    cosAngIncidence = clamp(cosAngIncidence, 0, 1);
	
    interpColor = (lightIntensity * cosAngIncidence) + ambientIntensity;
}
