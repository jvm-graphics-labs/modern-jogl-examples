#version 330

// Outputs
#define FRAG_COLOR  0

smooth in vec4 interpColor;

layout (location = FRAG_COLOR) out vec4 outputColor;

in vec4 diffuseColor_;
in vec3 vertexNormal;
in vec3 modelSpacePosition;

uniform vec3 modelSpaceLightPos;

uniform vec4 lightIntensity;
uniform vec4 ambientIntensity;

void main()
{
    vec3 lightDir = normalize(modelSpaceLightPos - modelSpacePosition);

    float cosAngIncidence = dot(normalize(vertexNormal), lightDir);
    cosAngIncidence = clamp(cosAngIncidence, 0, 1);
	
    outputColor = (diffuseColor_ * lightIntensity * cosAngIncidence) + (diffuseColor_ * ambientIntensity);
}
