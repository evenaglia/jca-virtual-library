package net.venaglia.realms.common.navigation;

import net.venaglia.realms.common.physical.bounds.BoundingSphere;
import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.projection.Camera;
import net.venaglia.realms.common.util.matrix.Matrix_1x4;
import net.venaglia.realms.common.util.matrix.Matrix_4x4;
import net.venaglia.realms.common.view.KeyboardManager;
import net.venaglia.realms.common.view.ViewEventHandler;
import org.lwjgl.input.Keyboard;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * User: ed
 * Date: 4/16/13
 * Time: 8:09 AM
 */
public class UserNavigation implements ViewEventHandler {

    private static final double ONE_DEGREE = Math.PI / 180.0;

    private final KeyboardManager keyboardManager;
    private final Map<Integer,Move> movesByKeyCode = new HashMap<Integer,Move>();
    private final Position nextPosition = new Position();
    private final Matrix_4x4 cameraRotation = new Matrix_4x4();
    private final Matrix_4x4 temp = new Matrix_4x4();
    private final Set<Move> activeMoves = new HashSet<Move>();
    private final Set<Move> activeMovesReadOnly = Collections.unmodifiableSet(activeMoves);

    public final Move FORWARD = new Move() {
                public void update(double elapsedSeconds, Position position, Set<Move> allActiveMoves) {
                    position.doMove(elapsedSeconds * movePerSecond);
                }
            };
    public final Move BACKWARD = new Move() {
                public void update(double elapsedSeconds, Position position, Set<Move> allActiveMoves) {
                    position.doMove(elapsedSeconds * -movePerSecond);
                }
            };
    public final Move TURN_LEFT = new Move() {
                public void update(double elapsedSeconds, Position position, Set<Move> allActiveMoves) {
                    if (activeMoves.contains(BACKWARD) && !activeMoves.contains(FORWARD)) {
                        position.heading = position.heading - turnPerSecond * ONE_DEGREE * elapsedSeconds;
                    } else {
                        position.heading = position.heading + turnPerSecond * ONE_DEGREE * elapsedSeconds;
                    }
                }
            };
    public final Move TURN_RIGHT = new Move() {
                public void update(double elapsedSeconds, Position position, Set<Move> allActiveMoves) {
                    if (activeMoves.contains(BACKWARD) && !activeMoves.contains(FORWARD)) {
                        position.heading = position.heading + turnPerSecond * ONE_DEGREE * elapsedSeconds;
                    } else {
                        position.heading = position.heading - turnPerSecond * ONE_DEGREE * elapsedSeconds;
                    }
                }
            };
    public final Move LOOK_UP = new Move() {
                public void update(double elapsedSeconds, Position position, Set<Move> allActiveMoves) {
                    position.pitch = Math.min(position.pitch + pitchPerSecond * ONE_DEGREE * elapsedSeconds, 1.5);
                }
            };
    public final Move LOOK_DOWN = new Move() {
                public void update(double elapsedSeconds, Position position, Set<Move> allActiveMoves) {
                    position.pitch = Math.max(position.pitch - pitchPerSecond * ONE_DEGREE * elapsedSeconds, -1.5);
                }
            };

    protected Camera camera;
    protected Position position = new Position();
    protected BoundingVolume<?> sceneBoundary;
    protected float minimumObjectDistance;
    protected double movePerSecond = 5.0;
    protected double turnPerSecond = 25.0; // 25 degrees per second
    protected double pitchPerSecond = 25.0; // 25 degrees per second
    protected long lastMoveMS = Long.MAX_VALUE;
    protected boolean forceCameraUpdate = true;

    public UserNavigation(KeyboardManager keyboardManager) {
        this(keyboardManager, null);
    }

    public UserNavigation(KeyboardManager keyboardManager, Camera camera) {
        this(keyboardManager, camera, null);
    }

    public UserNavigation(KeyboardManager keyboardManager, Camera camera, BoundingVolume<?> sceneBoundary) {
        this(keyboardManager, camera, sceneBoundary, 0.1f,
             Keyboard.KEY_UP, Keyboard.KEY_DOWN,
             Keyboard.KEY_LEFT, Keyboard.KEY_RIGHT,
             Keyboard.KEY_LBRACKET, Keyboard.KEY_RBRACKET);
    }

    public UserNavigation(KeyboardManager keyboardManager,
                          Camera camera,
                          BoundingVolume<?> sceneBoundary,
                          float minimumObjectDistance,
                          int forwardKey,
                          int backwardKey,
                          int turnLeftKey,
                          int turnRightKey,
                          int lookUpKey,
                          int lookDownKey) {
        this.keyboardManager = keyboardManager;
        this.camera = camera;
        this.sceneBoundary = sceneBoundary;
        this.minimumObjectDistance = minimumObjectDistance;
        bindMoveToKey(FORWARD, forwardKey);
        bindMoveToKey(BACKWARD, backwardKey);
        bindMoveToKey(TURN_LEFT, turnLeftKey);
        bindMoveToKey(TURN_RIGHT, turnRightKey);
        bindMoveToKey(LOOK_UP, lookUpKey);
        bindMoveToKey(LOOK_DOWN, lookDownKey);
    }

    public void bindMoveToKey(final Move move, int keyCode) {
        if (move == null) {
            movesByKeyCode.remove(keyCode);
        } else if (keyCode > 0) {
            movesByKeyCode.put(keyCode, move);
        }
    }

    public void unbindMove(final Move move) {
        for (Iterator<Map.Entry<Integer, Move>> iterator = movesByKeyCode.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Integer, Move> entry = iterator.next();
            if (entry.getValue() == move) {
                iterator.remove();
            }
        }
    }

    protected void update(double elapsedSeconds, Position nextPosition) {
        activeMoves.clear();
        for (Map.Entry<Integer,Move> entry : movesByKeyCode.entrySet()) {
            if (keyboardManager.isDown(entry.getKey())) {
                activeMoves.add(entry.getValue());
            }
        }
        for (Move move : activeMoves) {
            move.update(elapsedSeconds, nextPosition, activeMovesReadOnly);
        }
    }

    protected void recomputeCamera(Position position) {
        Point cameraPosition = new Point(position.cameraX, position.cameraY, position.cameraZ);
        camera.setPosition(cameraPosition);
        cameraRotation.loadIdentity();
        Matrix_4x4.product(cameraRotation, temp.loadRotation(Axis.Y, position.roll));
        Matrix_4x4.product(cameraRotation, temp.loadRotation(Axis.Z, position.heading));
        Matrix_4x4.product(cameraRotation, temp.loadRotation(Axis.X, -position.pitch));
        Vector direction = cameraRotation.product(0, 1, 0, Matrix_1x4.View.VECTOR);
        camera.setDirection(direction);
        camera.setRight(cameraRotation.product(-1, 0, 0, Matrix_1x4.View.VECTOR));
        position.moveX = direction.i;
        position.moveY = direction.j;
        BoundingSphere boundingSphere = sceneBoundary.asSphere();
        double d = Vector.computeDistance(cameraPosition, boundingSphere.center);
        float nearClippingDistance = Math.max(minimumObjectDistance, (float)(d - boundingSphere.radius));
        float farClippingDistance = (float)(d + boundingSphere.radius);
        camera.setClippingDistance(nearClippingDistance, farClippingDistance);
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
        forceCameraUpdate();
    }

    public void setPosition(Position position) {
        this.position.setPosition(position);
        forceCameraUpdate();
    }

    protected final void forceCameraUpdate() {
        this.forceCameraUpdate = true;
    }

    public void handleInit() {
        // no-op
    }

    public void handleClose() {
        // no-op
    }

    public void handleNewFrame(long now) {
        double elapsedSeconds = (now - lastMoveMS) / 1000.0;
        lastMoveMS = now;
        if (elapsedSeconds > 0) {
            nextPosition.setPosition(position);
            update(elapsedSeconds, nextPosition);
            if (forceCameraUpdate || !nextPosition.equals(position)) {
                nextPosition.normalize();
                forceCameraUpdate = false;
                position.setPosition(nextPosition);
                recomputeCamera(position);
            }
        }
    }

    interface Move {
        void update(double elapsedSeconds, Position position, Set<Move> allActiveMoves);
    }

}
