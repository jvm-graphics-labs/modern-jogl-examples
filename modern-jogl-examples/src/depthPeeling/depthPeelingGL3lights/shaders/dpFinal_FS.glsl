//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

uniform samplerRect ColorTexture;
uniform vec3 BackgroundColor;

out vec4 outputColor;

void main(void)
{
	//vec4 frontColor = texture(ColorTexture, gl_FragCoord.xy);

	//outputColor.rgb = frontColor.rgb + BackgroundColor * frontColor.a;
	//outputColor.a = 1.0f;

    outputColor = texture(ColorTexture, gl_FragCoord.xy);
}