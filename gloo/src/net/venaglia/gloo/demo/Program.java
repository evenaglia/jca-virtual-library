package net.venaglia.gloo.demo;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

import net.venaglia.gloo.projection.AbstractLoadableElement;
import net.venaglia.gloo.projection.Shader;

/**
 * User: ed
 * Date: 7/14/12
 * Time: 8:38 AM
 */
public class Program extends AbstractLoadableElement {

    private int program = Integer.MIN_VALUE;

    private final Shader[] shaders;

    public Program(Shader[] shaders) {
        this.shaders = shaders;
    }

    protected void loadImpl() {
        program = glCreateProgram();

        for (Shader shader : shaders) {
            shader.attach(program);
        }

        glLinkProgram(program);

        int status = glGetProgram(program, GL_LINK_STATUS);
        if (status == GL_FALSE) {
            int infoLogLength = glGetProgram(program, GL_INFO_LOG_LENGTH);

            String strInfoLog = glGetProgramInfoLog(program, infoLogLength);
            System.err.printf("Linker failure: %s\n", strInfoLog);
        }

        for (Shader shader : shaders) {
            shader.detach(program);
        }
    }

    @Override
    protected void unloadImpl() {
        glDeleteProgram(program);
        program = Integer.MIN_VALUE;
    }

    public void use() {
        ensureLoaded();
        glUseProgram(program);
    }
}
