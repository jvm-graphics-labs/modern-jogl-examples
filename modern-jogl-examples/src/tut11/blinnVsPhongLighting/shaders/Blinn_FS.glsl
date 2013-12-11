#version 330

in vec4 diffuseColor;
in vec3 vertexNormal;
in vec3 cameraSpacePosition;

out vec4 outputColor;

uniform vec4 lightDiffuseIntensity;
uniform vec4 lightAmbientIntensity;

uniform vec3 cameraSpaceLightPosition;

uniform float lightAttenuation;

const vec4 specularColor = vec4(0.25, 0.25, 0.25, 1.0);
uniform float shininessFactor;

float CalculateAttenuation(in vec3 cameraSpacePosition, out vec3 lightDirection)
{
	vec3 lightDifference =  cameraSpaceLightPosition - cameraSpacePosition;
	float lightDistanceSquare = dot(lightDifference, lightDifference);
	lightDirection = lightDifference * inversesqrt(lightDistanceSquare);
	
	return (1 / ( 1.0 + lightAttenuation * sqrt(lightDistanceSquare)));
}

void main()
{
	vec3 lightDirection = vec3(0.0);
	float attenuation = CalculateAttenuation(cameraSpacePosition, lightDirection);
	vec4 attenuationIntensity = attenuation * lightDiffuseIntensity;
	
	vec3 surfaceNormal = normalize(vertexNormal);
	
	vec3 viewDirection = normalize(-cameraSpacePosition);

        vec3 halfAngle = normalize(lightDirection + viewDirection);
        float blinnTerm = dot(surfaceNormal, halfAngle);
        blinnTerm = clamp(blinnTerm, 0, 1);
        blinnTerm = dot(surfaceNormal, lightDirection) >= 0.0 ? blinnTerm : 0.0;
        blinnTerm = pow(blinnTerm, shininessFactor);

        outputColor = (specularColor * attenuationIntensity * blinnTerm) + (diffuseColor * lightAmbientIntensity);
}
