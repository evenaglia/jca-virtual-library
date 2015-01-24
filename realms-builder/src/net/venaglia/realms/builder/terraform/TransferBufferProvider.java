package net.venaglia.realms.builder.terraform;

import net.venaglia.realms.common.map.world.AcreDetail;

/**
 * User: ed
 * Date: 11/28/14
 * Time: 3:37 PM
 */
public interface TransferBufferProvider {

    TransferBuffer getTransferBufferFor(int left, int right, AcreDetail acre, AcreDetail neighbor);
}
