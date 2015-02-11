package net.venaglia.realms.builder.terraform.sets;

import net.venaglia.common.util.Series;
import net.venaglia.realms.builder.terraform.AcreNavigator;
import net.venaglia.realms.common.map.world.AcreDetail;
import net.venaglia.realms.common.map.world.AcreIdSet;
import net.venaglia.realms.common.map.world.ref.AcreLookup;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * User: ed
 * Date: 2/3/15
 * Time: 10:38 PM
 */
public class BoundaryAcreSetBuilder implements AcreNavigator {

    private AcreLookup acreLookup;
    private NeighborPredicate predicate;

    private AcreIdSet included = new AcreIdSet();
    private AcreIdSet pushable;
    private AcreIdSet pendable;
    private AcreIdSet pending = new AcreIdSet();
    private SortedSet<AcreDetail> pendingQueue;

    private AcreDetail current;
    private int[] buffer = new int[6];
    private boolean done = false;

    public BoundaryAcreSetBuilder(AcreDetail start, AcreSet bounds, Comparator<AcreDetail> order, NeighborPredicate predicate) {
        assert start != null;
        assert bounds != null;
        this.acreLookup = bounds;
        this.pushable = bounds.getAcreIds().clone();
        this.pendable = bounds.getAcreIds().clone();
        this.predicate = predicate == null ? StandardNeighborPredicate.ALWAYS : predicate;
        this.pendingQueue = new ConcurrentSkipListSet<>(order == null ? AcreOrder.ID_ORDER : order);
        if (!bounds.contains(start)) {
            throw new IllegalArgumentException("Starting acre is not within the specified bounds");
        }
        push(start);
        pendable.remove(start.getId());
    }

    @Override
    public boolean hasNext() {
        return !pending.isEmpty();
    }

    @Override
    public AcreDetail next() {
        AcreDetail acre = pendingQueue.first();
        int acreId = acre.getId();
        pending.remove(acreId);
        pendingQueue.remove(acre);
        return acre;
    }

    @Override
    public void remove() {
        if (current == null) {
            throw new IllegalStateException();
        }
        included.remove(current.getId());
        current = null;
    }

    @Override
    public Series<AcreDetail> neighbors() {
        if (current == null) {
            throw new IllegalStateException();
        }
        return neighbors(current);
    }

    @Override
    public void push(AcreDetail acre) {
        included.add(acre.getId());
        if (!pushable.remove(acre.getId())) {
            // was pushed previously, no more work to do
        }
        int n = acre.getNeighborIds(buffer);
        for (int i = 0; i < n; i++) {
            int neighborId = buffer[i];
            if (pendable.remove(neighborId)) {
                AcreDetail neighbor = acreLookup.get(neighborId);
                if (predicate.allow(acre, neighbor)) {
                    pendingQueue.add(neighbor);
                    pending.add(neighborId);
                }
            }
        }
    }

    @Override
    public void reset() {
        throw new NoSuchElementException();
    }

    @Override
    public AcreSet done() {
        ensureNotDone();
        AcreSetImpl acreSet = new AcreSetImpl(false, acreLookup, included);
        done = true;
        acreLookup = null;
        included = null;
        pushable = null;
        pendable = null;
        buffer = null;
        return acreSet;
    }

    private void ensureNotDone() {
        if (done) {
            throw new IllegalStateException();
        }
    }

    private Series<AcreDetail> neighbors(final AcreDetail acre) {
        return new Series<AcreDetail>() {

            private int[] neighborIds = new int[6];
            private int n = acre.getNeighborIds(neighborIds);

            @Override
            public Iterator<AcreDetail> iterator() {
                return new Iterator<AcreDetail>() {

                    private int i = 0;

                    @Override
                    public boolean hasNext() {
                        return i < neighborIds.length;
                    }

                    @Override
                    public AcreDetail next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        return acreLookup.get(neighborIds[i++]);
                    }
                };
            }

            @Override
            public int size() {
                return n;
            }
        };
    }

    public interface NeighborPredicate {
        boolean allow(AcreDetail acre, AcreDetail neighbor);
    }

    public enum StandardNeighborPredicate implements NeighborPredicate {
        LOWER {
            @Override
            public boolean allow(AcreDetail acre, AcreDetail neighbor) {
                return neighbor.getElevation() < acre.getElevation();
            }
        },
        HIGHER {
            @Override
            public boolean allow(AcreDetail acre, AcreDetail neighbor) {
                return neighbor.getElevation() > acre.getElevation();
            }
        },
        ALWAYS {
            @Override
            public boolean allow(AcreDetail acre, AcreDetail neighbor) {
                return true;
            }
        },
        NEVER {
            @Override
            public boolean allow(AcreDetail acre, AcreDetail neighbor) {
                return false;
            }
        }
    }

    public enum AcreOrder implements Comparator<AcreDetail> {
        LOWEST_FIRST {
            @Override
            public int compare(AcreDetail o1, AcreDetail o2) {
                int cmp = Float.compare(o1.getElevation(), o2.getElevation());
                return cmp == 0 ? o1.getId() - o2.getId() : cmp;
            }
        },
        HIGHEST_FIRST {
            @Override
            public int compare(AcreDetail o1, AcreDetail o2) {
                int cmp = Float.compare(o2.getElevation(), o1.getElevation());
                return cmp == 0 ? o2.getId() - o1.getId() : cmp;
            }
        },
        ID_ORDER {
            @Override
            public int compare(AcreDetail o1, AcreDetail o2) {
                return o1.getId() - o2.getId();
            }
        }
    }
}
