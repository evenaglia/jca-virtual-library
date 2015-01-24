package net.venaglia.realms.builder.terraform.impl;

import net.venaglia.realms.builder.terraform.MineralContent;
import net.venaglia.realms.common.chemistry.elements.MaterialElement;

/**
* Created with IntelliJ IDEA.
* User: ed
* Date: 10/14/14
* Time: 9:13 PM
* To change this template use File | Settings | File Templates.
*/
public class MineralContentImpl implements MineralContent {

    private static final long[] MASKS = {
            0x0000000000000FFFl,
            0x0000000000FFF000l,
            0x0000000FFF000000l,
            0x0000FFF000000000l,
            0x0FFF000000000000l
    };
    private static final long[] UNMASKS = {
            0xFFFFFFFFFFFFF000l,
            0xFFFFFFFFFF000FFFl,
            0xFFFFFFF000FFFFFFl,
            0xFFFF000FFFFFFFFFl,
            0xF000FFFFFFFFFFFFl
    };
    private static final int[] STARTS = {
            0, 12, 24, 36, 48
    };

    public MineralContentImpl() { }

    private long[] buffer;
    private int index;
    private long[] scratchpad;
    private int scratchpadIndex;

    public float getPercent(MaterialElement element) {
        return getPart(element.ordinal()) / 4096.0f;
    }

    public void setPercent(MaterialElement element, float pct) {
        setPart(element.ordinal(), Math.round(Math.min(Math.max(pct, 0.0f), 1.0f) * 4095.0f));
    }

    @Override
    public void touch() {
        setRaw(getRaw());
    }

    public float getTotal() {
        long l = getRaw();
        int sum = 0;
        sum += l & 0xFFF;
        l >>= 12;
        sum += l & 0xFFF;
        l >>= 12;
        sum += l & 0xFFF;
        l >>= 12;
        sum += l & 0xFFF;
        l >>= 12;
        sum += l & 0xFFF;
        return sum / 4096.0F;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isDirty() {
        return scratchpad[scratchpadIndex] != CLEAN_VALUE;
    }

    public void commit() {
        if (isDirty()) {
            buffer[index] = scratchpad[scratchpadIndex];
        }
    }

    public void rollback() {
        scratchpad[scratchpadIndex] = CLEAN_VALUE;
    }

    @Override
    public float chance() {
        long l = getRaw();
        float sum = 0;
        sum += (l & 0xFFF) * MaterialElement.EARTH.getNaturalOccurrence();
        l >>= 12;
        sum += (l & 0xFFF) * MaterialElement.WATER.getNaturalOccurrence();
        l >>= 12;
        sum += (l & 0xFFF) * MaterialElement.AIR.getNaturalOccurrence();
        l >>= 12;
        sum += (l & 0xFFF) * MaterialElement.FIRE.getNaturalOccurrence();
        l >>= 12;
        sum += (l & 0xFFF) * MaterialElement.PLASMA.getNaturalOccurrence();
        return sum / 4096.0F;
    }

    private int getPart(int part) {
        return (int)((getRaw() & MASKS[part]) >> STARTS[part]);
    }

    private void setPart(int part, int value) {
        long update = (long)(value & 0xFFF) << STARTS[part];
        setRaw((getRaw() & UNMASKS[part]) | update);
    }

    private void setRaw(long v) {
        scratchpad[scratchpadIndex] = v;
    }

    private long getRaw() {
        return scratchpad[scratchpadIndex] != CLEAN_VALUE ? scratchpad[scratchpadIndex] : buffer[index];
    }

    public MineralContent load(long[] buffer, int index, long[] scratchpad, int scratchpadIndex) {
        this.buffer = buffer;
        this.index = index;
        this.scratchpad = scratchpad;
        this.scratchpadIndex = scratchpadIndex;
        return this;
    }

    public String toString() {
        String hex = Long.toString(getRaw() | 0x1000000000000000l, 16);
        return hex.substring(1,4) + "," + hex.substring(4,7) + "," +
               hex.substring(7,10) + "," + hex.substring(10,13) + "," +
               hex.substring(13,16);
    }
}
