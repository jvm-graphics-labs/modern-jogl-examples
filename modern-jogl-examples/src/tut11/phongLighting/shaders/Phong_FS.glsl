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


float CalcAttenuation(in vec3 cameraSpacePosition, out vec3 lightDirection)
{
	vec3 lightDifference =  cameraSpaceLightPosition - cameraSpacePosition;
	float lightDistanceSqr = dot(lightDifference, lightDifference);
	lightDirection = lightDifference * inversesqrt(lightDistanceSqr);
	
	return (1 / ( 1.0 + lightAttenuation * sqrt(lightDistanceSqr)));
}

void main()
{
	vec3 lightDirection = vec3(0.0);
	float attenuation = CalcAttenuation(cameraSpacePosition, lightDirection);
	vec4 attenIntensity = attenuation * lightDiffuseIntensity;
	
	vec3 surfaceNormal = normalize(vertexNormal);
	float cosAngleIncidence = dot(surfaceNormal, lightDirection);
	cosAngleIncidence = clamp(cosAngleIncidence, 0, 1);
	
	vec3 viewDirection = normalize(-cameraSpacePosition);
	vec3 reflectDirection = reflect(-lightDirection, surfaceNormal);
	float phongTerm = dot(viewDirection, reflectDirection);
	phongTerm = clamp(phongTerm, 0, 1);
	phongTerm = cosAngleIncidence != 0.0 ? phongTerm : 0.0;
	phongTerm = pow(phongTerm, shininessFactor);
	

	outputColor = (diffuseColor * attenIntensity * cosAngleIncidence) + (specularColor * attenIntensity * phongTerm) + (diffuseColor * lightAmbientIntensity);
}