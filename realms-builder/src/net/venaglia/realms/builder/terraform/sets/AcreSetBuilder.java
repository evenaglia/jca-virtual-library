package net.venaglia.realms.builder.terraform.sets;

import net.venaglia.common.util.IntIterator;
import net.venaglia.common.util.Series;
import net.venaglia.realms.builder.terraform.AcreNavigator;
import net.venaglia.realms.common.map.world.AcreDetail;
import net.venaglia.realms.common.map.world.AcreIdSet;
import net.venaglia.realms.common.map.world.ref.AcreLookup;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
* User: ed
* Date: 1/26/15
* Time: 10:20 PM
*/
public class AcreSetBuilder implements AcreNavigator {

    protected AcreLookup acreLookup;
    protected AcreIdSet visited;
    protected AcreIdSet included;
    protected AcreIdSet pending;
    protected IntIterator pendingIterator;
    protected AcreIdSet pendingNext;
    protected AcreIdSet[] rotatingSets = {
            new AcreIdSet(),
            new AcreIdSet(),
            new AcreIdSet()
    };
    protected int rotation = 0;

    protected int id = -1;
    protected int next = 0;
    protected boolean done = false;
    protected int[] buffer = {0,0,0,0,0,0};

    public AcreSetBuilder(AcreLookup acreLookup, IntIterator seed, int initialCapacity) {
        this.acreLookup = acreLookup;
        this.visited = new AcreIdSet(initialCapacity);
        this.included = new AcreIdSet(initialCapacity);
        while (seed != null && seed.hasNext()) {
            included.add(seed.next());
        }
        this.pending = this.included.clone();
        this.pendingIterator = this.pending.iterator();
        this.pendingNext = getEmptyAcreIdSet();
    }

    AcreSetBuilder(AcreLookup acreLookup, AcreIdSet original) {
        this.acreLookup = acreLookup;
        this.visited = new AcreIdSet(original.size());
        this.included = original.clone();
        this.pending = original.clone();
        this.pendingIterator = pending.iterator();
        this.pendingNext = getEmptyAcreIdSet();
    }

    @Override
    public boolean hasNext() {
        ensureNotDone();
        if (pendingIterator == null) {
            return false;
        } else if (pendingIterator.hasNext()) {
            return true;
        } else if (pendingNext.isEmpty()) {
            pending = null;
            pendingIterator = null;
            pendingNext  = null;
            return false;
        } else {
            pending = pendingNext;
            pendingIterator = pendingNext.iterator();
            pendingNext = getEmptyAcreIdSet();
            return true;
        }
    }

    @Override
    public AcreDetail next() {
        ensureNotDone();
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        id = pendingIterator.next();
        assert !visited.contains(id);
        if (!pendingIterator.hasNext()) {
            if (pendingNext.isEmpty()) {
                pending = null;
                pendingIterator = null;
                pendingNext = null;
            } else {
                pending = pendingNext;
                pendingIterator = pendingNext.iterator();
                pendingNext = getEmptyAcreIdSet();
            }
        }
        included.add(id);
        visited.add(id);
        return acreLookup.get(id);
    }

    @Override
    public void remove() {
        ensureNotDone();
        if (pendingIterator == null) {
            throw new IllegalStateException();
        }
        pendingIterator.remove();
        included.remove(id);
        id = -1;
    }

    @Override
    public void push(AcreDetail acre) {
        ensureNotDone();
        int id = acre.getId();
        included.add(id);
        if (!isVisitedOrPending(id)) {
            if (pendingNext == null) {
                pending = AcreIdSet.EMPTY;
                pendingIterator = pending.iterator();
                pendingNext = getEmptyAcreIdSet();
            }
            pendingNext.add(id);
        }
    }

    private boolean isVisitedOrPending(int id) {
        if (visited.contains(id)) {
            return true;
        }
        if (pendingNext == null) {
            return false;
        }
        if (pending.contains(id) || pendingNext.contains(id)) {
            return true;
        }
        return false;
    }

    @Override
    public Series<AcreDetail> neighbors() {
        if (id < 0) {
            throw new IllegalStateException();
        }
        final int n = acreLookup.get(id).getNeighborIds(buffer);
        final int c = id;
        return new Series<AcreDetail>() {
            @Override
            public Iterator<AcreDetail> iterator() {
                ensureSillCurrent();
                return new Iterator<AcreDetail>() {

                    private int i = 0;

                    @Override
                    public boolean hasNext() {
                        ensureSillCurrent();
                        return i < n;
                    }

                    @Override
                    public AcreDetail next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        return acreLookup.get(buffer[i++]);
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public int size() {
                ensureSillCurrent();
                return n;
            }

            private void ensureSillCurrent() {
                if (c != id) {
                    throw new IllegalStateException();
                }
            }
        };
    }

    @Override
    public void reset() {
        ensureNotDone();
        id = -1;
        next = 0;
        this.visited = new AcreIdSet(included.size());
        this.pendingIterator = included.clone().iterator();
        this.pendingNext = getEmptyAcreIdSet();
    }

    @Override
    public AcreSet done() {
        ensureNotDone();
        AcreSetImpl acreSet = new AcreSetImpl(false, acreLookup, included);
        done = true;
        acreLookup = null;
        visited = null;
        included = null;
        pendingIterator = null;
        pendingNext = null;
        buffer = null;
        return acreSet;
    }

    protected AcreIdSet getEmptyAcreIdSet() {
        AcreIdSet set = rotatingSets[rotation];
        rotation = (rotation + 1) % rotatingSets.length;
        set.clear();
        return set;
    }

    private void ensureNotDone() {
        if (done) {
            throw new IllegalStateException();
        }
    }

    public static void main(String[] args) {
        final AcreDetail[] acres = new AcreDetail[10];
        for (int i = 0; i < acres.length; i++) {
            AcreDetail acre = new AcreDetail();
            acre.setId(i);
            acre.setNeighborIds(new int[]{ (i + 1) % acres.length, (i + acres.length - 1) % acres.length });
            acre.setElevation(6 - i);
            acres[i] = acre;
        }
        AcreLookup lookup = new AcreLookup.ArrayWrapper(acres);
        AcreIdSet acreIdSet = new AcreIdSet(10);
        acreIdSet.add(2);
        AcreSetBuilder builder = new AcreSetBuilder(lookup, acreIdSet);
        while (builder.hasNext()) {
            AcreDetail acre = builder.next();
            System.out.println("Advance to acre " + acre.getId());
            for (AcreDetail neighbor : builder.neighbors()) {
                if (neighbor.getElevation() > 0) {
                    System.out.println("\tPushing neighbor " + neighbor.getId());
                    builder.push(neighbor);
                } else {
                    System.out.println("\tSkipping neighbor " + neighbor.getId());
                }
            }
        }
        AcreSet areaAcreSet = builder.done();
        assert areaAcreSet.size() == 6;
    }
}
