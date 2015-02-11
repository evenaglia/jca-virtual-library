package net.venaglia.realms.common.map.world.ref;

import net.venaglia.common.util.IntIterator;
import net.venaglia.common.util.IntLookup;
import net.venaglia.common.util.Series;
import net.venaglia.realms.common.map.world.AcreDetail;
import net.venaglia.realms.common.map.world.AcreIdSet;

import java.util.Iterator;

/**
 * User: ed
 * Date: 1/29/15
 * Time: 7:32 PM
 */
public interface AcreLookup extends IntLookup<AcreDetail> {

    class ArrayWrapper implements AcreLookup {

        private final AcreDetail[] acres;

        public ArrayWrapper(AcreDetail[] acres) {
            this.acres = acres;
        }

        @Override
        public AcreDetail get(int key) {
            return acres[key];
        }
    }

    default Series<AcreDetail> asSeries(AcreIdSet acreIdSet) {
        return new Series<AcreDetail>() {
            @Override
            public int size() {
                return acreIdSet.size();
            }

            @Override
            public Iterator<AcreDetail> iterator() {
                IntIterator iterator = acreIdSet.iterator();
                return new Iterator<AcreDetail>() {
                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public AcreDetail next() {
                        return get(iterator.next());
                    }

                    @Override
                    public void remove() {
                        iterator.remove();
                    }
                };
            }
        };
    }
}
