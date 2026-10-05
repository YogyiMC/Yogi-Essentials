#version 330

uniform sampler2D InSampler;
uniform sampler2D HistorySampler;

layout(std140) uniform YogiBlurParams {
    float PreviousWeight;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 now = texture(InSampler, texCoord);
    vec4 before = texture(HistorySampler, texCoord);
    vec3 blended = mix(now.rgb, before.rgb, clamp(PreviousWeight, 0.0, 0.99));
    fragColor = vec4(blended, 1.0);
}
