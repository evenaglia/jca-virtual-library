package net.venaglia.realms.builder.terraform;

import net.venaglia.realms.common.map.world.AcreDetail;

/**
 * User: ed
 * Date: 11/28/14
 * Time: 3:38 PM
 */
public interface TransferBuffer {

    int getAcreId1();

    AcreDetail getAcre1();

    int getAcreId2();

    AcreDetail getAcre2();

    boolean isSourceAcre(AcreDetail acreDetail);

    boolean isDestinationAcre(AcreDetail acreDetail);

    int getIndex();

    void setTransfer(double transfer);

    double getTransfer();

    MineralContent getAcre1MineralContent(MineralContentFactory.Neighbor neighbor);

    MineralContent getAcre2MineralContent(MineralContentFactory.Neighbor neighbor);
}
