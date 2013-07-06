package net.venaglia.gloo.view;

import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.projection.Camera;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.ProjectionBuffer;
import net.venaglia.gloo.projection.camera.PerspectiveCamera;
import net.venaglia.gloo.projection.impl.SimpleSelectObserver;
import net.venaglia.gloo.projection.shaders.ShaderProgram;
import net.venaglia.gloo.projection.impl.DirectProjectionBuffer;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.Dimension;

import static net.venaglia.gloo.util.CallLogger.*;
import static net.venaglia.gloo.view.MouseTargetEventListener.MouseButton;
import static org.lwjgl.opengl.GL11.*;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: ed
 * Date: 9/6/12
 * Time: 6:23 PM
 */
public class View3D extends Thread implements Closeable {

    public static final int ALPHA_CHANNEL = 0x0001;
    public static final int DEPTH_MASK = 0x0002;
    public static final int VSYNC = 0x0004;
    public static final int MOUSE_TARGETS = 0x0008;
    public static final int OVERLAY_2D = 0x0010;

    private static final int TITLE_DIRTY_BIT = 1;

    private final Dimension dimension;
    private final int options;
    private final List<ViewEventHandler> viewEventHandlers = new LinkedList<ViewEventHandler>();
    private final List<KeyboardEventHandler> keyboardEventHandlers = new LinkedList<KeyboardEventHandler>();
    private final KeyboardKeyHandlers keyboardKeyHandlers = new KeyboardKeyHandlers();

    private String title = "3D View";
    private Color backgroundColor = Color.BLACK;
    private Camera camera = new PerspectiveCamera();
    private boolean requestClose = false;
    private Brush brush;
    private ShaderProgram shader;
    private View3DMainLoop mainLoop;
    private MouseTarget<?> lastMouseTarget;
    private FramesPerSecond fps;
    private SimpleSelectObserver selectObserver;
    private Map<MouseTargetEventListener.MouseButton,MouseTarget<?>> buttonsDown;

    private AtomicInteger dirtyBits = new AtomicInteger(TITLE_DIRTY_BIT);

    public View3D(int width, int height) {
        this(width, height, ALPHA_CHANNEL | DEPTH_MASK | VSYNC | MOUSE_TARGETS | OVERLAY_2D);
    }

    public View3D(int width, int height, int options) {
        super(new ThreadGroup("3D View"), "Rendering Loop");
        keyboardEventHandlers.add(keyboardKeyHandlers);
        this.dimension = new Dimension(width, height);
        this.options = options;
        this.brush = Brush.TEXTURED;
        this.shader = ShaderProgram.DEFAULT_SHADER;
        this.buttonsDown = new EnumMap<MouseButton,MouseTarget<?>>(MouseButton.class);
    }

    @Override
    public void run() {
        prepareWindow();
        setupGL();
        ProjectionBuffer buffer = new DirectProjectionBuffer();
        buffer.useCamera(camera);
        buffer.applyBrush(brush);
        buffer.useShader(shader);
        buffer.resetAllStacks();
        userEventInit();
        Display.update();
        int frameCount = 0;
        while (!(requestClose || Display.isCloseRequested())) {
            long now = System.currentTimeMillis();
            if (fps != null) {
                fps.tick();
            }
            if (logCalls) {
                if (frameCount > 2) System.exit(0);
                logMessage("============================================================================== Begin frame " + (++frameCount) + " ============");
            }
            if (userBeforeFrame(now)) {
                updateDirtyValues();
                userNewFrame(now);
                processTargets(buffer, now);
                processMouseButtons();
                glColorMask(true, true, true, true);
                if (logCalls) logCall("glColorMask", true, true, true, true);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                if (logCalls) logCall("glClear", orBits(GL_COLOR_BUFFER_BIT, GL_DEPTH_BUFFER_BIT));
                buffer.useCamera(camera);
                buffer.applyBrush(brush);
                buffer.useShader(shader);
                buffer.resetAllStacks();
                userRenderFrame(buffer, now);
                userRenderOverlay(buffer, now);
                userAfterFrame(now);
            } else {
                updateDirtyValues();
            }
            while (Keyboard.next()) {
                userEventKeyPress(Keyboard.getEventKey(), Keyboard.getEventKeyState());
            }
            Display.update();
        }
        userEventExit();
    }

    private void processTargets(ProjectionBuffer buffer, long now) {
        if ((options & MOUSE_TARGETS) == 0 || mainLoop == null) {
            return;
        }
        MouseTargets targets = mainLoop.getMouseTargets(now);
        if (targets == null || targets.isEmpty()) {
            return;
        }
        if (selectObserver == null) {
            selectObserver = new SimpleSelectObserver();
        }
        int mouseX = Mouse.getX();
        int mouseY = Mouse.getY();
        MouseTarget<?> mouseTarget = null;
        while (!(targets == null || targets.isEmpty())) {
            GeometryBuffer targetBuffer = buffer.beginCameraSelect(camera, mouseX, mouseY);
            userRenderTargets(buffer, targetBuffer, now, targets);
            selectObserver.reset();
            buffer.endCameraSelect(selectObserver);
            int name = selectObserver.getClosestName();
            mouseTarget = name == Integer.MIN_VALUE ? null : targets.getByName(name);
            targets = mouseTarget == null ? null : mouseTarget.getChildren();
        }
        if (mouseTarget != lastMouseTarget) {
            fireMouseEvent(lastMouseTarget, null, 2);
            fireMouseEvent(mouseTarget, null, 1);
            lastMouseTarget = mouseTarget;
        }
    }

    private void processMouseButtons() {
        for (MouseButton mouseButton : MouseButton.values()) {
            boolean wasPressed = buttonsDown.containsKey(mouseButton);
            boolean isPressed = Mouse.isButtonDown(mouseButton.glCode);
            if (wasPressed != isPressed) {
                if (isPressed && lastMouseTarget != null) {
                    fireMouseEvent(lastMouseTarget, mouseButton, 3);
                    buttonsDown.put(mouseButton, lastMouseTarget);
                } else if (wasPressed) {
                    fireMouseEvent(buttonsDown.remove(mouseButton), mouseButton, 4);
                }
            }
        }
    }

    protected <V> void fireMouseEvent(MouseTarget<V> target, MouseButton button, int event) {
        MouseTargetEventListener<? super V> listener = target == null ? null : target.getListener();
        if (listener == null) {
            return;
        }
        try {
            switch (event) {
                case 1: // mouseOver
                    listener.mouseOver(target);
                    break;
                case 2: // mouseOut
                    listener.mouseOut(target);
                    break;
                case 3: // mouseDown
                    listener.mouseDown(target, button);
                    break;
                case 4: // mouseUp
                    listener.mouseUp(target, button);
                    break;
            }
        } catch (Throwable t) {
            getUncaughtExceptionHandler().uncaughtException(this, t);
        }
    }

    private boolean userBeforeFrame(long now) {
        try {
            if (mainLoop != null) {
                return mainLoop.beforeFrame(now);
            }
        } catch (Throwable t) {
            getUncaughtExceptionHandler().uncaughtException(this, t);
        }
        return false;
    }

    private void userNewFrame(long now) {
        for (ViewEventHandler handler : viewEventHandlers) {
            try {
                if (handler != null) {
                    handler.handleNewFrame(now);
                }
            } catch (Throwable t) {
                getUncaughtExceptionHandler().uncaughtException(this, t);
            }
        }
    }

    protected void userRenderTargets(ProjectionBuffer buffer,
                                     GeometryBuffer targetBuffer,
                                     long now,
                                     MouseTargets targets) {
        try {
            for (MouseTarget<?> target : targets) {
                buffer.loadName(target.getGlName());
                target.getProjectableObject().project(now, targetBuffer);
            }
        } catch (Throwable t) {
            getUncaughtExceptionHandler().uncaughtException(this, t);
        }
    }

    private void userRenderFrame(ProjectionBuffer buffer, long now) {
        try {
            if (mainLoop != null) {
                mainLoop.renderFrame(now, buffer);
            }
        } catch (Throwable t) {
            getUncaughtExceptionHandler().uncaughtException(this, t);
        }
    }

    private void userRenderOverlay(ProjectionBuffer buffer, long now) {
        if ((options & OVERLAY_2D) == 0 || mainLoop == null) {
            return;
        }
        try {
            mainLoop.renderOverlay(now, buffer.beginOverlay());
        } catch (Throwable t) {
            getUncaughtExceptionHandler().uncaughtException(this, t);
        } finally {
            buffer.endOverlay();
        }
    }

    private void userAfterFrame(long now) {
        try {
            if (mainLoop != null) {
                mainLoop.afterFrame(now);
            }
        } catch (Throwable t) {
            getUncaughtExceptionHandler().uncaughtException(this, t);
        }
    }

    private void userEventInit() {
        for (ViewEventHandler viewEventHandler : viewEventHandlers) {
            try {
                viewEventHandler.handleInit();
            } catch (Throwable t) {
                getUncaughtExceptionHandler().uncaughtException(this, t);
            }
        }
    }

    private void userEventKeyPress(int keyCode, boolean down) {
        for (KeyboardEventHandler handler : keyboardEventHandlers) {
            try {
                if (handler != null) {
                    if (down) {
                        handler.keyDown(keyCode);
                    } else {
                        handler.keyUp(keyCode);
                    }
                }
            } catch (Throwable t) {
                getUncaughtExceptionHandler().uncaughtException(this, t);
            }
        }
    }

    private void userEventExit() {
        try {
            Display.destroy();
        } finally {
            for (ViewEventHandler viewEventHandler : viewEventHandlers) {
                try {
                    if (viewEventHandler != null) {
                        viewEventHandler.handleClose();
                    }
                } catch (Throwable t) {
                    getUncaughtExceptionHandler().uncaughtException(this, t);
                }
            }
        }
    }

    public void setTitle(String title) {
        if (!this.title.equals(title)) {
            setDirty(TITLE_DIRTY_BIT);
            this.title = title == null ? "3D View" : title;
        }
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setCamera(Camera camera) {
        if (camera == null) {
            throw new NullPointerException("camera");
        }
        this.camera = camera;
    }

    public void setMainLoop(View3DMainLoop mainLoop) {
        this.mainLoop = mainLoop;
    }

    public void addViewEventHandler(ViewEventHandler viewEventHandler) {
        this.viewEventHandlers.add(viewEventHandler);
    }

    public void removeViewEventHandler(ViewEventHandler viewEventHandler) {
        this.viewEventHandlers.remove(viewEventHandler);
    }

    public void addKeyboardEventHandler(KeyboardEventHandler keyboardEventHandler) {
        this.keyboardEventHandlers.add(keyboardEventHandler);
    }

    public void removeKeyboardEventHandler(KeyboardEventHandler keyboardEventHandler) {
        this.keyboardEventHandlers.remove(keyboardEventHandler);
    }

    public void registerKeyHandlers(KeyHandler... keyHandlers) {
        registerKeyHandlers(Arrays.asList(keyHandlers));
    }

    public void registerKeyHandlers(Collection<KeyHandler> keyHandlers) {
        for (KeyHandler keyHandler : keyHandlers) {
            if (keyboardKeyHandlers.keyHandlers[keyHandler.keyCode] != null) {
                throw new IllegalArgumentException("Multiple key handlers specified or the same key code: " + keyHandler.keyCode);
            }
            keyboardKeyHandlers.keyHandlers[keyHandler.keyCode] = keyHandler;
        }
    }

    public boolean unregisterKeyHandler(KeyHandler keyHandler) {
        if (keyboardKeyHandlers.keyHandlers[keyHandler.keyCode] != keyHandler) {
            return false;
        }
        keyboardKeyHandlers.keyHandlers[keyHandler.keyCode] = null;
        return true;
    }

    public void setFPS(FramesPerSecond fps) {
        this.fps = fps;
    }

    private void prepareWindow() {
        try {
            Display.setDisplayMode(new DisplayMode(dimension.getWidth(), dimension.getHeight()));
            int alpha = (options & ALPHA_CHANNEL) == ALPHA_CHANNEL ? 8 : 0;
            int depth = (options & DEPTH_MASK) == DEPTH_MASK ? 16 : 0;
            PixelFormat pixel_format = new PixelFormat(32, alpha, depth, 0, 4);
            Display.create(pixel_format);
            if ((options & VSYNC) == VSYNC) {
                Display.setVSyncEnabled(true);
            }
        } catch (LWJGLException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void setupGL() {
        glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
        if (logCalls) logCall("glColorMaterial", GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
        glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a);
        if (logCalls) logCall("glClearColor", backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a);
        glColorMask(true, true, true, true);
        if (logCalls) logCall("glColorMask", true, true, true, true);
        glViewport(0, 0, dimension.getWidth(), dimension.getHeight());
        if (logCalls) logCall("glViewport", 0, 0, (long)dimension.getWidth(), (long)dimension.getHeight());
        glMatrixMode(GL_MODELVIEW);
        if (logCalls) logCall("glMatrixMode", GL_MODELVIEW);
        if ((options & DEPTH_MASK) == DEPTH_MASK) {
            glEnable(GL_DEPTH_TEST);
            if (logCalls) logCall("glEnable", GL_DEPTH_TEST);
        }
    }

    private void updateDirtyValues() {
        if (dirtyBits.get() != 0) {
            if (checkDirty(TITLE_DIRTY_BIT)) {
                Display.setTitle(title);
            }
        }
    }

    private void setDirty(int bit) {
        int oldValue;
        do {
            oldValue = dirtyBits.get();
            if ((oldValue & bit) == bit) {
                return; // nothing to do, bit is already set
            }
        }
        while (!dirtyBits.compareAndSet(oldValue, oldValue | bit));
    }

    private boolean checkDirty(int bit) {
        int oldValue;
        do {
            oldValue = dirtyBits.get();
            if ((oldValue & bit) == 0) {
                return false;
            }
        }
        while (!dirtyBits.compareAndSet(oldValue, oldValue & ~bit));
        return true;
    }

    public void close() {
        requestClose = true;
    }

    public void setDefaultBrush(Brush brush) {
        this.brush = brush;
    }

    public void setDefaultShader(ShaderProgram shader) {
        this.shader = shader;
    }

    private static class KeyboardKeyHandlers implements KeyboardEventHandler {

        final KeyHandler[] keyHandlers = new KeyHandler[256];

        public void keyDown(int keyCode) {
            KeyHandler keyHandler = keyHandlers[keyCode];
            if (keyHandler != null) {
                keyHandler.handleKeyDown(keyCode);
            }
        }

        public void keyUp(int keyCode) {
            KeyHandler keyHandler = keyHandlers[keyCode];
            if (keyHandler != null) {
                keyHandler.handleKeyUp(keyCode);
            }
        }
    }
}
