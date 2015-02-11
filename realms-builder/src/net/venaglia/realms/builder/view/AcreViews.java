package net.venaglia.realms.builder.view;

import net.venaglia.common.util.Named;
import net.venaglia.gloo.projection.Projectable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: ed
 * Date: 1/29/15
 * Time: 8:52 PM
 */
public class AcreViews<E extends Projectable & Named> {

    private List<E> views = new ArrayList<E>();
    private AtomicReference<Projectable[]> snapshot = new AtomicReference<Projectable[]>(new AcreView[0]);
    private Lock lock = new ReentrantLock();
    private int index = -1;

    public AcreViews() {
    }

    public AcreViews(Collection<E> views) {
        this.views.addAll(views);
        if (this.views.size() > 0) {
            index = 0;
        }
        this.snapshot.set(this.views.toArray(new Projectable[this.views.size()]));
    }

    @SafeVarargs
    public AcreViews(E... views) {
        Collections.addAll(this.views, views);
        if (this.views.size() > 0) {
            index = 0;
        }
        this.snapshot.set(this.views.toArray(new Projectable[this.views.size()]));
    }

    int size() {
        lock.lock();
        try {
            return views.size();
        } finally {
            lock.unlock();
        }
    }

    void nextView() {
        lock.lock();
        try {
            this.index = (this.index + 1) % views.size();
        } finally {
            lock.unlock();
        }
    }

    void previousView() {
        lock.lock();
        try {
            this.index = (this.index + views.size() - 1) % views.size();
        } finally {
            lock.unlock();
        }
    }

    E getActiveView() {
        lock.lock();
        try {
            return index >= 0 ? views.get(index) : null;
        } finally {
            lock.unlock();
        }
    }

    String getActiveViewName() {
        lock.lock();
        try {
            return index >= 0 ? views.get(index).getName() : null;
        } finally {
            lock.unlock();
        }
    }

    Projectable[] getAllViews() {
        return snapshot.get();
    }

    public void addView(E view) {
        addViewImpl(view, false);
    }

    public void addAndShowView(E view) {
        addViewImpl(view, true);
    }

    private void addViewImpl(E view, boolean showIt) {
        lock.lock();
        try {
            if (!views.contains(view)) {
                views.add(view);
                if (index == -1) {
                    index = 0;
                } else if (showIt) {
                    index = views.size() - 1;
                }
                snapshot.set(views.toArray(new Projectable[views.size()]));
            }
        } finally {
            lock.unlock();
        }
    }

    public void removeView(E view) {
        lock.lock();
        try {
            int i = views.indexOf(view);
            if (i >= 0) {
                views.remove(i);
                if (index >= i) index--;
                snapshot.set(views.toArray(new Projectable[views.size()]));
            }
        } finally {
            lock.unlock();
        }
    }

    public void removeView(String viewName) {
        lock.lock();
        try {
            for (int i = 0; i < views.size(); i++) {
                E view = views.get(i);
                if (viewName.equals(view.getName())) {
                    if (index >= i) index--;
                    views.remove(i);
                    snapshot.set(views.toArray(new Projectable[views.size()]));
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
