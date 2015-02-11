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
 * Date: 1/29/15
 * Time: 8:25 AM
 */
public class BasicAcreSetNavigator implements AcreNavigator {

    private final AcreIdSet included;
    private final AcreLookup acreLookup;
    private final int[] neighborIds = {0,0,0,0,0,0};

    private IntIterator iterator;
    private AcreDetail current;

    public BasicAcreSetNavigator(AcreIdSet included,
                                 AcreLookup acreLookup) {
        this.included = included;
        this.acreLookup = acreLookup;
        this.iterator = included.iterator();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public AcreDetail next() {
        return current = acreLookup.get(iterator.next());
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Series<AcreDetail> neighbors() {
        if (current == null) {
            throw new IllegalStateException();
        }
        final int n = current.getNeighborIds(neighborIds);
        final AcreDetail c = current;
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
                        return acreLookup.get(neighborIds[i++]);
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
                if (c != current) {
                    throw new IllegalStateException();
                }
            }
        };
    }

    @Override
    public void push(AcreDetail acre) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        this.iterator = included.iterator();
    }

    @Override
    public AcreSet done() {
        throw new UnsupportedOperationException();
    }
}
