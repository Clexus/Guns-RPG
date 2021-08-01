package lib.toma.animations;

import com.mojang.blaze3d.matrix.MatrixStack;
import lib.toma.animations.pipeline.AnimationStage;
import lib.toma.animations.pipeline.TickableAnimation;
import lib.toma.animations.pipeline.frame.IFrameProviderInstance;
import lib.toma.animations.pipeline.frame.IKeyframe;
import lib.toma.animations.pipeline.frame.Keyframes;

public class Animation extends TickableAnimation {

    private final IFrameProviderInstance provider;

    public Animation(IFrameProviderInstance provider, int length) {
        super(length);
        this.provider = provider;
    }

    @Override
    public void animate(AnimationStage stage, MatrixStack matrix) {
        if (!provider.canAnimate(stage)) return;
        float progressInterpolated = getIntepolatedProgress();
        IKeyframe currentFrame = provider.getCurrentFrame(stage, progressInterpolated);
        IKeyframe lastFrame = provider.getPreviousFrame(stage);
        float activeEndpoint = currentFrame.endpoint();
        float prevEndpoint = lastFrame.endpoint();
        float stageProgress = (progressInterpolated - prevEndpoint) / (activeEndpoint - prevEndpoint);
        Keyframes.processFrame(currentFrame, stageProgress, matrix);
    }

    @Override
    public void nextFrame(float actualProgress, float previousProgress) {
        provider.onAnimationProgressed(actualProgress, previousProgress, this);
    }
}
