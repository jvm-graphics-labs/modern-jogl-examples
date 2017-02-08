#version 330

// Outputs
#define FRAG_COLOR  0

in vec3 vertexNormal;
in vec3 cameraSpacePosition;
in vec2 shinTexCoord;

layout (location = FRAG_COLOR) out vec4 outputColor;

layout(std140) uniform;

uniform Material
{
    vec4 diffuseColor;
    vec4 specularColor;
    float specularShininess;
} mtl;

struct PerLight
{
    vec4 cameraSpaceLightPos;
    vec4 lightIntensity;
};

const int NUMBER_OF_LIGHTS = 2;

uniform Light
{
    vec4 ambientIntensity;
    float lightAttenuation;
    PerLight lights[NUMBER_OF_LIGHTS];
} lgt;

uniform sampler2D gaussianTexture;
uniform sampler2D shininessTexture;

float calcAttenuation(in vec3 cameraSpacePosition, in vec3 cameraSpaceLightPos, out vec3 lightDirection)
{
    vec3 lightDifference =  cameraSpaceLightPos - cameraSpacePosition;
    float lightDistanceSqr = dot(lightDifference, lightDifference);
    lightDirection = lightDifference * inversesqrt(lightDistanceSqr);

    return (1 / (1.0 + lgt.lightAttenuation * lightDistanceSqr));
}

vec4 computeLighting(in PerLight lightData, in vec3 cameraSpacePosition, in vec3 cameraSpaceNormal, 
        in float specularShininess)
{
    vec3 lightDir;
    vec4 lightIntensity;
    if(lightData.cameraSpaceLightPos.w == 0.0)
    {
        lightDir = vec3(lightData.cameraSpaceLightPos);
        lightIntensity = lightData.lightIntensity;
    }
    else
    {
        float atten = calcAttenuation(cameraSpacePosition, lightData.cameraSpaceLightPos.xyz, lightDir);
        lightIntensity = atten * lightData.lightIntensity;
    }

    vec3 surfaceNormal = normalize(cameraSpaceNormal);
    float cosAngIncidence = dot(surfaceNormal, lightDir);
    cosAngIncidence = cosAngIncidence < 0.0001 ? 0.0 : cosAngIncidence;

    vec3 viewDirection = normalize(-cameraSpacePosition);

    vec3 halfAngle = normalize(lightDir + viewDirection);
    vec2 texCoord;
    texCoord.s = dot(halfAngle, surfaceNormal);
    texCoord.t = specularShininess;
    float gaussianTerm = texture(gaussianTexture, texCoord).r;

    gaussianTerm = cosAngIncidence != 0.0 ? gaussianTerm : 0.0;

    vec4 lighting = mtl.diffuseColor * lightIntensity * cosAngIncidence;
    lighting += mtl.specularColor * lightIntensity * gaussianTerm;

    return lighting;
}

void main()
{
    float specularShininess = texture(shininessTexture, shinTexCoord).r;

    vec4 accumLighting = mtl.diffuseColor * lgt.ambientIntensity;
    for(int light = 0; light < NUMBER_OF_LIGHTS; light++)
    {
        accumLighting += computeLighting(lgt.lights[light], cameraSpacePosition, vertexNormal, specularShininess);
    }

    outputColor = sqrt(accumLighting); //2.0 gamma correction
}
