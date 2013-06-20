package net.venaglia.gloo.projection.shaders;

/**
 * User: ed
 * Date: 3/15/13
 * Time: 10:53 PM
 */
public interface ShaderProgram {

    ShaderProgram DEFAULT_SHADER = new ShaderProgram() {

        private final String[] textureNames = {};

        public int getGlProgramId() {
            return 0;
        }

        public String[] getTextureNames() {
            return textureNames;
        }

        public void deallocate() {
            throw new UnsupportedOperationException();
        }
    };

    int getGlProgramId();

    String[] getTextureNames();

    void deallocate();
}
