#version 330

in vec4 diffuseColor;
in vec3 vertexNormal;
in vec3 modelSpacePosition;

out vec4 outputColor;

uniform vec3 lightPositionModelSpace;

uniform vec4 lightDiffuseIntensity;
uniform vec4 lightAmbientIntensity;

void main()
{
    vec3 directionToLightModelSpace = normalize(lightPositionModelSpace - modelSpacePosition);

    float cosAngleIncidence = dot(normalize(vertexNormal), directionToLightModelSpace);
    cosAngleIncidence = clamp(cosAngleIncidence, 0, 1);

    outputColor = (diffuseColor * lightDiffuseIntensity * cosAngleIncidence) + (diffuseColor * lightAmbientIntensity);
}