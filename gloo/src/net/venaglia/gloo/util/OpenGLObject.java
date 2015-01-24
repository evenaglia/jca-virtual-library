package net.venaglia.gloo.util;

/**
 * User: ed
 * Date: 1/7/15
 * Time: 9:39 AM
 */
public interface OpenGLObject {

    OpenGLObject NULL = new OpenGLObject() {
        @Override
        public int getOpenGLObjectId() {
            return Integer.MIN_VALUE;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    };

    int getOpenGLObjectId();

    boolean equals(Object o);
}
