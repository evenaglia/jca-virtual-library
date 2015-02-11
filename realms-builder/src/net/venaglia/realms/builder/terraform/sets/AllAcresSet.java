package net.venaglia.realms.builder.terraform.sets;

import net.venaglia.common.util.IntIterator;
import net.venaglia.realms.builder.terraform.AcreNavigator;
import net.venaglia.realms.common.map.world.AcreDetail;
import net.venaglia.realms.common.map.world.AcreIdSet;
import net.venaglia.realms.common.map.world.ref.AcreLookup;

import java.util.Iterator;

/**
 * User: ed
 * Date: 1/30/15
 * Time: 10:09 PM
 */
public class AllAcresSet extends AcreLookup.ArrayWrapper implements AcreSet {

    public AllAcresSet(AcreDetail[] acres) {
        super(acres);
    }

    @Override
    public void removeAll(Iterable<AcreDetail> acres) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(AcreDetail acre) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAll(Iterable<AcreDetail> acres) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(AcreDetail acre) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AcreNavigator navigateReadOnly() {
        return new BasicAcreSetNavigator(AcreIdSet.ALL_ACRES, this);
    }

    @Override
    public AcreNavigator navigateNewAcreSet() {
        return new AcreSetBuilder(this, AcreIdSet.ALL_ACRES);
    }

    @Override
    public int size() {
        return AcreIdSet.ALL_ACRES.size();
    }

    @Override
    public AcreIdSet getAcreIds() {
        return AcreIdSet.ALL_ACRES;
    }

    @Override
    public Iterator<AcreDetail> iterator() {
        final IntIterator iterator = AcreIdSet.ALL_ACRES.iterator();
        return new Iterator<AcreDetail>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public AcreDetail next() {
                return AllAcresSet.this.get(iterator.next());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public boolean isEmpty() {
        return false; // never empty
    }

    @Override
    public boolean contains(AcreDetail acre) {
        return true;
    }
}
