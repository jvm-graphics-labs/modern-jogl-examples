#version 330

layout(location = 0) in vec3 position;
layout(location = 1) in vec4 inDiffuseColor;
layout(location = 2) in vec3 normal;

smooth out vec4 interpColor;

uniform vec3 lightPositionModelSpace;
uniform vec4 lightDiffuseIntensity;
uniform vec4 lightAmbientIntensity;

uniform mat4 modelToCameraMatrix;

uniform Projection
{
    mat4 cameraToClipMatrix;
};

void main()
{
    gl_Position = cameraToClipMatrix * (modelToCameraMatrix * vec4(position, 1.0));

    vec3 directionToLight = normalize(lightPositionModelSpace - position);

    float cosAngleIncidence = dot(normal, directionToLight);
    cosAngleIncidence = clamp(cosAngleIncidence, 0, 1);

    interpColor = (lightDiffuseIntensity * cosAngleIncidence * inDiffuseColor) + (lightAmbientIntensity * inDiffuseColor);
}