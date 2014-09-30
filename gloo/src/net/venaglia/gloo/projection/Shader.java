package net.venaglia.gloo.projection;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.*;

/**
 * User: ed
 * Date: 7/14/12
 * Time: 9:02 AM
 */
public class Shader extends AbstractLoadableElement {

    private final int shaderType;
    private final String shaderFile;

    private int shader = Integer.MIN_VALUE;

    public Shader(int shaderType, String shaderFile) {
        this.shaderType = shaderType;
        this.shaderFile = shaderFile;
    }

    @Override
    protected void loadImpl() {
        shader = glCreateShader(shaderType);
        glShaderSource(shader, shaderFile);

        glCompileShader(shader);

        int status = glGetShaderi(shader, GL_COMPILE_STATUS);
        if (status == GL_FALSE) {
            int infoLogLength = glGetShaderi(shader, GL_INFO_LOG_LENGTH);

            String infoLog = glGetShaderInfoLog(shader, infoLogLength);

            String strShaderType = null;
            switch (shaderType) {
                case GL_VERTEX_SHADER:
                    strShaderType = "vertex";
                    break;

                case GL_GEOMETRY_SHADER:
                    strShaderType = "geometry";
                    break;

                case GL_FRAGMENT_SHADER:
                    strShaderType = "fragment";
                    break;
            }

            System.err.printf("Compile failure in %s shader:\n%s\n", strShaderType, infoLog);
        }
    }

    @Override
    protected void unloadImpl() {
        glDeleteShader(shader);
        shader = Integer.MIN_VALUE;
    }

    public void attach(int program) {
        ensureLoaded();
        glAttachShader(program, shader);
    }

    public void detach(int program) {
        ensureLoaded();
        glDetachShader(program, shader);

    }
}
