package net.venaglia.realms.builder.terraform.sets;

import net.venaglia.common.util.IntIterator;
import net.venaglia.realms.builder.terraform.AcreNavigator;
import net.venaglia.realms.common.map.world.AcreDetail;
import net.venaglia.realms.common.map.world.AcreIdSet;
import net.venaglia.realms.common.map.world.ref.AcreLookup;

import java.util.Iterator;

/**
 * User: ed
 * Date: 1/24/15
 * Time: 3:47 PM
 */
public class AcreSetImpl implements AcreSet {

    protected final boolean readonly;
    protected final AcreLookup acreLookup;
    protected final AcreIdSet acresIncluded;

    public AcreSetImpl(AcreLookup acreLookup) {
        this(false, acreLookup, 64);
    }

    public AcreSetImpl(AcreLookup acreLookup, int initialCapacity) {
        this(false, acreLookup, initialCapacity);
    }

    public AcreSetImpl(boolean readonly, AcreLookup acreLookup, int initialCapacity) {
        if (acreLookup == null) {
            throw new NullPointerException("acreLookup");
        }
        this.readonly = readonly;
        this.acreLookup = acreLookup;
        this.acresIncluded = new AcreIdSet(initialCapacity);
    }

    AcreSetImpl(boolean readonly, AcreLookup acreLookup, AcreIdSet acresIncluded) {
        if (acreLookup == null) {
            throw new NullPointerException("acreLookup");
        }
        this.readonly = readonly;
        this.acreLookup = acreLookup;
        this.acresIncluded = acresIncluded;
    }

    private void ensureNotReadOnly() {
        if (readonly) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public int size() {
        return acresIncluded.size();
    }

    @Override
    public Iterator<AcreDetail> iterator() {
        return new AcreSetIterator(acresIncluded.iterator());
    }

    @Override
    public boolean isEmpty() {
        return acresIncluded.isEmpty();
    }

    @Override
    public AcreDetail get(int key) {
        return acresIncluded.contains(key) ? acreLookup.get(key) : null;
    }

    @Override
    public boolean contains(AcreDetail acre) {
        return acre != null && acresIncluded.contains(acre.getId());
    }

    @Override
    public void add(AcreDetail acre) {
        ensureNotReadOnly();
        acresIncluded.add(acre.getId());
    }

    @Override
    public void addAll(Iterable<AcreDetail> acres) {
        ensureNotReadOnly();
        for (AcreDetail acre : acres) {
            acresIncluded.add(acre.getId());
        }
    }

    @Override
    public void remove(AcreDetail acre) {
        ensureNotReadOnly();
        acresIncluded.remove(acre.getId());
    }

    @Override
    public void removeAll(Iterable<AcreDetail> acres) {
        ensureNotReadOnly();
        for (AcreDetail acre : acres) {
            acresIncluded.remove(acre.getId());
        }
    }

    @Override
    public AcreIdSet getAcreIds() {
        return acresIncluded.clone();
    }

    @Override
    public AcreNavigator navigateReadOnly() {
        return new BasicAcreSetNavigator(acresIncluded, acreLookup);
    }

    @Override
    public AcreNavigator navigateNewAcreSet() {
        return new AcreSetBuilder(acreLookup, acresIncluded);
    }

    protected class AcreSetIterator implements Iterator<AcreDetail> {

        protected final IntIterator iterator;

        public AcreSetIterator(IntIterator iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public AcreDetail next() {
            return acreLookup.get(iterator.next());
        }

        @Override
        public void remove() {
            ensureNotReadOnly();
            iterator.remove();
        }
    }

}
