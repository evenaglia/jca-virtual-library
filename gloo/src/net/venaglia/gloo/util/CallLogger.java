package net.venaglia.gloo.util;

import net.venaglia.gloo.physical.decorators.Brush;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
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
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: ed
 * Date: 3/21/13
 * Time: 3:48 PM
 */
public class CallLogger {

    public static final boolean logCalls = false;
    public static final boolean logStacks;

    private static final boolean logCallHash;
    private static final Set<Integer> deepStackHashes;

    static {
        Pattern matchTrue = Pattern.compile("1|t(rue)?", Pattern.CASE_INSENSITIVE);
        Pattern matchHash = Pattern.compile("^#?([0-9a-zA-Z]{4})$");
        logCallHash = matchTrue.matcher(System.getProperty("gloo.log.call.hash", "1")).matches();
        Set<Integer> hashesToLog = new HashSet<Integer>();
        for (String h : System.getProperty("gloo.log.call.stack", "").split("\\s+")) {
            Matcher matcher = matchHash.matcher(h);
            if (matcher.find()) {
                hashesToLog.add(Integer.parseInt(matcher.group(1), 16));
            }
        }
        deepStackHashes = Collections.unmodifiableSet(hashesToLog);
        logStacks = !deepStackHashes.isEmpty();
    }

    private static final Map<Integer,String> GL_CONSTANTS;
    private static final Map<Integer,String> GL_BITS;
    private static final MessageDigest SHA1;
    private static final char[] SPACES = new char[64];
    private static final Map<String,LogMatrix> MATRIX_BUFFER = new LinkedHashMap<String,LogMatrix>();
    private static final StringBuilder BUFFER = new StringBuilder(256);

    private static boolean insideListRecorder = false;
    private static boolean insideListRecorderAfter = false;
    private static boolean insideVertexSequence = false;
    private static boolean insideVertexSequenceAfter = false;

    static {
        Map<Integer, String> glConstants = new HashMap<Integer, String>();
        Map<Integer, String> glBits = new HashMap<Integer, String>();
        Class[] glClasses = { GL11.class, GL12.class, GL13.class, GL20.class };
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
        try {
            SHA1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        Arrays.fill(SPACES, ' ');
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

    public static void logCall(LogMatrix result, String method, Object... args) {
        logCallImpl(method, args, result, whereAmI(1, false), getError(method));
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

    public static Object glConstants(int... values) {
        return BufferFormatter.INT_GL_CONSTANT.wrapForToString(IntBuffer.wrap(values));
    }

    public static LogMatrix logMatrix(String name, FloatBuffer buffer, int w, int h) {
        return BufferFormatter.FLOAT_FIXED.wrapForMatrix(name, buffer, w, h);
    }

    public static LogMatrix logMatrix(String name, DoubleBuffer buffer, int w, int h) {
        return BufferFormatter.DOUBLE_FIXED.wrapForMatrix(name, buffer, w, h);
    }

    private static int getError(String method) {
        if ("glBegin".equals(method)) {
            insideVertexSequenceAfter = true;
            return 0; // we're already inside the geometry sequence, don't call glGetError()
        } else if ("glEnd".equals(method)) {
            insideVertexSequence = false;
        } else if ("glNewList".equals(method)) {
            insideListRecorderAfter = true;
        } else if ("glEndList".equals(method)) {
            insideListRecorder = false;
        }
        return insideVertexSequence ? 0 : GL11.glGetError();
    }

    public static void logMessage(String msg, int... tabs) {
        if (insideVertexSequence ^ insideListRecorder) {
            msg = "  " + msg;
        } else if (insideListRecorder & insideVertexSequence) {
            msg = "    " + msg;
        }
        System.out.println(tabs.length == 0 ? msg : formatTabs(msg, tabs));
        if (insideVertexSequenceAfter) {
            insideVertexSequence = true;
            insideVertexSequenceAfter = false;
        }
        if (insideListRecorderAfter) {
            insideListRecorder = true;
            insideListRecorderAfter = false;
        }
    }

    private static Object formatTabs(String msg, int... stops) {
        BUFFER.setLength(0);
        int i = 0;
        for (int j = msg.indexOf('\t'), k = 0; j >= 0; i = j + 1, j = msg.indexOf('\t', i), k++) {
            BUFFER.append(msg.substring(i, j));
            int stop = k < stops.length ? stops[k] : -1;
            if (stop < 0) {
                BUFFER.append('\t');
            } else {
                int spaces = stop - BUFFER.length();
                while (spaces > 0) {
                    BUFFER.append(SPACES, 0, Math.min(SPACES.length, spaces));
                    spaces -= Math.min(SPACES.length, spaces);
                }
            }
        }
        if (i < msg.length()) {
            BUFFER.append(msg.substring(i));
        }
        return BUFFER;
    }

    private static final String[] formats = new String[]{
            "%1$s(%2$s)", // method + args
            "%1$s(%2$s) = %3$s", // method + args + result
            "%1$s(%2$s) \t\t\t<-- !! %6$s !!", // method + args + error
            "%1$s(%2$s) = %3$s \t\t\t<-- !! %6$s !!", // method + args + result + error
            "%1$s(%2$s) \t\t@ [%5$s]", // method + args
            "%1$s(%2$s) = %3$s \t\t@ [%5$s]", // method + args + result
            "%1$s(%2$s) \t\t@ [%5$s] \t<-- !! %6$s !!", // method + args + error
            "%1$s(%2$s) = %3$s \t\t@ [%5$s] \t<-- !! %6$s !!", // method + args + result + error
            "%1$s(%2$s) \t#%4$04X", // method + args + hash
            "%1$s(%2$s) = %3$s \t#%4$04X", // method + args + result + hash
            "%1$s(%2$s) \t#%4$04X \t\t<-- !! %6$s !!", // method + args + error + hash
            "%1$s(%2$s) = %3$s \t#%4$04X \t\t<-- !! %6$s !!", // method + args + result + error + hash
            "%1$s(%2$s) \t#%4$04X \t@ [%5$s]", // method + args + hash
            "%1$s(%2$s) = %3$s \t#%4$04X \t@ [%5$s]", // method + args + result + hash
            "%1$s(%2$s) \t#%4$04X \t@ [%5$s] \t<-- !! %6$s !!", // method + args + error + hash
            "%1$s(%2$s) = %3$s \t#%4$04X \t@ [%5$s] \t<-- !! %6$s !!", // method + args + result + error + hash
    };

    private static void logCallImpl(String method, Object[] args, Object result, StackTrace where, int err) {
//        if (!logCalls) {
//            return;
//        }
        StringBuilder signature = callSignature(args);
        Integer hash = logCallHash ? callHash(method + "(" + signature + ")" + (where == null ? "" : where.hashCode())) : null;
        boolean logDeep = err != 0 || deepStackHashes.contains(hash);
        int format = (result == null ? 0 : 1) +
                     (err == 0 ? 0 : 2) +
                     (logStacks ? 0 : 4) +
                     (logCallHash ? 8 : 0);
        if (result instanceof LogMatrix) {
            LogMatrix logMatrix = (LogMatrix)result;
            String name = logMatrix.name();
            MATRIX_BUFFER.put(name, logMatrix);
            result = "|" + name + "|";
        }
        Object[] params = {
                method,
                signature,
                result,
                hash,
                where,
                err == 0 ? null : GL_CONSTANTS.get(err)
        };
        logMessage(String.format(formats[format], params), 100, 110, 160);
        for (LogMatrix logMatrix : MATRIX_BUFFER.values()) {
            for (String line : logMatrix.toString().split("\n")) {
                logMessage(line, 4, 20);
            }
        }
        if (logDeep) {
            if (where == null) {
                where = whereAmI(2, true);
            }
            where.printFullStack();
        }
    }

    private static StackTrace whereAmI(int depth, boolean force) {
            return force || logStacks ? new StackTrace(depth + 1) : null;
    }

    private static final Map<Brush,String> NAMED_BRUSHES;

    static {
        Map<Brush,String> brushes = new HashMap<Brush,String>();
        for (Field field : Brush.class.getFields()) {
            int mod = field.getModifiers();
            if (Modifier.isStatic(mod) && Modifier.isFinal(mod) && Brush.class.isAssignableFrom(field.getType())) {
                try {
                    Brush brush = (Brush)field.get(null);
                    brushes.put(brush, field.getDeclaringClass().getSimpleName() + "." + field.getName());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        NAMED_BRUSHES = Collections.unmodifiableMap(brushes);
    }

    private static StringBuilder callSignature(Object[] args) {
        BUFFER.setLength(0);
        MATRIX_BUFFER.clear();
        if (args.length == 0) {
            return BUFFER;
        }
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
            } else if (arg instanceof LogMatrix) {
                LogMatrix logMatrix = (LogMatrix)arg;
                arg = "|" + logMatrix.name() + "|";
                MATRIX_BUFFER.put((String)arg, logMatrix);
            } else if (arg instanceof Brush && NAMED_BRUSHES.containsKey(arg)) {
                arg = NAMED_BRUSHES.get(arg);
            }
            if (first) {
                first = false;
            } else {
                BUFFER.append(",");
            }
            BUFFER.append(arg);
        }
        return BUFFER;
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

    private static int callHash(String call) {
        byte[] digest = SHA1.digest(call.getBytes(Charset.forName("UTF-8")));
        return (((int)digest[0]) & 0xFF) << 8 | (((int)digest[1]) & 0xFF);
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

        static BufferFormatter<IntBuffer> INT_GL_CONSTANT = new BufferFormatter<IntBuffer>() {

            protected String format(IntBuffer buffer, int index) {
                int arg = buffer.get(index);
                if (arg == 0) {
                    return null;
                }
                if (GL_CONSTANTS.containsKey(arg)) {
                    return GL_CONSTANTS.get(arg);
                }
                return String.valueOf(arg);
            }
            protected int getLimit() {
                return 2;
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

        static BufferFormatter<FloatBuffer> FLOAT_FIXED = new BufferFormatter<FloatBuffer>() {
            protected String format(FloatBuffer buffer, int index) {
                return String.format("%10.4f", buffer.get(index));
            }
        };

        static BufferFormatter<DoubleBuffer> DOUBLE = new BufferFormatter<DoubleBuffer>() {
            protected String format(DoubleBuffer buffer, int index) {
                return String.format("%.4f", buffer.get(index));
            }
        };

        static BufferFormatter<DoubleBuffer> DOUBLE_FIXED = new BufferFormatter<DoubleBuffer>() {
            protected String format(DoubleBuffer buffer, int index) {
                return String.format("%10.4f", buffer.get(index));
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
            boolean elipsis = (limit << 1) > buffer.limit();
            boolean appendSeparator = false;
            for (int i = 0, l = buffer.limit(); i < l; i++) {
                if (elipsis && i == limit && l > limit * 2) {
                    string.append("...");
                    i = l - limit - 1;
                    appendSeparator = false;
                } else {
                    String format = format(buffer, i);
                    if (format != null) {
                        if (appendSeparator) {
                            string.append(',');
                        }
                        string.append(format);
                        appendSeparator = true;
                    }
                }
            }
            string.append(']');
            return string.toString();
        }

        public String toLogMatrix(B buffer, int w, int h) {
            if (buffer == null) {
                return null;
            }
            StringBuilder string = new StringBuilder();
            int k = 0;
            for (int j = 0; j < h; j++) {
                if (j > 0) {
                    string.append('\n');
                }
                string.append("|");
                for (int i = 0; i < w; i++) {
                    if (i > 0) {
                        string.append(',');
                    }
                    string.append(format(buffer, k++));
                }
                string.append("|");
            }
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

        public LogMatrix wrapForMatrix(final String name, final B buffer, final int w, final int h) {
            return new LogMatrix() {

                public String name() {
                    return name;
                }

                @Override
                public String toString() {
                    return "\t" + name + "\t" + BufferFormatter.this.toLogMatrix(buffer, w, h).replace("\n", "\n\t\t");
                }
            };
        }
    }

    private static class StackTrace {

        private final List<StackTraceElement> stack;

        private StackTrace(int depth) {
            List<StackTraceElement> stack = Arrays.asList(new Exception().getStackTrace());
            this.stack = stack.subList(depth + 2, Math.min(stack.size(), depth + 10));
        }

        @Override
        public String toString() {
            StackTraceElement ste = stack.get(0);
            return String.format("%s:%d", ste.getFileName(), ste.getLineNumber());
        }

        public void printFullStack() {
            for (StackTraceElement ste : stack) {
                System.out.println("\tat " + ste);
            }
        }

        public String getFileName() {
            return stack.isEmpty() ? null : stack.get(0).getFileName();
        }

        public int getLineNumber() {
            return stack.isEmpty() ? -1 : stack.get(0).getLineNumber();
        }

        @Override
        public int hashCode() {
            return stack.hashCode();
        }
    }

    public interface LogMatrix {
        String name();
    }
}
