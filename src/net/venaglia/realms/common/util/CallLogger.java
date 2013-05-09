package net.venaglia.realms.common.util;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ed
 * Date: 3/21/13
 * Time: 3:48 PM
 */
@SuppressWarnings({ "ConstantConditions", "PointlessBooleanExpression" })
public class CallLogger {

    public static final boolean logCalls = false;
    public static final boolean logLines = true;

    private static final Map<Integer,String> GL_CONSTANTS;
    private static final Map<Integer,String> GL_BITS;

    private static boolean insideVertexSequence = false;
    private static boolean insideVertexSequenceAfter = false;

    static {
        Map<Integer,String> glConstants = new HashMap<Integer,String>();
        Map<Integer,String> glBits = new HashMap<Integer,String>();
        Class[] glClasses = { GL11.class, GL13.class, GL20.class };
        for (Class glClass : glClasses) {
            for (Field field : glClass.getFields())
                if (Modifier.isPublic(field.getModifiers()) &&
                    Modifier.isStatic(field.getModifiers()) &&
                    Modifier.isFinal(field.getModifiers()) &&
                    Integer.TYPE.equals(field.getType())) {
                    Integer value;
                    try {
                        value = (Integer)field.get(null);
                    } catch (IllegalAccessException e) {
                        // shouldn't happen, but skip it if it does
                        continue;
                    }
                    glConstants.put(value, field.getName());
                    if (field.getName().endsWith("_BIT")) {
                        glBits.put(value, field.getName());
                    }
                }
        }
        glConstants.remove(0); // do not map this one
        GL_CONSTANTS = Collections.unmodifiableMap(glConstants);
        GL_BITS = Collections.unmodifiableMap(glBits);
    }

    public static void logCall(String method, Object... args) {
        logCallImpl(method, args, null, whereAmI(1, false), getError(method));
    }

    public static void logCall(boolean result, String method, Object... args) {
        logCallImpl(method, args, String.valueOf(result), whereAmI(1, false), getError(method));
    }

    public static void logCall(int result, String method, Object... args) {
        logCallImpl(method, args, String.valueOf(result), whereAmI(1, false), getError(method));
    }

    public static void logCall(double result, String method, Object... args) {
        logCallImpl(method, args, String.format("%.4f", result), whereAmI(1, false), getError(method));
    }

    public static void logCall(IntBuffer result, String method, Object... args) {
        logCallImpl(method, args, BufferFormatter.INT.toString(result), whereAmI(1, false), getError(method));
    }

    public static void logCall(FloatBuffer result, String method, Object... args) {
        logCallImpl(method, args, BufferFormatter.FLOAT.toString(result), whereAmI(1, false), getError(method));
    }

    public static void logCall(DoubleBuffer result, String method, Object... args) {
        logCallImpl(method, args, BufferFormatter.DOUBLE.toString(result), whereAmI(1, false), getError(method));
    }

    public static Object orBits(int... flags) {
        return new OrBits(flags);
    }

    public static Object buffer(Buffer buffer) {
        if (buffer instanceof IntBuffer) {
            return BufferFormatter.INT.wrapForToString((IntBuffer)buffer);
        } else if (buffer instanceof FloatBuffer) {
            return BufferFormatter.FLOAT.wrapForToString((FloatBuffer)buffer);
        } else if (buffer instanceof ByteBuffer) {
            return BufferFormatter.BYTE.wrapForToString((ByteBuffer)buffer);
        } else if (buffer instanceof DoubleBuffer) {
            return BufferFormatter.DOUBLE.wrapForToString((DoubleBuffer)buffer);
        } else if (buffer instanceof LongBuffer) {
            return BufferFormatter.LONG.wrapForToString((LongBuffer)buffer);
        } else if (buffer instanceof ShortBuffer) {
            return BufferFormatter.SHORT.wrapForToString((ShortBuffer)buffer);
        } else if (buffer instanceof CharBuffer) {
            return BufferFormatter.CHAR.wrapForToString((CharBuffer)buffer);
        }
        return buffer;
    }

    private static int getError(String method) {
        if ("glBegin".equals(method)) {
            insideVertexSequenceAfter = true;
        } else if ("glEnd".equals(method)) {
            insideVertexSequence = false;
        }
        return insideVertexSequence ? 0 : GL11.glGetError();
    }

    public static void logMessage(String msg) {
        if (insideVertexSequence) {
            System.out.print("  ");
        }
        System.out.println(msg);
        if (insideVertexSequenceAfter) {
            insideVertexSequence = true;
            insideVertexSequenceAfter = false;
        }
    }

    private static final String[] formats = new String[]{
            "%1$s(%2$s)", // method + args
            "%1$s(%2$s) = %3$s", // method + args + result
            "%1$s(%2$s) <-- !! %6$s !!", // method + args + error
            "%1$s(%2$s) = %3$s <-- !! %6$s !!", // method + args + result + error
            "%1$s(%2$s) @ [%4$s:%5$d]", // method + args
            "%1$s(%2$s) = %3$s @ [%4$s:%5$d]", // method + args + result
            "%1$s(%2$s) @ [%4$s:%5$d] <-- !! %6$s !!", // method + args + error
            "%1$s(%2$s) = %3$s @ [%4$s:%5$d] <-- !! %6$s !!", // method + args + result + error
    };

    private static void logCallImpl(String method, Object[] args, String result, StackTraceElement where, int err) {
//        if (!logCalls) {
//            return;
//        }
        if (where == null && err != 0) {
            where = whereAmI(2, true);
        }
        int format = (result == null ? 0 : 1) + (err == 0 ? 0 : 2) + (where == null ? 0 : 4);
        Object[] params = {
                method,
                callSignature(args),
                result,
                where == null ? null : where.getFileName(),
                where == null ? null : where.getLineNumber(),
                err == 0 ? null : GL_CONSTANTS.get(err)
        };
        logMessage(String.format(formats[format], params));
    }

    private static StackTraceElement whereAmI(int depth, boolean force) {
        if (logCalls && logLines || force) {
            return new Exception().getStackTrace()[depth + 1];
        } else {
            return null;
        }
    }

    private static CharSequence callSignature(Object[] args) {
        if (args.length == 0) {
            return "";
        }
        StringBuilder buffer = new StringBuilder();
        boolean first = true;
        for (Object arg : args) {
            if (arg instanceof CharSequence) {
                arg = escapeString(arg.toString());
            } else if (arg instanceof Integer) {
                if (GL_CONSTANTS.containsKey(arg)) {
                    arg = GL_CONSTANTS.get(arg);
                }
            } else if (arg instanceof Number) {
                Number number = (Number)arg;
                if (number instanceof Float || number instanceof Double || number.doubleValue() != number.longValue()) {
                    arg = String.format("%.4f", number.doubleValue());
                }
            } else if (arg instanceof Buffer) {
                arg = arg.getClass().getSimpleName() + "[" + ((Buffer)arg).limit() + "]";
            }
            if (first) {
                first = false;
            } else {
                buffer.append(",");
            }
            buffer.append(arg);
        }
        return buffer;
    }

    private static String escapeString(String s) {
        StringBuilder buffer = new StringBuilder(Math.round(s.length() * 1.1f) + 2);
        buffer.append("\"");
        for (int i = 0, l = s.length(); i < l; i++) {
            char c = s.charAt(i);
            switch (c) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    buffer.append("\\").append((int)c);
                    break;
                case '\n':
                    buffer.append("\\n");
                    break;
                case '\t':
                    buffer.append("\\t");
                    break;
                case '\f':
                    buffer.append("\\f");
                    break;
                case '\\':
                    buffer.append("\\\\");
                    break;
                case '\"':
                    buffer.append("\\\"");
                    break;
                default:
                    if (c < 32 || c > 127) {
                        buffer.append("\\u").append(String.format("%04x", (int)c));
                    } else {
                        buffer.append(c);
                    }
            }
            if (buffer.length() > 60) {
                buffer.append("...");
                break;
            }
        }
        buffer.append("\"");
        return buffer.toString();
    }

    private static class OrBits {
        private final int[] flags;

        private OrBits(int[] flags) {
            this.flags = flags;
        }

        @Override
        public String toString() {
            if (flags.length == 0) {
                return "0";
            }
            StringBuilder buffer = new StringBuilder();
            boolean first = true;
            for (int flag : flags) {
                if (first) first = false;
                else buffer.append(" | ");
                if (GL_BITS.containsKey(flag)) {
                    buffer.append(GL_BITS.get(flag));
                } else if (GL_CONSTANTS.containsKey(flag)) {
                    buffer.append(GL_CONSTANTS.get(flag));
                } else {
                    buffer.append("0x").append(Integer.toHexString(flag));
                }
            }
            return buffer.toString();
        }
    }

    private static class BufferFormat<B extends Buffer> {

        private final BufferFormatter<B> formatter;
        private final B buffer;

        private BufferFormat(BufferFormatter<B> formatter, B buffer) {
            this.formatter = formatter;
            this.buffer = buffer;
        }

        @Override
        public String toString() {
            return formatter.toString(buffer);
        }
    }

    private abstract static class BufferFormatter<B extends Buffer> {

        static BufferFormatter<ByteBuffer> BYTE = new BufferFormatter<ByteBuffer>() {
            @Override
            protected String format(ByteBuffer buffer, int index) {
                return String.format("0x%02x", ((int)buffer.get(index)) & 0xFF);
            }
            protected int getLimit() {
                return 4;
            }
        };

        static BufferFormatter<ShortBuffer> SHORT = new BufferFormatter<ShortBuffer>() {
            protected String format(ShortBuffer buffer, int index) {
                return String.valueOf(buffer.get(index));
            }
            protected int getLimit() {
                return 4;
            }
        };

        static BufferFormatter<IntBuffer> INT = new BufferFormatter<IntBuffer>() {
            protected String format(IntBuffer buffer, int index) {
                return String.valueOf(buffer.get(index));
            }
            protected int getLimit() {
                return 4;
            }
        };

        static BufferFormatter<LongBuffer> LONG = new BufferFormatter<LongBuffer>() {
            protected String format(LongBuffer buffer, int index) {
                return String.valueOf(buffer.get(index));
            }
            protected int getLimit() {
                return 4;
            }
        };

        static BufferFormatter<FloatBuffer> FLOAT = new BufferFormatter<FloatBuffer>() {
            protected String format(FloatBuffer buffer, int index) {
                return String.format("%.4f", buffer.get(index));
            }
        };

        static BufferFormatter<DoubleBuffer> DOUBLE = new BufferFormatter<DoubleBuffer>() {
            protected String format(DoubleBuffer buffer, int index) {
                return String.format("%.4f", buffer.get(index));
            }
        };

        static BufferFormatter<CharBuffer> CHAR = new BufferFormatter<CharBuffer>() {
            protected String format(CharBuffer buffer, int index) {
                return null;
            }
            public String toString(CharBuffer buffer) {
                return escapeString(buffer.toString());
            }
        };

        protected abstract String format(B buffer, int index);

        protected int getLimit() {
            return 2;
        }

        public String toString(B buffer) {
            StringBuilder string = new StringBuilder();
            string.append('[');
            int limit = getLimit();
            for (int i = 0, l = buffer.limit(); i < l; i++) {
                if (i > 0) string.append(',');
                if (i == limit && l > limit * 2) {
                    string.append("...");
                    i = l - limit - 1;
                } else {
                    string.append(format(buffer, i));
                }
            }
            string.append(']');
            return string.toString();

        }

        public String toMatrixString(B buffer, int size) {
            if (buffer.limit() != 16) {
                return toString(buffer);
            }
            StringBuilder string = new StringBuilder();
            string.append('[');
            int limit = getLimit();
            for (int i = 0, l = buffer.limit(); i < l; i++) {
                if (i > 0) string.append(',');
                if (i == limit && l > limit * 2) {
                    string.append("...");
                    i = l - limit - 1;
                } else {
                    string.append(format(buffer, i));
                }
            }
            string.append(']');
            return string.toString();
        }

        public Object wrapForToString(final B buffer) {
            return new Object() {
                @Override
                public String toString() {
                    return BufferFormatter.this.toString(buffer);
                }
            };
        }
    }
}
