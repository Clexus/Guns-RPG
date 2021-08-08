package lib.toma.animations.pipeline.frame;

import lib.toma.animations.AnimationUtils;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

public class MutableKeyframe implements IKeyframe {

    public float endpoint;
    public Vector3d position = Vector3d.ZERO;
    public Vector3f scale = new Vector3f();
    public Quaternion rotation = Quaternion.ONE.copy();
    private Vector3d pos0 = Vector3d.ZERO;
    private Vector3f scale0 = AnimationUtils.DEFAULT_SCALE_VECTOR;
    private Quaternion quat0 = Quaternion.ONE;

    public static MutableKeyframe mutable(IKeyframe frame) {
        MutableKeyframe mkf = new MutableKeyframe();
        mkf.setEndpoint(frame.endpoint());
        mkf.setPosition(frame.positionTarget());
        mkf.setPos0(frame.initialPosition());
        mkf.setScale(frame.scaleTarget());
        mkf.setScale0(frame.initialScale());
        mkf.setRotation(frame.rotationTarget());
        mkf.setQuat0(frame.initialRotation());
        return mkf;
    }

    @Override
    public float endpoint() {
        return endpoint;
    }

    public void setEndpoint(float endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public Vector3d positionTarget() {
        return position;
    }

    public void setPosition(Vector3d position) {
        this.position = position;
    }

    @Override
    public Vector3f scaleTarget() {
        return scale;
    }

    public void setScale(Vector3f scale) {
        this.scale = scale;
    }

    @Override
    public Quaternion rotationTarget() {
        return rotation;
    }

    public void setRotation(Quaternion rotation) {
        this.rotation = rotation;
    }

    public void setRotation(Vector3f vec, float deg) {
        setRotation(new Quaternion(vec, deg, true));
    }

    @Override
    public Vector3d initialPosition() {
        return pos0;
    }

    public void setPos0(Vector3d pos0) {
        this.pos0 = pos0;
    }

    @Override
    public Vector3f initialScale() {
        return scale0;
    }

    public void setScale0(Vector3f scale0) {
        this.scale0 = scale0;
    }

    @Override
    public Quaternion initialRotation() {
        return quat0;
    }

    public void setQuat0(Quaternion quat0) {
        this.quat0 = quat0;
    }

    @Override
    public void baseOn(IKeyframe parent) {
        pos0 = Keyframes.getInitialPosition(parent);
        scale0 = Keyframes.getInitialScale(parent);
        quat0 = Keyframes.getInitialRotation(parent);
    }
}
