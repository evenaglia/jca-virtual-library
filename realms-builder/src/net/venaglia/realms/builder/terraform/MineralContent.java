package net.venaglia.realms.builder.terraform;

import net.venaglia.realms.common.chemistry.elements.MaterialElement;

/**
 * User: ed
 * Date: 12/3/14
 * Time: 8:21 AM
 */
public interface MineralContent {

    long CLEAN_VALUE = 0x1000000000000000l;

    float getPercent(MaterialElement element);

    void setPercent(MaterialElement element, float pct);

    void touch();

    float getTotal();

    boolean isDirty();

    void commit();

    void rollback();

    float chance();
}
