#version 330

in vec4 diffuseColor;
in vec3 vertexNormal;

out vec4 outputColor;

uniform vec4 lightDiffuseIntensity;
uniform vec4 lightAmbientIntensity;

uniform vec3 lightPositionCameraSpace;

uniform float lightAttenuation;
uniform bool rSquare;

uniform UnProjection
{
    mat4 clipToCameraMatrix;
    ivec2 windowSize;
};

vec3 CalculateCameraSpacePosition()
{
    vec4 ndcPosition;

    ndcPosition.xy = ((gl_FragCoord.xy / windowSize.xy) * 2.0) - 1.0;

    ndcPosition.z = (2.0 * gl_FragCoord.z - gl_DepthRange.near - gl_DepthRange.far) / (gl_DepthRange.far - gl_DepthRange.near);

    ndcPosition.w = 1.0;

    vec4 clipPosition = ndcPosition / gl_FragCoord.w;

    return vec3(clipToCameraMatrix * clipPosition);
}

vec4 ApplyLightIntensity(in vec3 cameraSpacePosition, out vec3 lightDirection)
{
    vec3 lightDifference = lightPositionCameraSpace - cameraSpacePosition;
    float lightDistanceSqr = dot(lightDifference, lightDifference);
    lightDirection = lightDifference * inversesqrt(lightDistanceSqr);

    float distanceFactor = rSquare ? lightDistanceSqr : sqrt(lightDistanceSqr);

    return lightDiffuseIntensity * (1 / (1.0 + lightAttenuation * distanceFactor));
}

void main()
{
    vec3 cameraSpacePosition = CalculateCameraSpacePosition();

    vec3 lightDirection = vec3(0.0);

    vec4 attenuationIntensity = ApplyLightIntensity(cameraSpacePosition, lightDirection);

    float cosAngleIncidence = dot(normalize(vertexNormal), lightDirection);
    cosAngleIncidence = clamp(cosAngleIncidence, 0, 1);

    outputColor = (diffuseColor * attenuationIntensity * cosAngleIncidence) + (diffuseColor * lightAmbientIntensity);
    //outputColor = vec4(1.0, 0.0, 1.0, 1.0);
}