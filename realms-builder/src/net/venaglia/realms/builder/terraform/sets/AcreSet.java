package net.venaglia.realms.builder.terraform.sets;

import net.venaglia.common.util.Series;
import net.venaglia.realms.builder.terraform.AcreNavigator;
import net.venaglia.realms.common.map.world.AcreDetail;
import net.venaglia.realms.common.map.world.AcreIdSet;
import net.venaglia.realms.common.map.world.ref.AcreLookup;

/**
 * User: ed
 * Date: 1/25/15
 * Time: 10:41 AM
 */
public interface AcreSet extends Series<AcreDetail>, AcreLookup {

    boolean isEmpty();

    boolean contains(AcreDetail acre);

    void add(AcreDetail acre);

    void addAll(Iterable<AcreDetail> acres);

    void remove(AcreDetail acre);

    void removeAll(Iterable<AcreDetail> acres);

    AcreIdSet getAcreIds();

    AcreNavigator navigateReadOnly();

    AcreNavigator navigateNewAcreSet();
}
