package lib.toma.animations.pipeline.frame;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import lib.toma.animations.pipeline.AnimationStage;
import lib.toma.animations.pipeline.event.IAnimationEvent;
import lib.toma.animations.serialization.IKeyframeTypeSerializer;

public final class NoFramesProvider implements IKeyframeProvider {

    private static final NoFramesProvider NO_FRAMES = new NoFramesProvider();

    private NoFramesProvider() {}

    public static IKeyframeProvider empty() {
        return NO_FRAMES;
    }

    @Override
    public FrameProviderType<?> getType() {
        return FrameProviderType.EMPTY;
    }

    @Override
    public boolean shouldAdvance(AnimationStage stage, float progress, byte frameIndex) {
        return false;
    }

    @Override
    public IKeyframe getCurrentFrame(AnimationStage stage, float progress, byte frameIndex) {
        return Keyframes.none();
    }

    @Override
    public IKeyframe getOldFrame(AnimationStage stage, byte frameIndex) {
        return Keyframes.none();
    }

    @Override
    public IAnimationEvent[] getEvents() {
        return IAnimationEvent.NO_EVENTS;
    }

    @Override
    public int getCacheSize() {
        return 0;
    }

    public static class Serializer implements IKeyframeTypeSerializer<NoFramesProvider> {

        @Override
        public void serialize(JsonObject data, NoFramesProvider provider, JsonSerializationContext context) {}

        @Override
        public NoFramesProvider deserialize(JsonObject source, JsonDeserializationContext context, IAnimationEvent[] events) throws JsonParseException {
            return NO_FRAMES;
        }
    }
}
