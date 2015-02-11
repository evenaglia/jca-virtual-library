package net.venaglia.realms.builder.terraform;

import net.venaglia.common.util.extensible.ExtendedPropertyKey;
import net.venaglia.common.util.extensible.ExtendedPropertyProvider;
import net.venaglia.realms.builder.terraform.sets.AcreDetailExtendedPropertyProvider;
import net.venaglia.realms.common.map.world.AcreDetail;

/**
 * User: ed
 * Date: 2/3/15
 * Time: 8:14 AM
 */
public class AcreTags {

    private final ExtendedPropertyKey.IntKey key = new ExtendedPropertyKey.IntKey("tags");
    private final ExtendedPropertyProvider.IntProvider<AcreDetail> bits =
            new AcreDetailExtendedPropertyProvider.IntProvider(key);

    public enum Tag {
        COASTAL_BOUNDARY,
        GEOGRAPHIC_PEAK,
        RIDGE_LINE,
        RIVER_HEAD,
        WATERWAY,
        CONFLUENCE,
        LAKE_BOTTOM,
        LAKE_BOUNDARY;

        private final int mask = 1 << ordinal();
    }

    public boolean hasTag(AcreDetail acre, Tag tag) {
        return (getBits(acre) & tag.mask) != 0;
    }

    public boolean hasAnyTag(AcreDetail acre, Tag... tags) {
        return (getBits(acre) & getCompositeMask(tags)) != 0;
    }

    public boolean hasAllTags(AcreDetail acre, Tag... tags) {
        int mask = getCompositeMask(tags);
        return (getBits(acre) & mask) == mask;
    }

    public boolean setTag(AcreDetail acre, Tag tag) {
        return setBits(acre, tag.mask);
    }

    public boolean clearTag(AcreDetail acre, Tag tag) {
        return unsetBits(acre, tag.mask);
    }

    public boolean isEmpty(AcreDetail acre) {
        return getBits(acre) == 0;
    }

    public int size(AcreDetail acre) {
        return Integer.bitCount(getBits(acre));
    }

    private int getCompositeMask(Tag[] tags) {
        int mask = 0;
        for (Tag tag : tags) {
            mask |= tag.mask;
        }
        return mask;
    }

    private int getBits(AcreDetail acre) {
        return bits.getInt(acre, 0);
    }

    private boolean setBits(AcreDetail acre, int toSet) {
        int b = bits.getInt(acre, 0);
        if ((b & toSet) == 0) {
            bits.setInt(acre, b | toSet);
            return true;
        }
        return false;
    }

    private boolean unsetBits(AcreDetail acre, int toUnset) {
        int b = bits.getInt(acre, 0);
        if ((b & toUnset) != 0) {
            bits.setInt(acre, b & ~toUnset);
            return true;
        }
        return false;
    }
}
