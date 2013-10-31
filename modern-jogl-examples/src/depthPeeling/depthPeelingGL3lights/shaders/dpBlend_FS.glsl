//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

uniform samplerRect C0texture;
uniform samplerRect C1texture;
//uniform samplerRect C2texture;

out vec4 outputColor;

void main(void)
{
	vec4 c0 = texture(C0texture, gl_FragCoord.xy);
	vec4 c1 = texture(C1texture, gl_FragCoord.xy);
//	vec4 c2 = texture(C2texture, gl_FragCoord.xy);

      vec3 result = c0.rgb * c0.a + (1.0 - c0.a) * c1.rgb;
      //vec3 c1Layer = c1.rgb * c1.a + (1.0 - c1.a) * c2.rgb;
      //vec3 result = c0.rgb * c0.a + (1.0 - c0.a) * c1Layer;
        
      outputColor = vec4(result, 1.0);
      //  outputColor = vec4(c1.rgb, 1.0);
}
