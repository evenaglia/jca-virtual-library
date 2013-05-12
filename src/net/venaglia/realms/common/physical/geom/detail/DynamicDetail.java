package net.venaglia.realms.common.physical.geom.detail;

import net.venaglia.realms.common.physical.bounds.Bounded;
import net.venaglia.realms.common.physical.bounds.BoundingBox;
import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.Projectable;
import net.venaglia.realms.common.projection.ProjectionBuffer;
import net.venaglia.realms.common.util.Ref;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * User: ed
 * Date: 4/23/13
 * Time: 10:27 AM
 */
public class DynamicDetail<P extends Projectable & Bounded> implements Projectable, Bounded {

    private static final DetailLevel[] DETAIL_LEVELS = DetailLevel.values();


    // todo: fiddle with the factor to get the right detail level
    private static final double VIEW_ANGLE_FACTOR = 1303.7972938088067; // n = 4096 / PI

    private final Map<DetailLevel,P> alternates;
    private final DetailLevel defaultDetailLevel;
    private final Ref<DetailComputer> detailComputer;
    private final BoundingBox bounds;
    private final float sizeFactor;

    private DetailListener detailListener;

    public DynamicDetail (DynamicDetailSource<P> source,
                          DetailLevel defaultDetailLevel,
                          Ref<DetailComputer> detailComputer) {
        this.alternates = new EnumMap<DetailLevel,P>(DetailLevel.class);
        this.defaultDetailLevel = defaultDetailLevel;
        this.detailComputer = detailComputer;
        List<BoundingBox> boxes = new ArrayList<BoundingBox>(DETAIL_LEVELS.length);
        for (DetailLevel detailLevel : DetailLevel.values()) {
            P projectable = source.produceAt(detailLevel);
            if (projectable != null) {
                this.alternates.put(detailLevel, projectable);
                boxes.add(projectable.getBounds().asBox());
            }
        }
        this.bounds = new BoundingBox(boxes.toArray(new BoundingBox[boxes.size()]));
        this.sizeFactor = source.getSizeFactor();
    }

    public boolean isStatic() {
        return false;
    }

    public void project(long nowMS, GeometryBuffer buffer) {
        if (buffer instanceof ProjectionBuffer) {
            project(nowMS, (ProjectionBuffer)buffer);
        } else if (defaultDetailLevel != null) {
            usingDetail(defaultDetailLevel);
            P projectable = alternates.get(defaultDetailLevel);
            if (projectable != null) {
                projectable.project(nowMS, buffer);
            }
        }
    }

    public void project(long nowMS, ProjectionBuffer buffer) {
        DetailLevel detailLevel = null;
        if (detailComputer != null) {
            DetailComputer computer = detailComputer.get();
            detailLevel = computer == null ? null : computer.computeDetail(buffer.getCameraViewPoint(), getBounds().getLongestDimension());
        }
        if (detailLevel == null) {
            detailLevel = defaultDetailLevel == null ? DetailLevel.MEDIUM : defaultDetailLevel;
        }
        P projectable = alternates.get(detailLevel);
        if (projectable != null) {
            usingDetail(detailLevel);
            projectable.project(nowMS, buffer);
        }
    }

    private void usingDetail(DetailLevel detailLevel) {
        if (detailListener != null) {
            detailListener.usingDetail(detailLevel);
        }
    }

    public void setDetailListener(DetailListener detailListener) {
        this.detailListener = detailListener;
    }

    public BoundingVolume<?> getBounds() {
        return bounds;
    }
}
