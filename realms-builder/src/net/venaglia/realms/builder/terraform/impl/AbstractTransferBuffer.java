package net.venaglia.realms.builder.terraform.impl;

import net.venaglia.realms.builder.terraform.MineralContent;
import net.venaglia.realms.builder.terraform.MineralContentFactory;
import net.venaglia.realms.builder.terraform.TransferBuffer;
import net.venaglia.realms.common.map.world.AcreDetail;

/**
 * User: ed
 * Date: 11/28/14
 * Time: 10:17 AM
 */
public abstract class AbstractTransferBuffer implements Comparable<AbstractTransferBuffer>, TransferBuffer {

    private final int acreId1;
    private final int acreId2;
    private final int myIndex;

    private double transfer;

    public AbstractTransferBuffer(int acreId1, int acreId2, int myIndex) {
        this.myIndex = myIndex;
        this.acreId1 = acreId1;
        this.acreId2 = acreId2;
    }

    @Override
    public int getAcreId1() {
        return acreId1;
    }

    @Override
    public AcreDetail getAcre1() {
        return getAcre(acreId1);
    }

    @Override
    public int getAcreId2() {
        return acreId2;
    }

    @Override
    public AcreDetail getAcre2() {
        return getAcre(acreId2);
    }

    @Override
    public boolean isSourceAcre(AcreDetail acreDetail) {
        return (transfer > 0 ? acreId1 : acreId2) == acreDetail.getId();
    }

    @Override
    public boolean isDestinationAcre(AcreDetail acreDetail) {
        return (transfer > 0 ? acreId2 : acreId1) == acreDetail.getId();
    }

    @Override
    public int getIndex() {
        return myIndex;
    }

    @Override
    public double getTransfer() {
        return transfer;
    }

    @Override
    public void setTransfer(double transfer) {
        this.transfer = transfer;
    }

    @Override
    public MineralContent getAcre1MineralContent(MineralContentFactory.Neighbor neighbor) {
        return getMineralContent(transfer > 0 ? acreId1 : acreId2, neighbor);
    }

    @Override
    public MineralContent getAcre2MineralContent(MineralContentFactory.Neighbor neighbor) {
        return getMineralContent(transfer > 0 ? acreId1 : acreId2, neighbor);
    }

    protected abstract AcreDetail getAcre(int id);

    protected abstract MineralContent getMineralContent(int id, MineralContentFactory.Neighbor neighbor);

    @Override
    public int compareTo(AbstractTransferBuffer o) {
        return compareTo(o.acreId1, o.acreId2);
    }

    public int compareTo(int acreId1, int acreId2) {
        return (this.acreId1 < acreId1)
               ? -1
               : ((this.acreId1 == acreId1)
                  ? (this.acreId2 < acreId2)
                    ? -1
                    : ((this.acreId2 == acreId2) ? 0 : 1)
                  : 1);
    }

    @Override
    public String toString() {
        return String.format("TransferBuffer[%d->%d]=%.3f", acreId1, acreId2, transfer);
    }
}
