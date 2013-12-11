#version 330

in vec4 diffuseColor;
in vec3 vertexNormal;
in vec3 cameraSpacePosition;

out vec4 outputColor;

uniform vec3 modelSpaceLightPosition;

uniform vec4 lightDiffuseIntensity;
uniform vec4 lightAmbientIntensity;

uniform vec3 cameraSpaceLightPosition;

uniform float lightAttenuation;

vec4 ApplyLightIntensity(in vec3 cameraSpacePosition, out vec3 lightDirection)
{
    vec3 lightDifference = cameraSpaceLightPosition - cameraSpacePosition;
    float lightDistanceSquare = dot(lightDifference, lightDifference);
    lightDirection = lightDifference * inversesqrt(lightDistanceSquare);

    return lightDiffuseIntensity * (1 / (1.0 + lightAttenuation * sqrt(lightDistanceSquare)));
}

void main()
{
    vec3 lightDirection = vec3(0.0);
    vec4 attenuationIntensity = ApplyLightIntensity(cameraSpacePosition, lightDirection);

    float cosAngleIncidence = dot(normalize(vertexNormal), lightDirection);
    cosAngleIncidence = clamp(cosAngleIncidence, 0, 1);

    outputColor = (diffuseColor * attenuationIntensity * cosAngleIncidence) + (diffuseColor * lightAmbientIntensity);
}