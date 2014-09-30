/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-2011 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package net.venaglia.common.util;

import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that contains long as a set of ranges. While this set may report an enormous size, it is not likely
 * that it will consume a large amount of memeory if the values are mostly consecutive.
 */
public class RangeBasedLongSet implements Iterable<Long> {

    List<Range> ranges;
    long size; // although signed, this value will be manipulated as if unsigned

    private int modCount = 0;

    public RangeBasedLongSet() {
        ranges = new ArrayList<Range>();
        ranges.add(new Range(false, Long.MIN_VALUE, Long.MAX_VALUE));
        size = 0;
    }

    public RangeBasedLongSet(RangeBasedLongSet longSet) {
        ranges = new ArrayList<Range>(longSet.ranges.size());
        for (int i = 0; i < longSet.ranges.size(); i++) {
            Range range = longSet.ranges.get(i);
            ranges.add(new Range(range.isIncluded(), range.getBegin(), range.getEnd()));
        }
        size = longSet.size;
    }

    private void valdateFromTo(long from, long to) {
        if (from > to) {
            throw new IllegalArgumentException("The \"from\" value must be less " +
                                               "than or equal to the \"to\" value");
        }
    }

    private Range findRange(long value) {
        Range range = null;
        int low = 0;
        int high = ranges.size() - 1;
        while (low <= high) {
            int mid = (low + high) >> 1;
            Range midRange = ranges.get(mid);
            int cmp = midRange.compareTo(value);

            if (cmp < 0) {
                low = mid + 1;
            }
            else if (cmp > 0) {
                high = mid - 1;
            }
            else {
                midRange.setIndex(mid);
                range = midRange;
                break;
            }
        }
        return range;
    }

    private void alterRanges(Range[] newRanges) {
        int baseIndex = -1;
        int newRangeIndex = -1;
        for (int i = 0; i < newRanges.length; i++) {
            Range oNewRange = newRanges[i];
            if (oNewRange.getIndex() >= 0) {
                newRangeIndex = i;
                baseIndex = oNewRange.getIndex();
            }
        }
        for (int i = 0; i < newRanges.length; i++) {
            Range oNewRange = newRanges[i];
            int insertIndex = baseIndex + i;
            oNewRange.setIndex(insertIndex);
            if (i != newRangeIndex) {
                ranges.add(insertIndex, oNewRange);
                size += oNewRange.size();
            }
        }
        // remove any following ranges that are overlapped by newly inserted ranges.
        {
            int testIndex = baseIndex + newRanges.length - 1;
            Range thisRange = ranges.get(testIndex);
            boolean doneRemoving = (testIndex + 1) >= ranges.size();
            while (!doneRemoving) {
                Range nextRange = ranges.get(testIndex + 1);
                if ((thisRange.getEnd() + 1) == nextRange.getBegin()) {
                    doneRemoving = true;
                }
                else {
                    Range range = ranges.remove(testIndex + 1);
                    size -= range.size();
                    if (testIndex + 1 >= ranges.size()) {
                        doneRemoving = true;
                    }
                }
            }
        }
        for (int i = baseIndex + newRanges.length - 1; i >= (baseIndex - 1); i--) {
            if ((i >= 0) && (i + 1 < ranges.size())) {
                Range thisRange = ranges.get(i);
                Range nextRange = ranges.get(i + 1);
                if ((thisRange.isIncluded() == nextRange.isIncluded()) &&
                    ((thisRange.getEnd() + 1) == nextRange.getBegin()))
                {
                    // consolidate these adjacent ranges, size is not modified here.
                    nextRange.setBegin(thisRange.getBegin());
                    ranges.remove(i);
                }
            }
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(long value) {
        return findRange(value).isIncluded();
    }

    public boolean add(long value) {
        Range range = findRange(value);
        boolean changed = false;
        if (!range.isIncluded()) {
            Range newRange = new Range(true, value, value);
            Range[] modifyRanges;
            if ((range.getBegin() == value) && (range.getEnd() == value)) {
                size += range.setIncluded(true);
                modifyRanges = new Range[]{range};
            }
            else if (range.getBegin() == value) {
                size += range.setBegin(value + 1);
                modifyRanges = new Range[]{newRange, range};
            }
            else if (range.getEnd() == value) {
                size += range.setEnd(value - 1);
                modifyRanges = new Range[]{range, newRange};
            }
            else {
                long endValue = range.getEnd();
                size += range.setEnd(value - 1);
                modifyRanges = new Range[]{range, newRange, new Range(false, value + 1, endValue)};
            }
            alterRanges(modifyRanges);
            modCount++;
            changed = true;
        }
        return changed;
    }

    public boolean remove(long value) {
        Range range = findRange(value);
        boolean changed = false;
        if (range.isIncluded()) {
            Range[] modifyRanges;
            if ((range.getBegin() == value) && (range.getEnd() == value)) {
                size += range.setIncluded(false);
                modifyRanges = new Range[]{range};
            }
            else if (range.getBegin() == value) {
                size += range.setBegin(value + 1);
                modifyRanges = new Range[]{new Range(false, value, value), range};
            }
            else if (range.getEnd() == value) {
                size += range.setEnd(value - 1);
                modifyRanges = new Range[]{range, new Range(false, value, value)};
            }
            else {
                long endValue = range.getEnd();
                size += range.setEnd(value - 1);
                modifyRanges = new Range[]{range, new Range(false, value, value),
                        new Range(true, value + 1, endValue)};
            }
            alterRanges(modifyRanges);
            modCount++;
            changed = true;
        }
        return changed;
    }

    public boolean containsAll(long from, long to) {
        valdateFromTo(from, to);
        Range range = findRange(from);
        return range.isIncluded() && range.contains(to);
    }

    public boolean containsAll(RangeBasedLongSet values) {
        boolean containsAll = true;
        for (int i = 0; i < values.ranges.size(); i++) {
            Range range = values.ranges.get(i);
            if (range.isIncluded() && (!containsAll(range.getBegin(), range.getEnd()))) {
                containsAll = false;
                break;
            }
        }
        return containsAll;
    }

    public boolean containsAny(long from, long to) {
        try {
            long value = getNext(from);
            return value <= to;
        }
        catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean containsAny(RangeBasedLongSet values) {
        boolean containsAny = false;
        for (int i = 0; i < values.ranges.size(); i++) {
            Range range = values.ranges.get(i);
            if (range.isIncluded() && (containsAny(range.getBegin(), range.getEnd()))) {
                containsAny = true;
                break;
            }
        }
        return containsAny;
    }

    public long getNext(long value) throws NoSuchElementException {
        Range range = findRange(value);
        if ((!range.isIncluded()) && (range.getEnd() == Long.MAX_VALUE)) {
            throw new NoSuchElementException();
        }
        return range.isIncluded() ? value : range.getEnd() + 1;
    }

    public long getNextNotIncluded(long value) throws NoSuchElementException {
        Range range = findRange(value);
        if (range.isIncluded() && (range.getEnd() == Long.MAX_VALUE)) {
            throw new NoSuchElementException();
        }
        return range.isIncluded() ? range.getEnd() + 1 : value;
    }

    public boolean addAll(long from, long to) {
        valdateFromTo(from, to);
        Range firstRange = findRange(from);
        Range lastRange = findRange(to);
        Range[] modifyRanges = null;
        if (firstRange.isIncluded() && lastRange.isIncluded()) {
            if (firstRange.getIndex() == lastRange.getIndex()) {
                // nothing to do, already included
            }
            else {
                size += firstRange.setEnd(lastRange.getEnd());
                modifyRanges = new Range[]{firstRange};
                alterRanges(modifyRanges);
                modCount++;
            }
        }
        else if (lastRange.isIncluded()) {
            if (firstRange.getBegin() == from) {
                size += firstRange.setIncluded(true);
                size += firstRange.setEnd(lastRange.getEnd());
                modifyRanges = new Range[]{firstRange};
                alterRanges(modifyRanges);
                modCount++;
            }
            else {
                size += firstRange.setEnd(from - 1);
                Range newRange = new Range(true, from, lastRange.getBegin() - 1);
                modifyRanges = new Range[]{firstRange, newRange};
                alterRanges(modifyRanges);
                modCount++;
            }
        }
        else if (firstRange.isIncluded()) {
            size += firstRange.setEnd(to);
            if (lastRange.getEnd() != to) {
                lastRange.setBegin(to + 1);
            }
            modifyRanges = new Range[]{firstRange};
            alterRanges(modifyRanges);
            modCount++;
        }
        else if ((firstRange.getBegin() == from) && (lastRange.getEnd() == to)) {
            size += firstRange.setIncluded(true);
            size += firstRange.setEnd(to);
            modifyRanges = new Range[]{firstRange};
            alterRanges(modifyRanges);
            modCount++;
        }
        else if (firstRange.getBegin() == from) {
            Range newRange = new Range(false, to + 1, lastRange.getEnd());
            size += firstRange.setIncluded(true);
            size += firstRange.setEnd(to);
            modifyRanges = new Range[]{firstRange, newRange};
            alterRanges(modifyRanges);
            modCount++;
        }
        else if (lastRange.getEnd() == to) {
            Range newRange = new Range(true, from, to);
            size += firstRange.setEnd(from - 1);
            modifyRanges = new Range[]{firstRange, newRange};
            alterRanges(modifyRanges);
            modCount++;
        }
        else {
            if (firstRange.getIndex() == lastRange.getIndex()) {
                Range newRange = new Range(true, from, to);
                Range newRange2 = new Range(false, to + 1, firstRange.getEnd());
                size += firstRange.setEnd(from - 1);
                modifyRanges = new Range[]{firstRange, newRange, newRange2};
                alterRanges(modifyRanges);
                modCount++;
            }
            else {
                Range newRange = new Range(true, from, to);
                size += firstRange.setEnd(from - 1);
                size += lastRange.setBegin(to + 1);
                modifyRanges = new Range[]{firstRange, newRange};
                alterRanges(modifyRanges);
                modCount++;
            }
        }
        return modifyRanges != null;
    }

    public boolean addAll(RangeBasedLongSet values) {
        boolean changed = false;
        for (int i = 0; i < values.ranges.size(); i++) {
            Range range = values.ranges.get(i);
            if (range.isIncluded()) {
                changed |= addAll(range.getBegin(), range.getEnd());
            }
        }
        return changed;
    }

    public boolean retainAll(long from, long to) {
        valdateFromTo(from, to);
        RangeBasedLongSet tempSet = new RangeBasedLongSet();
        tempSet.addAll(from, to);
        return retainAll(tempSet);
    }

    public boolean retainAll(RangeBasedLongSet values) {
        boolean changed = false;
        List<Range> ranges = new ArrayList<Range>(this.ranges.size());
        Iterator<Range> leftRanges = this.ranges.iterator();
        Iterator<Range> rightRanges = values.ranges.iterator();
        Range left = leftRanges.next();
        Range right = rightRanges.next();
        boolean done = false;
        while (!done) {
            // This loops increments based on the left side;
            // left and right will always begin at the same value.
            changed |= (left.isIncluded() != right.isIncluded());
            if (left.getEnd() == right.getEnd()) {
                ranges.add(new Range(left.isIncluded() && right.isIncluded(), left.getBegin(), left.getEnd()));
                if (leftRanges.hasNext()) {
                    left = leftRanges.next();
                    right = rightRanges.next();
                }
                else {
                    done = true;
                }
            }
            else if (left.getEnd() < right.getEnd()) {
                ranges.add(new Range(left.isIncluded() && right.isIncluded(), left.getBegin(), left.getEnd()));
                right = new Range(right.isIncluded(), left.getEnd() + 1, right.getEnd());
                left = leftRanges.next();
            }
            else if (left.getEnd() > right.getEnd()) {
                ranges.add(new Range(left.isIncluded() && right.isIncluded(), left.getBegin(), right.getEnd()));
                left = new Range(left.isIncluded(), right.getEnd() + 1, left.getEnd());
                right = rightRanges.next();
            }
        }

        if (changed) {
            // Consolidate and optimize ranges.
            Iterator<Range> iterator = ranges.iterator();
            Range leftRange = iterator.next();
            long size = leftRange.size();
            while (iterator.hasNext()) {
                Range rightRange = iterator.next();
                size += rightRange.size();
                if (rightRange.isIncluded() == leftRange.isIncluded()) {
                    leftRange.setEnd(rightRange.getEnd());
                    iterator.remove();
                }
                else {
                    leftRange = rightRange;
                }
            }
            this.ranges.clear();
            this.ranges = ranges;
            this.size = size;
            modCount++;
        }
        return changed;
    }

    public boolean removeAll(long from, long to) {
        valdateFromTo(from, to);
        Range firstRange = findRange(from);
        Range lastRange = findRange(to);
        Range[] modifyRanges = null;
        if (!firstRange.isIncluded() && !lastRange.isIncluded()) {
            if (firstRange.getIndex() == lastRange.getIndex()) {
                // nothing to do, already not included
            }
            else {
                size += firstRange.setEnd(lastRange.getEnd());
                modifyRanges = new Range[]{firstRange};
            }
        }
        else if (!lastRange.isIncluded()) {
            if (firstRange.getBegin() == from) {
                size += firstRange.setIncluded(false);
                size += firstRange.setEnd(lastRange.getEnd());
                modifyRanges = new Range[]{firstRange};
            }
            else {
                size += firstRange.setEnd(from - 1);
                Range newRange = new Range(false, from, lastRange.getBegin() - 1);
                modifyRanges = new Range[]{firstRange, newRange};
            }
        }
        else if (!firstRange.isIncluded()) {
            size += firstRange.setEnd(to);
            if (lastRange.getEnd() != to) {
                size += lastRange.setBegin(to + 1);
            }
            modifyRanges = new Range[]{firstRange};
        }
        else if ((firstRange.getBegin() == from) && (lastRange.getEnd() == to)) {
            size += firstRange.setIncluded(false);
            size += firstRange.setEnd(to);
            modifyRanges = new Range[]{firstRange};
        }
        else if (firstRange.getBegin() == from) {
            Range newRange = new Range(true, to + 1, lastRange.getEnd());
            size += firstRange.setIncluded(false);
            size += firstRange.setEnd(to);
            modifyRanges = new Range[]{firstRange, newRange};
        }
        else if (lastRange.getEnd() == to) {
            Range newRange = new Range(false, from, to);
            size += firstRange.setEnd(from - 1);
            modifyRanges = new Range[]{firstRange, newRange};
        }
        else {
            if (firstRange.getIndex() == lastRange.getIndex()) {
                Range newRange = new Range(false, from, to);
                Range newRange2 = new Range(true, to + 1, firstRange.getEnd());
                size += firstRange.setEnd(from - 1);
                modifyRanges = new Range[]{firstRange, newRange, newRange2};
            }
            else {
                Range newRange = new Range(false, from, to);
                size += firstRange.setEnd(from - 1);
                size += lastRange.setBegin(to + 1);
                modifyRanges = new Range[]{firstRange, newRange};
            }
        }
        if (modifyRanges != null) {
            alterRanges(modifyRanges);
            modCount++;
        }
        return modifyRanges != null;
    }

    public boolean removeAll(RangeBasedLongSet values) {
        boolean changed = false;
        for (int i = 0; i < values.ranges.size(); i++) {
            Range range = values.ranges.get(i);
            if (range.isIncluded()) {
                changed |= removeAll(range.getBegin(), range.getEnd());
            }
        }
        return changed;
    }

    public Iterator<Long> iterator() {
        return iterator(Long.MIN_VALUE);
    }

    public Iterator<Long> iterator(long firstValue) {
        return new LongIterator(firstValue);
    }

    public void clear() {
        if (size != 0) {
            ranges.clear();
            ranges.add(new Range(false, Long.MIN_VALUE, Long.MAX_VALUE));
            size = 0;
            modCount++;
        }
    }

    public void invert() {
        for (Range range : ranges) {
            size += range.setIncluded(!range.isIncluded());
        }
        modCount++;
    }

    public int size() {
        return (size < 0 || size > ((long) Integer.MAX_VALUE)) ? Integer.MAX_VALUE : ((int) size);
    }

    public long longSize() {
        return size < 0 ? Long.MAX_VALUE : size;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RangeBasedLongSet)) {
            return false;
        }

        RangeBasedLongSet oThat = (RangeBasedLongSet) o;

        return ranges.equals(oThat.ranges);

    }

    public int hashCode() {
        return ranges.hashCode();
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        boolean firstRange = true;
        buffer.append('[');
        for (Range range : ranges) {
            if (range.isIncluded()) {
                if (firstRange) {
                    firstRange = false;
                }
                else {
                    buffer.append(',');
                }
                buffer.append(range.toString());
            }
        }
        buffer.append(']');
        return buffer.toString();
    }

    public RangeBasedLongSet clone() {
        return new RangeBasedLongSet(this);
    }

    class Range {

        private boolean included;
        private long begin; // inclusive
        private long end;   // inclusive
        private int index = -1;

        Range(boolean included, long begin, long end) {
            this.included = included;
            this.begin = begin;
            this.end = end;
        }

        int compareTo(long value) {
            return (begin > value) ? 1 : ((end < value) ? -1 : 0);
        }

        boolean isIncluded() {
            return included;
        }

        long setIncluded(boolean included) {
            long previousSize = size();
            this.included = included;
            return size() - previousSize;
        }

        long getBegin() {
            return begin;
        }

        long setBegin(long begin) {
            long previousSize = size();
            this.begin = begin;
            return size() - previousSize;
        }

        long getEnd() {
            return end;
        }

        long setEnd(long end) {
            long previousSize = size();
            this.end = end;
            return size() - previousSize;
        }

        /**
         * only valid when obtained immediately after calling findRange().
         */
        int getIndex() {
            return index;
        }

        void setIndex(int index) {
            this.index = index;
        }

        boolean contains(long value) {
            return (value <= end) && (value >= begin);
        }

        long size() {
            return included ? end - begin + 1L : 0L;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Range range = (Range) o;

            if (included != range.included) {
                return false;
            }
            if (begin != range.begin) {
                return false;
            }
            if (end != range.end) {
                return false;
            }

            return true;
        }

        public int hashCode() {
            int result;
            result = (included ? 1 : 0);
            result = 31 * result + (int) (begin ^ (begin >>> 32));
            result = 31 * result + (int) (end ^ (end >>> 32));
            return result;
        }

        public String toString() {
            return (begin == end)
                   ? String.valueOf(begin)
                   : String.valueOf(begin) + ".." + String.valueOf(end);
        }
    }

    private class LongIterator implements Iterator<Long> {

        private int expectedModCount = modCount;
        private boolean nextIsMinValue;
        private boolean hasLastValue = false;
        private long lastValue;
        private Range nextRange = null;

        private LongIterator(long firstValue) {
            if (firstValue == Long.MIN_VALUE) {
                nextIsMinValue = true;
                nextRange = ranges.get(0);
                nextRange.setIndex(0);
                lastValue = Long.MIN_VALUE;
            }
            else {
                nextIsMinValue = false;
                lastValue = firstValue - 1;
            }
        }

        private void checkForComodification() {
            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
        }

        public boolean hasNext() {
            checkForComodification();
            if (!hasLastValue && !nextIsMinValue) {
                nextRange = findRange(lastValue);
            }
            while ((nextRange != null) && ((!nextRange.isIncluded()) || (nextRange.getEnd() <= lastValue))) {
                nextIsMinValue = false;
                int index = nextRange.getIndex() + 1;
                if (index < ranges.size()) {
                    nextRange = ranges.get(index);
                    nextRange.setIndex(index);
                }
                else {
                    nextRange = null; // no more values
                    nextIsMinValue = true; // quickens next call to hasNext();
                }
            }
            return nextRange != null;
        }

        public Long next() {
            checkForComodification();
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            hasLastValue = true;
            lastValue = nextIsMinValue ? Long.MIN_VALUE
                    : lastValue < nextRange.getBegin() ? nextRange.getBegin() : lastValue + 1;
            nextIsMinValue = false;
            return lastValue;
        }

        public void remove() {
            checkForComodification();
            if (!hasLastValue) {
                throw new IllegalStateException();
            }
            RangeBasedLongSet.this.remove(lastValue);
            expectedModCount = modCount;
            hasLastValue = false;
        }
    }

    private static final Pattern MATCH_RANGE = Pattern.compile("^(-?[1-9][0-9]{0,18}|0)(?:\\.\\.(-?[1-9][0-9]{0,18}|0))?$");

    public static RangeBasedLongSet parse(String s) throws ParseException {
        if (s.startsWith("[") && s.endsWith("]")) {
            RangeBasedLongSet set = new RangeBasedLongSet();
            if (s.length() == 2) {
                return set;
            }
            int index = 1;
            for (String range : s.substring(1, s.length() - 1).split(",")) {
                Matcher matcher = MATCH_RANGE.matcher(range);
                if (matcher.find()) {
                    if (matcher.group(2) == null) {
                        set.add(Long.parseLong(matcher.group(1)));
                    }
                    else {
                        set.addAll(Long.parseLong(matcher.group(1)), Long.parseLong(matcher.group(2)));
                    }
                }
                else {
                    throw new ParseException(s, index);
                }
                index += range.length() + 1;
            }
            return set;
        }
        else {
            throw new ParseException(s, 0);
        }
    }
}
