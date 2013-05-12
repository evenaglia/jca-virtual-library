package com.jivesoftware.jcalibrary;

import com.jivesoftware.jcalibrary.objects.VisualObjects;
import com.jivesoftware.jcalibrary.scheduler.WorkScheduler;
import com.jivesoftware.jcalibrary.structures.JiveInstance;
import com.jivesoftware.jcalibrary.structures.JiveInstanceState;
import com.jivesoftware.jcalibrary.structures.ServerRack;
import com.jivesoftware.jcalibrary.structures.ServerSlot;
import net.venaglia.realms.common.navigation.Position;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: ed
 * Date: 5/12/13
 * Time: 11:37 AM
 */
public class LibraryHUD {

    private static final Polygon CAMERA = new Polygon(
            new int[]{ 0, 3, 0, -3 },
            new int[]{ 6, -6, -3, -6 },
            4
    );

    private int radius = 384;
    private JFrame frame;
    private JPanel libraryHotspots;
    private JPanel activeInstanceDetail;
    private double cameraX, cameraY, cameraA;
    private AtomicReference<Collection<PointData>> data = new AtomicReference<Collection<PointData>>();

    public LibraryHUD(final ServerRack[] serverRacks) {
        this.frame = new JFrame("Overview");
        this.libraryHotspots = new JPanel() {
            @Override
            public void paint(Graphics graphics) {
                Graphics2D g = (Graphics2D)graphics;
                int width = frame.getWidth();
                int height = frame.getHeight();
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, width, height);
                g.translate(width / 2, height / 2 - 10);
                g.scale(radius / -20.f, radius / 20.f);
                drawLibrary(g);
                drawCamera(g);
                drawInstances(g);
            }
        };
        this.frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                radius = Math.round(Math.min(frame.getHeight(), frame.getWidth()) * 0.45f);
            }
        });
        this.frame.add(libraryHotspots);
        this.frame.setSize(radius << 1, radius << 1);
        this.frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        WorkScheduler.interval(new Runnable() {

            private final int dataSize = 4096;
            private PointData[][] pointData = {
                    new PointData[dataSize],
                    new PointData[dataSize],
                    new PointData[dataSize]
            };
            private int writeData = 0;

            {
                for (PointData[] data : pointData) {
                    for (int i = 0; i < data.length; i++) {
                        data[i] = new PointData();
                        data[i] = new PointData();
                    }
                }
            }

            public void run() {
                int i = 0;
                PointData[] allData = pointData[writeData];
                double[] xys = {0,0,1};
                long now = System.currentTimeMillis();
                for (ServerRack serverRack : serverRacks) {
                    for (ServerSlot slot : serverRack.getSlots()) {
                        JiveInstance jiveInstance = slot.getJiveInstance();
                        if (jiveInstance != null) {
                            jiveInstance.checkAndClearDirty();
                            PointData data = allData[i++];
                            slot.getSlotTransformation().getXYScale(xys);
                            data.x = Math.round((float)xys[0] * 100.0f);
                            data.y = Math.round((float)xys[1] * 100.0f);
                            data.scale = (float)xys[2];
                            JiveInstanceState state = VisualObjects.OVERALL_STATE.get(jiveInstance);
                            data.state = state;
                            net.venaglia.realms.common.physical.decorators.Color color = state.getColor(now);
                            data.rgb = toInt(color.r,16) | toInt(color.g,8) | toInt(color.b,0);
                        }
                    }
                }
                data.set(Arrays.asList(allData).subList(0,i));
                writeData = (writeData + 1) % pointData.length;
            }

            private int toInt(float f, int bits) {
                return Math.min(255, Math.max(0, Math.round(f * 255))) << bits;
            }
        }, 33, TimeUnit.MILLISECONDS);
    }

    private void drawLibrary(Graphics2D g) {
        g.setColor(Color.CYAN);
        g.setStroke(new BasicStroke(0.125f));
        g.drawOval(-20,-20,40,40);
        g.drawOval(-14,-14,28,28);
        g.drawOval(-4,-4,8,8);
    }

    private void drawCamera(Graphics2D g) {
        AffineTransform transform = g.getTransform();
        AffineTransform copy = new AffineTransform(transform);
        copy.translate(cameraX, cameraY);
        copy.rotate(cameraA);
        copy.scale(0.1, 0.1);
        g.setTransform(copy);
        g.setColor(Color.WHITE);
        g.fill(CAMERA);
        g.setTransform(transform);
    }

    private void drawInstances(Graphics2D g) {
        AffineTransform transform = g.getTransform();
        Collection<PointData> allData = this.data.get();
        if (allData == null || allData.isEmpty()) {
            return;
        }
        g.scale(0.01,0.01);
        PointData lastData = null;
        for (PointData data : allData) {
            if (data.state != JiveInstanceState.EMPTY_SLOT) {
                if (!data.colorEquals(lastData)) {
                    g.setColor(new Color(data.rgb));
                }
                int r = Math.round(data.scale * 15.0f);
                g.fillOval(data.x - r, data.y - r, r<<1, r<<1);
            }
        }
        g.setTransform(transform);
    }

    public void updateCamera(Position position) {
        cameraX = position.cameraX;
        cameraY = position.cameraY;
        cameraA = position.heading;
        libraryHotspots.repaint();
    }

    public void show() {
        this.frame.setVisible(true);
    }

    private static class PointData implements Comparable<PointData> {
        int x,y;
        float scale;
        int rgb;
        JiveInstanceState state = JiveInstanceState.EMPTY_SLOT;

        @Override
        public int compareTo(PointData o) {
            return state.compareTo(o.state);
        }

        public boolean colorEquals(PointData data) {
            return data != null && data.rgb == rgb;
        }

        @Override
        public String toString() {
            return String.format("PointData(%.2f,%.2f,%6x)", x / 100.0f, y / 100.0f, rgb);
        }
    }
}
