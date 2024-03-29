#version 460 core

struct Light {
	vec3 colour;
	vec3 position;
	vec3 attenuation;
	float enabled;
};
layout (std140) uniform LightBlock{
	Light lights[{maxlights}];
};

uniform float ambientBrightness;

in vec3 vPosition;
in vec3 vNormal;
in vec3 vLightDirection[{maxlights}];
flat in vec4 vColour;

out lowp vec4 outColour;

void main() {
	
	vec3 baseColour = vColour.rgb;
	float colStr = 1;
	vec3 reflectTint =  vColour.rgb;
	float refStr = 1;
	float alpha = vColour.a;
	
    vec3 matAmbientColour =  colStr * baseColour;
	
	vec3 runningDiffuse = vec3(0);
	int count = 0;
	
	//https://www.desmos.com/calculator/r3vn8t14ab equation for lighting attenuation
	for(int i = 0;i < {maxlights};i++){
		if(lights[i].enabled > 0.5){
		
			float distance = length(vLightDirection[i]);
			float strFactor=1;
			if( abs(distance) > lights[i].attenuation.x){
				strFactor = 1 - pow(max(distance - lights[i].attenuation.x,0),1/lights[i].attenuation.z)/lights[i].attenuation.y;
			}
			
			vec3 uvNormal = normalize(vNormal);
			vec3 uvLightDirection = normalize(vLightDirection[i]);
			
			float nDot1 = dot(uvNormal, uvLightDirection);
			
			float brightness = nDot1 + (lights[i].attenuation.z/2);
			vec3 matDiffuseColour = (refStr * max(0.0, brightness) * reflectTint);
			vec3 lightDiffuse = brightness * lights[i].colour.rgb;
			runningDiffuse  =  runningDiffuse + ((matDiffuseColour + lightDiffuse) * max(strFactor,0));
			count += 1;
		}
	}
	runningDiffuse = runningDiffuse / count;
	//outColour = vec4(lightDiffuse, 1) + (vec4(matAmbientColour, 1) );
	vec4 minAmb = vec4(matAmbientColour,alpha) * ambientBrightness;
	outColour = max((vec4(runningDiffuse,1) * vec4(matAmbientColour,alpha)), minAmb);
}