#version 330

layout(location = 0) in vec3 position;
layout(location = 2) in vec3 normal;

smooth out vec4 interpColor;

uniform vec3 lightPosition;
uniform vec4 lightDiffuseIntensity;
uniform vec4 lightAmbientIntensity;

uniform mat4 modelToCameraMatrix;
uniform mat3 normalModelToCameraMatrix;

layout(std140) uniform Projection
{
    mat4 cameraToClipMatrix;
};

void main()
{
    vec4 cameraPosition = (modelToCameraMatrix * vec4(position, 1.0));
    gl_Position = cameraToClipMatrix * cameraPosition;

    vec3 normalCameraSpace = normalize(normalModelToCameraMatrix * normal);

    vec3 dirToLight = normalize(lightPosition - vec3(cameraPosition));

    float cosAngleIncidence = dot(normalCameraSpace, dirToLight);
    cosAngleIncidence = clamp(cosAngleIncidence, 0, 1);

    interpColor = (lightDiffuseIntensity * cosAngleIncidence) + lightAmbientIntensity;
}