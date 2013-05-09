package net.venaglia.realms.common.projection.shaders;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import static net.venaglia.realms.common.util.CallLogger.*;
import static net.venaglia.realms.common.util.CallLogger.logCall;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL20.*;

/**
 * User: ed
 * Date: 3/15/13
 * Time: 10:55 PM
 */
public class ShaderCompiler {

    enum Type {
        VERTEX(GL_VERTEX_SHADER), FRAGMENT(GL_FRAGMENT_SHADER);

        final int code;

        Type(int code) {
            this.code = code;
        }

        String getDefaultSource() {
            switch (this) {
                case VERTEX:
                    return "void main() {\n" +
//                           "  gl_Normal = gl_NormalMatrix * gl_Normal;\n" +
                           "  gl_Position = ftransform();\n" +
                           "  gl_FrontColor = gl_Color;\n" +
                           "  gl_BackColor = gl_Color;\n" +
                           "}";
                case FRAGMENT:
                    return "void main() {\n" +
                           "  gl_FragColor = gl_Color;\n" +
                           "}";
            }
            return null;
        }
    }

    private Map<Type,CharSequence> sources =
            new EnumMap<Type,CharSequence>(Type.class);
    private String[] textureNames = ShaderProgram.DEFAULT_SHADER.getTextureNames();

    public ShaderCompiler setVertexShaderSource(CharSequence sourceCode) {
        sources.put(Type.VERTEX, sourceCode);
        return this;
    }

    public ShaderCompiler useDefaultVertexShader() {
        this.sources.put(Type.VERTEX, Type.VERTEX.getDefaultSource());
        return this;
    }

    public ShaderCompiler setFragmentShaderSource(CharSequence sourceCode) {
        sources.put(Type.FRAGMENT, sourceCode);
        return this;
    }

    public ShaderCompiler useDefaultFragmentShader() {
        this.textureNames = ShaderProgram.DEFAULT_SHADER.getTextureNames();
        this.sources.put(Type.FRAGMENT, Type.FRAGMENT.getDefaultSource());
        return this;
    }

    public ShaderCompiler setTextureNames(String... textureNames) {
        this.textureNames = textureNames;
        return this;
    }

    public ShaderProgram compile() {
        if (sources.size() != Type.values().length) {
            EnumSet<Type> missing = EnumSet.allOf(Type.class);
            missing.removeAll(sources.keySet());
            throw new IllegalStateException("Not all shader types have had sources set: " + missing);
        }
        int glProgramId = glCreateProgram();
        if (logCalls) logCall(glProgramId, "glCreateProgram");
        int glVertexShaderId = compileShader(Type.VERTEX);
        glAttachShader(glProgramId, glVertexShaderId);
        if (logCalls) logCall("glAttachShader", (long)glProgramId, (long)glVertexShaderId);
        int glFragmentShaderId = compileShader(Type.FRAGMENT);
        glAttachShader(glProgramId, glFragmentShaderId);
        if (logCalls) logCall("glAttachShader", (long)glProgramId, (long)glFragmentShaderId);
        linkProgram(glProgramId);
        glUseProgram(glProgramId);
        if (logCalls) logCall("glUseProgram", (long)glProgramId);
        for (int i = 0, l = textureNames.length; i < l; i++) {
            int loc = glGetUniformLocation(glProgramId, textureNames[i]);
            if (logCalls) logCall(loc, "glGetUniformLocation", (long)glProgramId, textureNames[i]);
            glUniform1i(loc, GL_TEXTURE0 + i);
            if (logCalls) logCall("glUniform1i", (long)loc, GL_TEXTURE0 + i);
        }
        return new ShaderProgramImpl(glProgramId, glVertexShaderId, glFragmentShaderId, textureNames);
    }

    private int compileShader(Type shaderType) {
        int glShaderId = glCreateShader(shaderType.code);
        if (logCalls) logCall(glShaderId, "glCreateShader", shaderType.code);
        glShaderSource(glShaderId, sources.get(shaderType));
        if (logCalls) logCall("glShaderSource", (long)glShaderId, sources.get(shaderType));
        glCompileShader(glShaderId);
        if (logCalls) logCall("glCompileShader", (long)glShaderId);
        int status = glGetShaderi(glShaderId, GL_COMPILE_STATUS);
        if (logCalls) logCall(status, "glGetShaderi", (long)glShaderId, GL_COMPILE_STATUS);
        if (status == GL_FALSE) {
            throw new ShaderException("Failed to compile shader: \n" + sources.get(shaderType));
        }
        return glShaderId;
    }

    private void linkProgram(int glProgramId, int... glShaderIds) {
        for (int glShaderId : glShaderIds) {
            glAttachShader(glProgramId, glShaderId);
            if (logCalls) logCall("glAttachShader", (long)glProgramId, (long)glShaderId);
        }
        glLinkProgram(glProgramId);
        if (logCalls) logCall("glLinkProgram", (long)glProgramId);
        glValidateProgram(glProgramId);
        if (logCalls) logCall("glValidateProgram", (long)glProgramId);
    }

    private static class ShaderProgramImpl implements ShaderProgram {

        private boolean active = true;
        private int glProgramId;
        private int glVertexShaderId;
        private int glFragmentShaderId;
        private String[] textureNames;

        private ShaderProgramImpl(int glProgramId, int glVertexShaderId, int glFragmentShaderId, String[] textureNames) {
            this.glProgramId = glProgramId;
            this.glVertexShaderId = glVertexShaderId;
            this.glFragmentShaderId = glFragmentShaderId;
            this.textureNames = textureNames;
        }

        public int getGlProgramId() {
            if (!active) {
                throw new IllegalStateException();
            }
            return glProgramId;
        }

        public String[] getTextureNames() {
            return textureNames;
        }

        public void deallocate() {
            if (active) {
                active = false;
                glDeleteProgram(glProgramId);
                if (logCalls) logCall("glDeleteProgram", (long)glProgramId);
                glDeleteShader(glFragmentShaderId);
                if (logCalls) logCall("glDeleteShader", (long)glFragmentShaderId);
                glDeleteShader(glVertexShaderId);
                if (logCalls) logCall("glDeleteShader", (long)glVertexShaderId);
            }
        }
    }
}
