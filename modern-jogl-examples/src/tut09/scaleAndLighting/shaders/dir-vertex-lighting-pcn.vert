#version 330

// Attribute
#define POSITION    0
#define COLOR       1
#define NORMAL      2

layout (location = POSITION) in vec3 position;
layout (location = COLOR) in vec4 diffuseColor;
layout (location = NORMAL) in vec3 normal;

smooth out vec4 interpColor;

uniform vec3 dirToLight;
uniform vec4 lightIntensity;

uniform mat4 modelToCameraMatrix;
uniform mat3 normalModelToCameraMatrix;

layout(std140) uniform Projection
{
    mat4 cameraToClipMatrix;
};

void main()
{
    gl_Position = cameraToClipMatrix * (modelToCameraMatrix * vec4(position, 1.0));

    vec3 normalCamSpace = normalize(normalModelToCameraMatrix * normal);

    float cosIncidenceAngle = dot(normalCamSpace, dirToLight);
    cosIncidenceAngle = clamp(cosIncidenceAngle, 0, 1);

    interpColor = lightIntensity * diffuseColor * cosIncidenceAngle;
}