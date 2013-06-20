package net.venaglia.gloo.util.debug;

import net.venaglia.gloo.physical.geom.Vector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: ed
 * Date: 12/25/12
 * Time: 2:25 PM
 */
public class OutputGraph {

    private final double scale;

    private JFrame window;
    private List<RenderedElement> elements = Collections.synchronizedList(new ArrayList<RenderedElement>());
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

    public void addLine(Color color, double... points) {
        if (points.length < 4 || points.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        elements.add(new RenderedLine(color == null ? Color.white : color, points));
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

    public void clear() {
        elements.clear();
        window.repaint();
    }

    private static interface PointXForm {

        int processX(double x);

        int processY(double y);
    }

    private abstract static class RenderedElement {
        protected final Color color;

        protected RenderedElement(Color color) {
            this.color = color;
        }

        public final void render(Graphics2D g2d, PointXForm xf) {
            g2d.setColor(this.color);
            renderElement(g2d, xf);
            renderLabel(g2d, g2d.getFontMetrics(), xf);
        }

        protected void renderLabel(Graphics2D g2d, FontMetrics fontMetrics, PointXForm xf) {
        }

        protected abstract void renderElement(Graphics2D g2d, PointXForm xf);
    }

    private static class RenderedLabel extends RenderedElement {

        private final double x;
        private final double y;
        private final String label;
        private final int a;
        private final int b;

        private RenderedLabel(double x, double y, String label, Color color) {
            this(x, y, label, 0, 0, color);
        }

        private RenderedLabel(double x, double y, String label, int a, int b, Color color) {
            super(color);
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
            for (int k = 0; k < lines.length; k++) {
                String line = lines[k];
                int width = fontMetrics.stringWidth(line);
                g2d.drawString(line,
                               x - (width / 2) * (a + 1) - i,
                               y - (height / 2) * (b + 1) - j + k * lineHeight + fontMetrics.getMaxAscent());
            }
        }
    }

    private static class RenderedPoint extends RenderedLabel {

        private final double x;
        private final double y;

        private RenderedPoint(double x, double y, Color color, String label) {
            super(x, y, label, 0, -5, color);
            this.x = x;
            this.y = y;
        }

        protected void renderElement(Graphics2D g2d, PointXForm xf) {
            super.renderElement(g2d, xf);
            int x = xf.processX(this.x);
            int y = xf.processY(this.y);
            g2d.fillOval(x - 2, y - 2, 4, 4);
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

    private static class RenderedPoly extends RenderedElement {

        protected final double[] points;

        private RenderedPoly(Color color, double... points) {
            super(color);
            this.points = points;
        }

        protected void renderElement(Graphics2D g2d, PointXForm xf) {
            int l = points.length / 2;
            int[] x = new int[l];
            int[] y = new int[l];
            for (int i = 0; i < l; i++) {
                x[i] = xf.processX(points[i * 2]);
                y[i] = xf.processY(points[i * 2 + 1]);
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
            super(color);
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

    public static void main(String[] args) {
        OutputGraph out = new OutputGraph("test",1024,  0.0, 0.0, 100.0);
        out.onClose(new Runnable() {
            public void run() {
                System.exit(0);
            }
        });
        out.addPoint(null, "Point A", -1.0, -1.0);
        out.addPoint(null, "Point B", 1.0, -1.0);
        out.addPoint(null, "Point C", -1.0, 1.0);
        out.addPoint(null, "Point D", 1.0, 1.0);
        out.addPoly(Color.gray, 0.0, -1.0, 1.0, 0.0, 0.0, 1.0, -1.0, 0.0, 0.0, -1.0);
        out.addLine(Color.black, -0.5, 0.0, 0.5, 0.0);
        out.addArrow(Color.black, 0.0, -0.5, 0.0, 0.5);
    }
}
