#version 330
#extension GL_EXT_gpu_shader4 : enable

layout(std140) uniform;
layout(points) in;
layout(triangle_strip, max_vertices = 4) out;

uniform Projection
{
    mat4 cameraToClipMatrix;
};

in VertexData
{
    vec3 cameraSpherePos;
    float sphereRadius;
} vertexData[];

out FragData
{
    flat vec3 cameraSpherePos;
    flat float sphereRadius;
    smooth vec2 mapping;
};

const float boxCorrection = 1.5;

void main()
{
    vec4 cameraCornerPos;
    
    //Bottom-left
    mapping = vec2(-1.0, -1.0) * boxCorrection;
    cameraSpherePos = vec3(vertexData[0].cameraSpherePos);
    sphereRadius = vertexData[0].sphereRadius;
    cameraCornerPos = vec4(vertexData[0].cameraSpherePos, 1.0);
    cameraCornerPos.xy += vec2(-vertexData[0].sphereRadius, -vertexData[0].sphereRadius) * boxCorrection;
    gl_Position = cameraToClipMatrix * cameraCornerPos;
    gl_PrimitiveID = gl_PrimitiveIDIn;
    EmitVertex();

    //Top-left
    mapping = vec2(-1.0, 1.0) * boxCorrection;
    cameraSpherePos = vec3(vertexData[0].cameraSpherePos);
    sphereRadius = vertexData[0].sphereRadius;
    cameraCornerPos = vec4(vertexData[0].cameraSpherePos, 1.0);
    cameraCornerPos.xy += vec2(-vertexData[0].sphereRadius, vertexData[0].sphereRadius) * boxCorrection;
    gl_Position = cameraToClipMatrix * cameraCornerPos;
    gl_PrimitiveID = gl_PrimitiveIDIn;
    EmitVertex();

    //Bottom-right
    mapping = vec2(1.0, -1.0) * boxCorrection;
    cameraSpherePos = vec3(vertexData[0].cameraSpherePos);
    sphereRadius = vertexData[0].sphereRadius;
    cameraCornerPos = vec4(vertexData[0].cameraSpherePos, 1.0);
    cameraCornerPos.xy += vec2(vertexData[0].sphereRadius, -vertexData[0].sphereRadius) * boxCorrection;
    gl_Position = cameraToClipMatrix * cameraCornerPos;
    gl_PrimitiveID = gl_PrimitiveIDIn;
    EmitVertex();

    //Top-right
    mapping = vec2(1.0, 1.0) * boxCorrection;
    cameraSpherePos = vec3(vertexData[0].cameraSpherePos);
    sphereRadius = vertexData[0].sphereRadius;
    cameraCornerPos = vec4(vertexData[0].cameraSpherePos, 1.0);
    cameraCornerPos.xy += vec2(vertexData[0].sphereRadius, vertexData[0].sphereRadius) * boxCorrection;
    gl_Position = cameraToClipMatrix * cameraCornerPos;
    gl_PrimitiveID = gl_PrimitiveIDIn;
    EmitVertex();
}