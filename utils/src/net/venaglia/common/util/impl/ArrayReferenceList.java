package net.venaglia.common.util.impl;

import java.util.AbstractList;
import java.util.RandomAccess;

/**
 * User: ed
 * Date: 1/16/13
 * Time: 5:40 PM
 */
public class ArrayReferenceList<T> extends AbstractList<T> implements RandomAccess {

    private final T[] array;
    private final int[] indices;

    private ArrayReferenceList(T[] array, int[] indices) {
        this.array = array;
        this.indices = indices;
    }

    @Override
    public T get(int i) {
        return array[indices[i]];
    }

    @Override
    public T set(int i, T t) {
        return array[indices[i]] = t;
    }

    @Override
    public int size() {
        return indices.length;
    }

    public static <T> ArrayReferenceList<T> createFor(T[] array, int... indices) {
        return new ArrayReferenceList<T>(array, indices);
    }

    public boolean validateIndices() {
        int max = array.length - 1;
        for (int index : indices) {
            if (index < 0 || index > max) {
                return false;
            }
        }
        return true;
    }
}
