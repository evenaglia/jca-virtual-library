package net.venaglia.realms.common.util.debug;

import net.venaglia.realms.common.physical.geom.*;
import net.venaglia.realms.common.projection.Camera;

import javax.swing.*;
import java.awt.*;

/**
 * User: ed
 * Date: 9/7/12
 * Time: 9:44 PM
 */
public class CameraWindow extends JFrame {

    private final Camera camera;
    private boolean disposed = false;

    public CameraWindow(Camera camera, String title) throws HeadlessException {
        super(title);
        this.setSize(240, 160);
        this.camera = camera;
        Thread thread = new Thread(new Runnable() {
            public void run() {
                int expectedModCount = Integer.MIN_VALUE;
                while (!disposed) {
                    try {
                        if (isVisible() && expectedModCount != CameraWindow.this.camera.getModCount()) {
                            expectedModCount = CameraWindow.this.camera.getModCount();
                            invalidate();
                        }
                        Thread.sleep(8);
                    } catch (InterruptedException e) {
                        // don't care
                    }
                }
            }
        }, "Camera window");
        thread.setDaemon(true);
        thread.start();
    }

    public CameraWindow(Camera camera) throws HeadlessException {
        this(camera, "Camera");
    }

    @Override
    public void paint(Graphics graphics) {
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.fillRect(0,0,240,160);
        net.venaglia.realms.common.physical.geom.Point eye = camera.getPosition();
        net.venaglia.realms.common.physical.geom.Point at = eye.translate(camera.getDirection());
        Vector up = camera.getUp().normalize();
        int y = -15;
        graphics.drawString("  eye: " + eye, 10, y += 25);
        graphics.drawString("  dir: " + camera.getDirection(), 10, y += 25);
        graphics.drawString("   at: " + at, 10, y += 25);
        graphics.drawString("right: " + camera.getRight(), 10, y += 25);
        graphics.drawString("   up: " + up, 10, y += 25);
        graphics.drawString(" near: " + camera.getNearClippingDistance(), 10, y += 25);
        graphics.drawString("  far: " + camera.getNearClippingDistance(), 10, y += 25);
        graphics.drawString(" view: " + camera.getViewAngle() + "\u00BA", 10, y += 25);
    }

    @Override
    public void dispose() {
        super.dispose();
        this.disposed = true;
    }
}
