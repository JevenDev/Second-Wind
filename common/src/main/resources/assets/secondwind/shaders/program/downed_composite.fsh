#version 150

uniform sampler2D DiffuseSampler;

uniform vec2 InSize;
uniform float time;
uniform float aspectRatio;
uniform float DownedBlend;
uniform float Urgency;
uniform float PulseStrength;
uniform float VignetteStrength;
uniform float DesaturationStrength;
uniform float TintStrength;
uniform float BloomStrength;

in vec2 texCoord;
out vec4 fragColor;

float luma(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

float hash(vec2 position) {
    return fract(sin(dot(position, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 position) {
    vec2 cell = floor(position);
    vec2 fraction = fract(position);
    vec2 weight = fraction * fraction * (3.0 - 2.0 * fraction);
    return mix(mix(hash(cell), hash(cell + vec2(1.0, 0.0)), weight.x),
            mix(hash(cell + vec2(0.0, 1.0)), hash(cell + vec2(1.0, 1.0)), weight.x),
            weight.y);
}

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);
    vec2 centeredUv = texCoord * 2.0 - 1.0;

    float heartbeatPhase = fract(time * 0.95);
    float pulse = 1.0 - smoothstep(0.0, 0.10, abs(heartbeatPhase - 0.18));
    pulse += (1.0 - smoothstep(0.0, 0.13, abs(heartbeatPhase - 0.38))) * 0.45;
    float glowPulse = 0.96 + pulse * (0.01 + PulseStrength * 0.08);

    vec3 sceneColor = original.rgb;
    float edgeStrength = 0.0;
    float mist = 0.0;
    if (VignetteStrength > 0.0) {
        vec2 screenUv = vec2(centeredUv.x * aspectRatio, centeredUv.y);
        vec2 mistUv = screenUv * 4.0;
        mist = noise(mistUv + vec2(time * 0.09, -time * 0.12)) * 0.65
                + noise(mistUv * 2.1 - vec2(time * 0.07, time * 0.05)) * 0.35;

        // fourth-power radius follows the edges without narrowing the center on wide displays
        float edgeRadius = sqrt(length(centeredUv * centeredUv));
        float edgeMask = smoothstep(0.60, 1.0 - Urgency * 0.06,
                edgeRadius + (mist - 0.5) * 0.12);
        edgeStrength = edgeMask * VignetteStrength;

        if (edgeStrength > 0.0) {
            vec2 radialStep = screenUv / max(length(screenUv), 0.001)
                    / vec2(aspectRatio, 1.0) / 1080.0;
            vec2 halfTexel = vec2(0.5) / InSize;
            vec2 distortion = radialStep * edgeStrength * ((mist - 0.5) * 3.0 + pulse * PulseStrength * 12.0);
            vec2 fringe = radialStep * edgeStrength * (0.6 + Urgency * 1.6 + pulse * PulseStrength * 8.0);
            vec2 sampleUv = texCoord + distortion;
            sceneColor = vec3(
                    texture(DiffuseSampler, clamp(sampleUv + fringe, halfTexel, 1.0 - halfTexel)).r,
                    texture(DiffuseSampler, clamp(sampleUv, halfTexel, 1.0 - halfTexel)).g,
                    texture(DiffuseSampler, clamp(sampleUv - fringe, halfTexel, 1.0 - halfTexel)).b);
        }
    }

    float grayscale = luma(sceneColor);
    vec3 desaturated = mix(sceneColor, vec3(grayscale), DesaturationStrength);
    vec3 bloodTint = vec3(grayscale * 1.08, grayscale * 0.52, grayscale * 0.54)
            + vec3(0.12, 0.0, 0.0) * Urgency;
    vec3 graded = mix(desaturated, bloodTint, TintStrength);

    float vignetteDarkness = edgeStrength * (0.60 + Urgency * 0.10 + pulse * PulseStrength);
    vec3 vignetted = graded * (1.0 - vignetteDarkness);
    vec3 edgeGlow = vec3(0.20, 0.008, 0.018) * edgeStrength * (0.35 + mist * 0.65)
            * (0.75 + Urgency * 0.55 + pulse * PulseStrength * 5.0);

    vec3 bloom = vec3(0.0);
    if (BloomStrength > 0.0) {
        vec2 offset = vec2(3.0) / InSize;
        vec3 blurred = (texture(DiffuseSampler, texCoord + offset).rgb
                + texture(DiffuseSampler, texCoord - offset).rgb
                + texture(DiffuseSampler, texCoord + vec2(offset.x, -offset.y)).rgb
                + texture(DiffuseSampler, texCoord + vec2(-offset.x, offset.y)).rgb) * 0.25;
        bloom = max(blurred - vec3(0.55), vec3(0.0)) * BloomStrength * glowPulse;
    }

    vec3 finalColor = mix(original.rgb, vignetted + edgeGlow + bloom, DownedBlend);
    fragColor = vec4(clamp(finalColor, 0.0, 1.0), original.a);
}
