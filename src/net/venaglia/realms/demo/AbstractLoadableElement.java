package net.venaglia.realms.demo;

/**
 * User: ed
 * Date: 7/14/12
 * Time: 9:08 AM
 */
public abstract class AbstractLoadableElement {

    private boolean loaded;

    public final boolean isLoaded() {
        return loaded;
    }

    protected final void ensureLoaded() {
        if (!loaded) {
            throw new IllegalStateException(getClass().getSimpleName() + " not loaded");
        }
    }

    public final void load() {
        if (loaded) {
            throw new IllegalStateException();
        }
        loadImpl();
        loaded = true;
    }

    public final void unload() {
        if (isLoaded()) {
            unloadImpl();
            loaded = false;
        }
    }

    protected abstract void loadImpl();

    protected abstract void unloadImpl();
}
