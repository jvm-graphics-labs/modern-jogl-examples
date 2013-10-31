//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

uniform samplerRect DepthTexture;

uniform vec4 color;

out vec4 outputColor;

void main(void)
{
	// Bit-exact comparison between FP32 z-buffer and fragment depth
	float frontDepth = texture(DepthTexture, gl_FragCoord.xy).r;
	if (gl_FragCoord.z <= frontDepth) {
		discard;
	}
	
	// Shade all the fragments behind the z-buffer
	//vec4 color = ShadeFragment();
	//outputColor = vec4(color.rgb * color.a, color.a);

        outputColor = vec4(color.rgb * color.a, 1.0f);
}