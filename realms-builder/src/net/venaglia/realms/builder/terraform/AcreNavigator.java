package net.venaglia.realms.builder.terraform;

import net.venaglia.common.util.Series;
import net.venaglia.realms.builder.terraform.sets.AcreSet;
import net.venaglia.realms.common.map.world.AcreDetail;

import java.util.Iterator;

/**
 * User: ed
 * Date: 1/26/15
 * Time: 5:49 PM
 *
 * An acre navigator iterates over a set of {@link AcreDetail} objects, while
 * concurrently building a new AcreSet. Each acre returned from {@link #next()}
 * is added to the scratchpad set and may be subsequently removed again by
 * calling {@link #remove()}.
 */
public interface AcreNavigator extends Iterator<AcreDetail> {

    /**
     * @return true of there are more neighbors to visit with this iterator
     */
    boolean hasNext();

    /**
     * @return The next acre
     * @throws java.util.NoSuchElementException if there are no more acres
     */
    AcreDetail next();

    /**
     * Removes the current acre from tha AcreSet backing this AcreNavigator
     * @throws IllegalStateException if next() has not yet been called, if
     *     remove has already been called()
     * @throws UnsupportedOperationException if the acre returned is part of
     *     a line and removing it would break the linked list.
     */
    void remove();

    /**
     * @return A {@link Series} of {@link AcreDetail} objects.
     * @throws IllegalStateException if next() has not yet been called, or if
     *     remove has already been called()
     */
    Series<AcreDetail> neighbors();

    /**
     * Add the passed acre back to the scratchpad set. If the acre passed here
     * has not been iterated over, it is queued and will be before hasNext()
     * returns false. If this AcreNavigator was at the end (hasNext() returns
     * false), and that passed acre has not been visited, hasNext() will return
     * true so this acre can be visited.
     * <p>
     * Unless this AcreNavigator is being used to build a LineAcreSet, this
     * method can be invoked before next() or hasNext() is called, it may even
     * be called on an AcreSet that is initially empty.
     * @param acre An acre that should be visited later. If this AcreNavigator
     *             is backed by a mutable AcreSet, the passed acre is added to
     *             that set.
     * @throws IllegalStateException if next() has not yet been called, or if
     *     remove has already been called(), if this AcreNavigator is building
     *     a LineAcreSet.
     */
    void push(AcreDetail acre);

    /**
     * Resets this AcreNavigator back to the beginning and starts all over
     * again. <b>Note</b>: This acre set will iterate over those acres
     * "included" in scratchpad AcreSet within this AcreNavigator, which may
     * have changed since it was constructed from calls to {@link #remove()}
     * and {@link #push(AcreDetail)}.
     * @throws UnsupportedOperationException if reset isn't supported by this
     *     navigator.
     */
    void reset();

    /**
     * Completes and closes this AcreNavigator
     * @return The completed AcreSet
     * @throws UnsupportedOperationException if this navigator doesn't support
     *     the ability to create an AcreSet.
     */
    default AcreSet done() {
        throw new UnsupportedOperationException();
    }
}
