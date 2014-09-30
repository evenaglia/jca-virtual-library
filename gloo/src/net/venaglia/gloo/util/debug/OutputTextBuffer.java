package net.venaglia.gloo.util.debug;

import java.awt.*;

/**
 * User: ed
 * Date: 8/18/14
 * Time: 7:45 AM
 */
public class OutputTextBuffer implements Appendable {

    private Color color = Color.LIGHT_GRAY;
    private StringBuilder buffer = new StringBuilder();
    private OutputFragment head, tail;

    public OutputTextBuffer append(boolean b) {
        ensureOpen();
        buffer.append(b);
        return this;
    }

    public OutputTextBuffer append(byte b) {
        ensureOpen();
        buffer.append(b);
        return this;
    }

    public OutputTextBuffer append(short s) {
        ensureOpen();
        buffer.append(s);
        return this;
    }

    public OutputTextBuffer append(int i) {
        ensureOpen();
        buffer.append(i);
        return this;
    }

    public OutputTextBuffer append(long l) {
        ensureOpen();
        buffer.append(l);
        return this;
    }

    public OutputTextBuffer append(float f) {
        ensureOpen();
        buffer.append(f);
        return this;
    }

    public OutputTextBuffer append(double d) {
        ensureOpen();
        buffer.append(d);
        return this;
    }

    public OutputTextBuffer append(char c) {
        ensureOpen();
        buffer.append(c);
        return this;
    }

    public OutputTextBuffer append(char[] c) {
        ensureOpen();
        buffer.append(c);
        return this;
    }

    public OutputTextBuffer append(char[] c, int off, int len) {
        ensureOpen();
        buffer.append(c, off, len);
        return this;
    }

    public OutputTextBuffer append(CharSequence s) {
        ensureOpen();
        buffer.append(s);
        return this;
    }

    public OutputTextBuffer append(CharSequence s, int off, int len) {
        ensureOpen();
        buffer.append(s, off, len);
        return this;
    }

    public OutputTextBuffer append(String s) {
        ensureOpen();
        buffer.append(s);
        return this;
    }

    public OutputTextBuffer append(String format, Object params) {
        ensureOpen();
        buffer.append(String.format(format, params));
        return this;
    }

    public OutputTextBuffer append(Object o) {
        ensureOpen();
        buffer.append(o);
        return this;
    }

    public OutputTextBuffer setColor(Color color) {
        ensureOpen();
        if (color == null) {
            throw new NullPointerException("color");
        }
        flush();
        this.color = color;
        return this;
    }

    OutputTextBuffer close() {
        if (buffer != null) {
            flush();
            buffer = null;
            if (head != null) {
                for (OutputFragment frag = head.next, last = head; frag != null; last = frag, frag = frag.next) {
                    OutputFragment next = frag.next;
                    if (next != null && !next.lineBreakBefore && frag.text.length() == 0) {
                        OutputFragment replace = new OutputFragment(next.color, next.text, frag.lineBreakBefore);
                        next.lineBreakBefore = frag.lineBreakBefore;
                        replace.next = next.next;
                        last.next = next;
                        frag = next;
                    }
                }
            }
        }
        return this;
    }

    /**
     * Renders this buffer to the passed Graphics2D, returning the width/height extents drawn
     * @param graphics The output buffer on which to draw
     * @return The dimensions of the region drawn to, or null if noting was drawn
     */
    Rectangle paint(Graphics2D graphics, int x, int y) {
        if (head == null || head == tail && head.text.length() == 0) {
            return null;
        }
        graphics.setFont(Font.decode("Droid Sans Mono-10"));
        Color saveColor = graphics.getColor();
        OutputFragment fragment = head;
        FontMetrics metrics = graphics.getFontMetrics();
        int height = metrics.getHeight();
        int i = x, j = y + metrics.getAscent();
        int minX = x, minY = y;
        int maxX = x, maxY = y + height;
        while (fragment != null) {
            minX = Math.min(minX, i);
            if (fragment.lineBreakBefore) {
                j += height;
                i = x;
                maxY += height;
            }
            if (fragment.text.length() > 0) {
                graphics.setColor(fragment.color);
                graphics.drawString(fragment.text, i, j);
                i += metrics.stringWidth(fragment.text);
                maxX = Math.max(maxX, i);
            }
            fragment = fragment.next;
        }
        graphics.setColor(saveColor);
        maxY += metrics.getDescent();
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    private void ensureOpen() {
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer is closed");
        }
    }

    private void flush() {
        String text = buffer.toString();
        buffer.setLength(0);
        int lastEOL = -1;
        int nextEOL = text.indexOf('\n');
        while (nextEOL >= 0) {
            push(new OutputFragment(color, text.substring(lastEOL + 1, nextEOL), lastEOL >= 0));
            lastEOL = nextEOL;
            nextEOL = text.indexOf('\n', lastEOL + 1);
        }
        push(new OutputFragment(color, text.substring(lastEOL + 1), lastEOL >= 0));
    }

    private void push(OutputFragment fragment) {
        if (head == null && tail == null && fragment.text.length() == 0) {
            return;
        }
        if (tail != null) {
            tail.next = fragment;
        } else {
            head = fragment;
        }
        tail = fragment;
    }

    private static class OutputFragment {

        private final Color color;
        private final String text;

        private boolean lineBreakBefore;
        private OutputFragment next;

        private OutputFragment(Color color, String text, boolean lineBreakBefore) {
            if (color == null) throw new NullPointerException("color");
            if (text == null) throw new NullPointerException("text");
            this.color = color;
            this.text = text;
            this.lineBreakBefore = lineBreakBefore;
        }
    }
}
