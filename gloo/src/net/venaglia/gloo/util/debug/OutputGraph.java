package net.venaglia.gloo.util.debug;

import net.venaglia.common.util.Series;
import net.venaglia.gloo.physical.geom.Vector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: ed
 * Date: 12/25/12
 * Time: 2:25 PM
 */
public class OutputGraph {

    private static AtomicReference<Color> MOUSE_OVER_HIGHLIGHT_COLOR = null; // new AtomicReference<Color>();

    private final double scale;

    private JFrame window;
    private Popover popover;
    private List<RenderedElement> elements = Collections.synchronizedList(new ArrayList<RenderedElement>());
    private List<RenderedMouseOver> mouseOvers = new ArrayList<RenderedMouseOver>();
    private Runnable runOnClose;

    public OutputGraph(String name, int size, double centerX, double centerY, double scale) {
        this(name, new Dimension(size, size), centerX, centerY, scale);
    }

    public OutputGraph(String name,
                       final Dimension windowDimension,
                       final double centerX,
                       final double centerY,
                       final double scale) {
        this.scale = scale;
        final PointXForm xf = new PointXForm() {
            public int processX(double x) {
                return (int)Math.round((x - centerX) * scale) + windowDimension.width / 2;
            }

            public int processY(double y) {
                return (int)Math.round((y - centerY) * -scale) + windowDimension.height / 2;
            }
        };
        if (MOUSE_OVER_HIGHLIGHT_COLOR != null) {
            Thread thread = new Thread(new Runnable() {
                @SuppressWarnings("InfiniteLoopStatement")
                public void run() {
                    while (true) {
                        double now = (System.currentTimeMillis() % 10000) * Math.PI / 10000.0;
                        MOUSE_OVER_HIGHLIGHT_COLOR.set(new Color(colorSin(now),
                                                                 colorSin(now + Math.PI / 3.0),
                                                                 colorSin(now - Math.PI / 3.0)));
                        window.invalidate();
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            // don't care
                        }
                    }
                }

                private float colorSin(double now) {
                    return (float)(Math.sin(now) * 0.5 + 0.5);
                }
            });
            thread.setDaemon(true);
            thread.start();
        }
        Rectangle maximumWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        JPanel frame = new JPanel() {

            private Font font = Font.decode(Font.SANS_SERIF + "-" + Font.PLAIN + "-8");

            @Override
            public void paint(Graphics graphics) {
                super.paint(graphics);
                Graphics2D g2d = (Graphics2D)graphics;
                g2d.setFont(font);
                g2d.setStroke(new BasicStroke(0.75f));
                for (RenderedElement e : elements.toArray(new RenderedElement[elements.size()])) {
                    e.render(g2d, xf);
                }
            }
        };
        frame.setBackground(Color.black);
        frame.setSize(windowDimension);
        frame.addMouseMotionListener(new MouseMotionAdapter() {

            private RenderedMouseOver lastActive = null;

            @Override
            public void mouseMoved(MouseEvent e) {
                if (mouseOvers.isEmpty()) return;
                RenderedMouseOver active = null;
                for (RenderedMouseOver mouseOver : mouseOvers) {
                    if (mouseOver.shape != null && mouseOver.shape.contains(e.getX(), e.getY())) {
                        active = mouseOver;
                        break;
                    }
                }
                if (active != null) {
                    popover.setLocation(e.getXOnScreen() - 10, e.getYOnScreen() + 24);
                    if (lastActive != active) {
                        popover.invalidate();
                        popover.setText(active.text);
                        lastActive = active;
                    }
                    popover.setVisible(true);
                } else {
                    popover.setVisible(false);
                }
            }
        });
        window = new JFrame();
        window.setName(name);
        if (windowDimension.width > 1024 || windowDimension.height > 1024) {
            JScrollPane pane = new JScrollPane(frame);
            frame.setPreferredSize(new Dimension(Math.min(windowDimension.width, 1024), Math.max(windowDimension.height, 1024)));
            window.add(pane);
        } else {
            frame.setSize(windowDimension);
            window.add(frame);
        }
        window.addWindowListener(new WindowAdapter() {

            @Override
            public void windowOpened(WindowEvent windowEvent) {
                window.setSize(windowDimension);
            }

            @Override
            public void windowClosing(WindowEvent windowEvent) {
                if (runOnClose != null) {
                    runOnClose.run();
                }
            }
        });
        window.setVisible(true);
        window.pack();
        if (window.getHeight() > maximumWindowBounds.height || windowDimension.getWidth() > maximumWindowBounds.width) {
            window.setSize(Math.min(window.getWidth(), windowDimension.width), Math.min(window.getHeight(), windowDimension.height));
        }
        popover = new Popover();
        popover.addPropertyChangeListener(Popover.RENDERED_SIZE, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                popover.setSize((Dimension)evt.getNewValue());
            }
        });
    }

    public void onClose(Runnable runOnClose) {
        this.runOnClose = runOnClose;
    }

    public void addPoint(Color color, String label, double x, double y) {
        elements.add(new RenderedPoint(x, y,
                                       color == null ? Color.white : color,
                                       label == null || label.length() == 0 ? null : label));
        window.repaint();
    }

    public void addPixels(Color color, double... points) {
        if (points.length < 2 || points.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        elements.add(new RenderedPixels(points,
                                        color == null ? Color.white : color));
        window.repaint();
    }

    public void addLine(Color color, double... points) {
        if (points.length < 4 || points.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        elements.add(new RenderedLine(color == null ? Color.white : color, points));
        window.repaint();
    }

    public void addCircle(Color color, String label, double x, double y, int r) {
        elements.add(new RenderedCircle(color == null ? Color.white : color,
                                        label == null || label.length() == 0 ? null : label,
                                        x, y, r));
        window.repaint();
    }

    public void addArrow(Color color, double... points) {
        if (points.length < 4 || points.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        elements.add(new RenderedArrow(color == null ? Color.white : color, points));
        window.repaint();
    }

    public void addPoly(Color color, double... points) {
        if (points.length < 4 || points.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        elements.add(new RenderedPoly(color == null ? Color.white : color, points));
        window.repaint();
    }

    public void addLabel(Color color, String label, double x, double y) {
        if (label != null && label.length() > 0) {
            elements.add(new RenderedLabel(x, y, label, color == null ? Color.white : color));
            window.repaint();
        }
    }

    public void addImage(Color color, BufferedImage image, String label, double x, double y) {
        if (image != null) {
            elements.add(new RenderedImage(x, y, image, (float)(1.0 / scale), label, color == null ? Color.white : color));
            window.repaint();
        }
    }

    public void addImage(Color color, BufferedImage image, String label, double x, double y, double scale) {
        if (image != null) {
            elements.add(new RenderedImage(x, y, image, (float)(scale / this.scale), label, color == null ? Color.white : color));
            window.repaint();
        }
    }

    public void addMouseOver(String text, Region region) {
        addMouseOver(new OutputTextBuffer().append(text), region);
    }

    public void addMouseOver(OutputTextBuffer text, Region region) {
        if (text != null && region != null && !region.close().isOpen()) {
            double[] regionBounds = getRegionBounds(region);
            RenderedMouseOver mouseOver = new RenderedMouseOver(regionBounds, !region.isFromLastElement(), text.close());
            elements.add(mouseOver);
            mouseOvers.add(mouseOver);
        }
    }

    private double[] getRegionBounds(Region region) {
        if (region.isFromLastElement()) {
            if (elements.isEmpty()) {
                throw new IllegalStateException("No previous element to calculate region from");
            }
            return elements.get(elements.size() - 1).getBounds();
        }
        return region.getBounds();
    }

    public <P> ProjectedOutputGraph<P> project(OutputGraphProjection<P> projection) {
        return new ProjectedOutputGraph<P>(this, projection);
    }

    public void clear() {
        elements.clear();
        mouseOvers.clear();
        popover.setVisible(false);
        popover.setText(new OutputTextBuffer().close());
        window.repaint();
    }

    private static interface PointXForm {

        int processX(double x);

        int processY(double y);
    }

    private enum Corner {
        UPPER_LEFT, UPPER_RIGHT, LOWER_RIGHT, LOWER_LEFT;

        double getX (double x1, double x2) {
            switch (this) {
                case UPPER_LEFT:
                case LOWER_LEFT:
                    return x1;
                default:
                    return x2;
            }
        }

        double getY (double y1, double y2) {
            switch (this) {
                case UPPER_LEFT:
                case UPPER_RIGHT:
                    return y1;
                default:
                    return y2;
            }
        }

        Series<Corner> goCW() {
            return asSeriesImpl(this.ordinal(), 1);
        }

        private Series<Corner> asSeriesImpl(final int start, final int dir) {
            return new Series<Corner>() {

                public int size() {
                    return 4;
                }

                public Iterator<Corner> iterator() {
                    return new Iterator<Corner>() {

                        private int current = start;
                        private int remaining = 4;

                        public boolean hasNext() {
                            return remaining > 0;
                        }

                        public Corner next() {
                            if (remaining == 0) {
                                throw new NoSuchElementException();
                            }
                            remaining--;
                            try {
                                return values()[current];
                            } finally {
                                current = (current + dir) & 3;
                            }
                        }

                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
            };
        }
    }

    private abstract static class RenderedElement {

        protected final Color color;
        protected final double[] bounds;

        private int boundsWrite = 0;

        protected RenderedElement(Color color) {
            this(color, 0);
        }

        protected RenderedElement(Color color, int boundaryPoints) {
            this.color = color;
            this.bounds = new double[boundaryPoints * 2]; // (x + y) * n
        }

        public final void render(Graphics2D g2d, PointXForm xf) {
            g2d.setColor(this.color);
            renderElement(g2d, xf);
            renderLabel(g2d, g2d.getFontMetrics(), xf);
        }

        protected void renderLabel(Graphics2D g2d, FontMetrics fontMetrics, PointXForm xf) {
        }

        protected abstract void renderElement(Graphics2D g2d, PointXForm xf);

        protected double[] getBounds() {
            return bounds;
        }

        protected void rectangularBounds(double x1, double y1, double x2, double y2, Corner start) {
            if (boundsWrite >= bounds.length) {
                return; // already full
            }
            Series<Corner> corners = start.goCW();
            if (boundsWrite + corners.size() * 2 > bounds.length) {
                throw new ArrayIndexOutOfBoundsException("More Corners in Series than will fit in bounds[] array");
            }
            if (x1 > x2) {
                double x3 = x1;
                x1 = x2;
                x2 = x3;
            }
            if (y1 > y2) {
                double y3 = y1;
                y1 = y2;
                y2 = y3;
            }
            for (Corner corner : corners) {
                bounds[boundsWrite++] = corner.getX(x1, x2);
                bounds[boundsWrite++] = corner.getY(y1, y2);
            }
        }
    }

    private static class RenderedLabel extends RenderedElement {

        private final double x;
        private final double y;
        private final String label;
        private final int a;
        private final int b;

        private RenderedLabel(double x, double y, String label, Color color) {
            this(x, y, label, 0, 0, color, 0);
        }

        private RenderedLabel(double x, double y, String label, int a, int b, Color color, int extraPoints) {
            super(color, 4 + extraPoints);
            this.x = x;
            this.y = y;
            this.label = label;
            this.a = a; // -1 left justified, 0 centered, 1 right justified
            this.b = b; // -1 top aligned, 0 centered, 1 bottom aligned
        }

        @Override
        protected void renderElement(Graphics2D g2d, PointXForm xf) {
            if (label == null || label.length() == 0) {
                return;
            }
            String[] lines = label.split("\n");
            FontMetrics fontMetrics = g2d.getFontMetrics();
            int lineHeight = fontMetrics.getHeight();
            int height = lineHeight * lines.length;
            float x = xf.processX(this.x);
            float y = xf.processY(this.y);
            int a = Math.min(Math.max(this.a, -1), 1);
            int b = Math.min(Math.max(this.b, -1), 1);
            int i = this.a - a;
            int j = this.b - b;
            float startX = x, startY = y;
            int maxWidth = 0;
            int maxHeight = 0;
            for (int k = 0; k < lines.length; k++) {
                String line = lines[k];
                int width = fontMetrics.stringWidth(line);
                if (k == 0) {
                    startX = x - (width / 2) * (a + 1) - i;
                    startY = y - (height / 2) * (b + 1) - j;
                }
                g2d.drawString(line,
                               x - (width / 2) * (a + 1) - i,
                               y - (height / 2) * (b + 1) - j + k * lineHeight + fontMetrics.getMaxAscent());
                maxHeight += lineHeight;
                maxWidth = Math.max(maxWidth, width);
            }
            rectangularBounds(startX, startY, startX + maxWidth, startY + maxHeight, Corner.UPPER_RIGHT);
        }
    }

    private static class RenderedPoint extends RenderedLabel {

        private final double x;
        private final double y;

        private RenderedPoint(double x, double y, Color color, String label) {
            super(x, y, label, 0, -5, color, 4);
            this.x = x;
            this.y = y;
        }

        protected void renderElement(Graphics2D g2d, PointXForm xf) {
            super.renderElement(g2d, xf);
            int x = xf.processX(this.x);
            int y = xf.processY(this.y);
            g2d.fillOval(x - 2, y - 2, 4, 4);
            rectangularBounds(x - 2, y - 2, x + 2, y + 2, Corner.LOWER_LEFT);
        }
    }

    private static class RenderedPixels extends RenderedElement {

        private final double[] points;

        private RenderedPixels(double[] points, Color color) {
            super(color);
            this.points = points;
        }

        protected void renderElement(Graphics2D g2d, PointXForm xf) {
            for (int i = 0; i < points.length; i += 2) {
                int x = xf.processX(points[i]);
                int y = xf.processY(points[i + 1]);
                g2d.fillOval(x, y, 1, 1);
            }
        }
    }

    private static class RenderedLine extends RenderedElement {

        protected final double[] points;

        private RenderedLine(Color color, double... points) {
            super(color);
            this.points = points;
        }

        protected void renderElement(Graphics2D g2d, PointXForm xf) {
            int x = xf.processX(points[0]);
            int y = xf.processY(points[1]);
            for (int i = 2; i < points.length; i += 2) {
                int a = xf.processX(points[i]);
                int b = xf.processY(points[i + 1]);
                g2d.drawLine(x, y, a, b);
                x = a;
                y = b;
            }
        }
    }

    private static class RenderedCircle extends RenderedLabel {

        private final double x;
        private final double y;
        private final int r;

        private RenderedCircle(Color color, String label, double x, double y, int r) {
            super(x, y, label, 0, -5, color, 4);
            this.x = x;
            this.y = y;
            this.r = r;
        }

        protected void renderElement(Graphics2D g2d, PointXForm xf) {
            int x = xf.processX(this.x);
            int y = xf.processY(this.y);
            g2d.drawOval(x - r, y - r, r + r, r + r);
            rectangularBounds(x - r, y - r, x + r + r, y + r + r, Corner.LOWER_LEFT);
        }
    }

    private static class RenderedPoly extends RenderedElement {

        protected final double[] points;

        private RenderedPoly(Color color, double... points) {
            super(color, points.length >> 1);
            this.points = points;
        }

        protected void renderElement(Graphics2D g2d, PointXForm xf) {
            int l = points.length / 2;
            int[] x = new int[l];
            int[] y = new int[l];
            for (int i = 0, j = 0; i < l; i++) {
                bounds[j++] = x[i] = xf.processX(points[i * 2]);
                bounds[j++] = y[i] = xf.processY(points[i * 2 + 1]);
            }
            g2d.fillPolygon(x, y, l);
        }
    }

    private static class RenderedArrow extends RenderedLine {

        private RenderedArrow(Color color, double... points) {
            super(color, points);
        }

        @Override
        protected void renderElement(Graphics2D g2d, PointXForm xf) {
            super.renderElement(g2d, xf);
            int l = points.length;
            int a = xf.processX(points[l - 2]);
            int b = xf.processY(points[l - 1]);
            int i = xf.processX(points[l - 4]) - a;
            int j = xf.processY(points[l - 3]) - b;
            Vector v = new Vector(i, j, 0.0).normalize(7.0);
            Vector w = new Vector(j, -i, 0.0).normalize(3.0);
            g2d.drawLine(a, b, (int)Math.round(a + v.i - w.i), (int)Math.round(b + v.j - w.j));
            g2d.drawLine(a, b, (int)Math.round(a + v.i + w.i), (int)Math.round(b + v.j + w.j));
        }
    }

    private static class RenderedImage extends RenderedElement {

        private final double x;
        private final double y;
        private final Image image;
        private final float scale;
        private final String label;
        private final int a;
        private final int b;
        private final int height;
        private final int width;

        private RenderedImage(double x, double y, BufferedImage image, float scale, String label, Color color) {
            this(x, y, image, scale, label, 0, 0, color);
        }

        private RenderedImage(double x, double y, BufferedImage image, float scale, String label, int a, int b, Color color) {
            super(color, 4);
            this.x = x;
            this.y = y;
            this.image = image;
            this.scale = scale;
            this.label = label;
            this.a = a; // -1 left justified, 0 centered, 1 right justified
            this.b = b; // -1 top aligned, 0 centered, 1 bottom aligned
            this.height = image.getHeight();
            this.width = image.getWidth();
        }

        @Override
        protected void renderElement(Graphics2D g2d, PointXForm xf) {
            float x1 = xf.processX(this.x);
            float x2 = xf.processX(this.x + width * scale);
            float y2 = xf.processY(this.y);
            float y1 = xf.processY(this.y + height * scale);
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawRect(Math.round(x1) - 1, Math.round(y1) - 1, Math.round(x2 - x1) + 1, Math.round(y2 - y1) + 1);
            g2d.drawImage(image, Math.round(x1), Math.round(y1), Math.round(x2 - x1), Math.round(y2 - y1), null);
            rectangularBounds(Math.round(x1), Math.round(y1), Math.round(x2 - x1), Math.round(y2 - y1), Corner.UPPER_LEFT);

            if (label == null || label.length() == 0) {
                return;
            }
            String[] lines = label.split("\n");
            FontMetrics fontMetrics = g2d.getFontMetrics();
            int lineHeight = fontMetrics.getHeight();
            int height = lineHeight * lines.length;
            int a = Math.min(Math.max(this.a, -1), 1);
            int b = Math.min(Math.max(this.b, -1), 1);
            int x0 = Math.round(a * (x2 - x1) / 2.0f + x1);
            int y0 = Math.round(b * (y2 - y1) / 2.0f + y1);
            int i = this.a - a;
            int j = this.b - b;
            for (int k = 0; k < lines.length; k++) {
                String line = lines[k];
                int width = fontMetrics.stringWidth(line);
                g2d.drawString(line,
                               x0 - (width / 2) * (a + 1) - i,
                               y0 - (height / 2) * (b + 1) - j + k * lineHeight + fontMetrics.getMaxAscent());
            }
        }
    }

    private static class RenderedMouseOver extends RenderedElement {

        private final double[] region;
        private final boolean applyTransform;
        private final OutputTextBuffer text;

        private Shape shape;

        private RenderedMouseOver(double[] region, boolean applyTransform, OutputTextBuffer text) {
            super(Color.BLACK);
            this.applyTransform = applyTransform;
            assert region.length > 0 && region.length % 8 == 0 : "Expected region to define a fixed number of quads";
            this.region = region;
            this.text = text;
        }

        @Override
        protected void renderElement(Graphics2D g2d, PointXForm xf) {
            if (shape == null) {
                Area area = new Area();
                this.shape = area;
                int x[] = new int[4], y[] = new int[4];
                for (int i = 0, l = region.length; i < l;) {
                    if (applyTransform) {
                        x[0] = xf.processX(region[i++]);
                        y[0] = xf.processY(region[i++]);
                        x[1] = xf.processX(region[i++]);
                        y[1] = xf.processY(region[i++]);
                        x[2] = xf.processX(region[i++]);
                        y[2] = xf.processY(region[i++]);
                        x[3] = xf.processX(region[i++]);
                        y[3] = xf.processY(region[i++]);
                    } else {
                        x[0] = (int)(region[i++]);
                        y[0] = (int)(region[i++]);
                        x[1] = (int)(region[i++]);
                        y[1] = (int)(region[i++]);
                        x[2] = (int)(region[i++]);
                        y[2] = (int)(region[i++]);
                        x[3] = (int)(region[i++]);
                        y[3] = (int)(region[i++]);
                    }
//                    System.out.printf("(%d,%d)-(%d,%d)-(%d,%d)-(%d,%d)\n", x[0], y[0], x[1], y[1], x[2], y[2], x[3], y[3]);
                    area.add(new Area(new Polygon(x, y, 4)));
                }
            }
            if (MOUSE_OVER_HIGHLIGHT_COLOR != null) {
                Color color = MOUSE_OVER_HIGHLIGHT_COLOR.get();
                g2d.setColor(color);
                g2d.draw(this.shape);
            }
        }
    }

    private static class Popover extends JFrame {

        public static final String RENDERED_SIZE = "rendered_size";

        private OutputTextBuffer text = new OutputTextBuffer().close();
        private Dimension renderedSize = new Dimension(8,8);

        private Popover() throws HeadlessException {
            setSize(renderedSize);
            setFocusable(false);
            setFocusableWindowState(false);
            setAlwaysOnTop(true);
            setResizable(false);
            setUndecorated(true);
        }

        private void setText(OutputTextBuffer text) {
            this.text = text;
        }

        @Override
        public void paint(Graphics g) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
            Rectangle rectangle = text.paint((Graphics2D)g, 4, 4);
            Dimension size = rectangle == null ? new Dimension(8,8) : rectangle.getSize();
            size = new Dimension(size.width + 8, size.height + 8);
            if (!renderedSize.equals(size)) {
                firePropertyChange(RENDERED_SIZE, renderedSize, size);
                renderedSize = size;
            }
        }
    }

    public static void main(String[] args) {
        OutputGraph out = new OutputGraph("test",1024,  0.0, 0.0, 100.0);
        out.onClose(new Runnable() {
            public void run() {
                System.exit(0);
            }
        });
        out.addPoly(new Color(0.1f, 0.1f, 0.1f), 0.0, -1.0, 1.0, 0.0, 0.0, 1.0, -1.0, 0.0, 0.0, -1.0);
        out.addPoint(null, "Point A", -1.0, -1.0);
        out.addPoint(null, "Point B", 1.0, -1.0);
        out.addPoint(null, "Point C", -1.0, 1.0);
        out.addPoint(null, "Point D", 1.0, 1.0);
        out.addCircle(Color.GREEN, null, 1.0, 1.0, 4);
        out.addPoly(Color.gray, 0.0, -1.0, 1.0, 0.0, 0.0, 1.0, -1.0, 0.0, 0.0, -1.0);
        out.addLine(Color.black, -0.5, 0.0, 0.5, 0.0);
        out.addArrow(Color.black, 0.0, -0.5, 0.0, 0.5);
        OutputTextBuffer textBuffer = new OutputTextBuffer();
        textBuffer.setColor(Color.green).append("Test MouseOver:\n");
        textBuffer.setColor(Color.white).append("(");
        textBuffer.setColor(Color.yellow).append("x");
        textBuffer.setColor(Color.white).append(",");
        textBuffer.setColor(Color.yellow).append("y");
        textBuffer.setColor(Color.white).append(")\n");
        textBuffer.setColor(Color.gray).append("another line");
        Region region = new Region();
        region.addPoint(-1.1, -1.1).addPoint(-0.9, -1.1).addPoint(-0.9, -0.9).addPoint(-1.1, -0.9);
        out.addMouseOver(textBuffer, region);
    }
}
