package net.venaglia.common.util.serializer;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: ed
 * Date: 9/17/14
 * Time: 7:53 AM
 */
public class SerializerDebugger {

    private static final ThreadLocal<SerializerDebugger> ACTIVE_DEBUGGER = new ThreadLocal<SerializerDebugger>();
    private static final Marker DUMMY_MARKER = new Marker() {
        public void close() { }
    };

    enum State {
        READY, CAPTURING, DONE
    }

    private State state = State.READY;
    private int depth = 0;
    private ByteBuffer buffer;
    private Node top;
    private Node active;

    private Marker startMethod(String method) {
        if (state == State.DONE) {
            throw new IllegalStateException("Debugger has data from a previous serialize operation. Call reset() before calling " + method + "()");
        }
        if (depth++ == 0) {
            active = new Node("<root>");
            state = State.CAPTURING;
            return active;
        }
        return null;
    }

    private void stopMethod(String method, Marker marker) {
        if (state != State.CAPTURING) {
            throw new IllegalStateException("Cannot stop a capture in " + method + "() after the debugger has already been stopped.");
        }
        if (marker != null) {
            marker.close();
        }
        if (--depth == 0) {
            state = State.DONE;
        }
    }

    public synchronized <T> void serialize(SerializerStrategy<T> strategy, T value, ByteBuffer out) {
        SerializerDebugger debugger = ACTIVE_DEBUGGER.get();
        buffer = out;
        Marker marker = startMethod("serialize");
        try {
            ACTIVE_DEBUGGER.set(this);
            strategy.serialize(value, out);
        } finally {
            ACTIVE_DEBUGGER.set(debugger);
            stopMethod("serialize", marker);
            buffer = null;
        }
    }

    public synchronized <T> T deserialize(SerializerStrategy<T> strategy, ByteBuffer in) {
        SerializerDebugger debugger = ACTIVE_DEBUGGER.get();
        buffer = in;
        Marker marker = startMethod("deserialize");
        try {
            ACTIVE_DEBUGGER.set(this);
            return strategy.deserialize(in);
        } finally {
            ACTIVE_DEBUGGER.set(debugger);
            stopMethod("deserialize", marker);
            buffer = null;
        }
    }

    public void writeReport(PrintStream out) {
        writeReport(new PrintWriter(out, true));
    }

    public synchronized void writeReport(PrintWriter out) {
        if (state != State.DONE) {
            throw new IllegalStateException("Cannot generate a report of nothing. You must call serialize() or deserialize() first.");
        }
        int places = String.valueOf(top.end - top.begin).length();
        String addrFormat = "%0" + places + "x";
        writeImpl("", top, addrFormat, out);
        out.println();
        out.println("<end>");
    }

    private void writeImpl(String indent, Node node, String addrFormat, PrintWriter out) {
        out.print(indent);
        out.print(node.label);
        List<Object> parts = node.parts();
        int pointer = node.begin;
        String childIndent = indent + "    ";
        for (Object part : parts) {
            out.println();
            if (part instanceof byte[]) {
                byte[] data = (byte[])part;
                out.print(childIndent);
                for (int i = 0, l = data.length; i < l; i += 16) {
                    if (i > 0) {
                        out.println();
                        out.print(childIndent);
                    }
                    out.printf(addrFormat, pointer);
                    out.print(" : ");
                    printHex(data, i, 8, out);
                    out.print("- ");
                    printHex(data, i + 8, 8, out);
                    out.print(": ");
                    printChars(data, i, 16, out);
                    pointer += 16;
                }
            } else if (part instanceof Node) {
                Node n = (Node)part;
                writeImpl(childIndent, n, addrFormat, out);
                pointer += n.end - n.begin;
            }
        }
    }

    private void printHex(byte[] bytes, int offset, int length, PrintWriter out) {
        for (int i = 0, j = offset; i < length; i++) {
            if (j < bytes.length) {
                int v = bytes[j++];
                out.print("0123456789abcdef".charAt((v >> 4) & 0xF));
                out.print("0123456789abcdef".charAt(v & 0xF));
                out.print(' ');
            } else {
                out.print("   ");
            }
        }
    }

    private void printChars(byte[] bytes, int offset, int length, PrintWriter out) {
        for (int i = 0, j = offset; i < length; i++) {
            if (j < bytes.length) {
                int v = bytes[j++];
                if (v >= ' ' && v <= '~' || Character.isLetterOrDigit(v)) {
                    out.print((char)v);
                } else {
                    out.print('.');
                }
            } else {
                out.print(' ');
            }
        }
    }

    Node mark(String label) {
        return new Node(label);
    }

    public synchronized void reset() {
        state = State.READY;
        depth = 0;
        top = null;
        active = null;
    }

    class Node implements Marker {
        public final String label;
        public final ByteBuffer buffer;
        public int begin;
        public int end = -1;
        public Node parent;
        public List<Node> children;

        Node(String label) {
            if (label == null) {
                throw new NullPointerException("label");
            }
            this.label = label;
            this.parent = SerializerDebugger.this.active;
            SerializerDebugger.this.active = this;
            if (this.parent == null) {
                SerializerDebugger.this.top = this;
            } else {
                if (this.parent.children == null) {
                    this.parent.children = new ArrayList<Node>(4);
                }
                this.parent.children.add(this);
            }
            this.buffer = SerializerDebugger.this.buffer;
            this.begin = this.buffer.position();
        }

        public void close() {
            if (active != this) {
                throw new IllegalStateException("Cannot end a node that is not the active node: " + stack());
            }
            active = this.parent;
            this.end = buffer.position();
        }

        List<Object> parts() {
            if (children == null || children.isEmpty()) {
                return begin == end ?
                       Collections.emptyList() :
                       Collections.<Object>singletonList(getDataBlock(begin, end));
            } else {
                List<Object> result = new ArrayList<Object>(children.size() + 2);
                int from = this.begin;
                for (Node child : children) {
                    if (from < child.begin) {
                        result.add(getDataBlock(from, child.begin));
                    }
                    result.add(child);
                    from = child.end;
                }
                if (from < end) {
                    result.add(getDataBlock(from, end));
                }
                return result;
            }
        }

        private byte[] getDataBlock(int begin, int end) {
            byte[] slice = new byte[end - begin];
            int position = buffer.position();
            try {
                buffer.position(begin);
                buffer.get(slice);
                return slice;
            } finally {
                buffer.position(position);
            }
        }

        private String stack() {
            return parent == null ? label : parent.stack() + "." + label;
        }
    }

    static Marker start(String label) {
        if (label == null) {
            return DUMMY_MARKER;
        }
        SerializerDebugger debugger = ACTIVE_DEBUGGER.get();
        if (debugger != null) {
            return debugger.mark(label);
        } else {
            return DUMMY_MARKER;
        }
    }

    interface Marker {
        void close();
    }
}
