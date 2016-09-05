#version 330

layout(std140) uniform;

out vec2 mapping;

uniform Projection
{
    mat4 cameraToClipMatrix;
};

uniform float sphereRadius;
uniform vec3 cameraSpherePos;

const float BOX_CORRECTION = 1.5;

void main()
{
    vec2 offset;
    switch(gl_VertexID)
    {
    //Bottom-left
    case 0:
        mapping = vec2(-1.0, -1.0) * BOX_CORRECTION;
        offset = vec2(-sphereRadius, -sphereRadius);
        break;
    //Top-left
    case 1:
        mapping = vec2(-1.0, 1.0) * BOX_CORRECTION;
        offset = vec2(-sphereRadius, sphereRadius);
        break;
    //Bottom-right
    case 2:
        mapping = vec2(1.0, -1.0) * BOX_CORRECTION;
        offset = vec2(sphereRadius, -sphereRadius);
        break;
    //Top-right
    case 3:        
        mapping = vec2(1.0, 1.0) * BOX_CORRECTION;
        offset = vec2(sphereRadius, sphereRadius);
        break;
    }

    vec4 cameraCornerPos = vec4(cameraSpherePos, 1.0);
    cameraCornerPos.xy += offset * BOX_CORRECTION;

    gl_Position = cameraToClipMatrix * cameraCornerPos;
}