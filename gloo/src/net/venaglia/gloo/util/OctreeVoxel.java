package net.venaglia.gloo.util;

/**
* User: ed
* Date: 3/5/13
* Time: 7:26 PM
*/
public enum OctreeVoxel {

    XL_YL_ZL, XH_YL_ZL,
    XL_YH_ZL, XH_YH_ZL,
    XL_YL_ZH, XH_YL_ZH,
    XL_YH_ZH, XH_YH_ZH;

    public double splitX (double l, double h) {
        switch (this) {
            case XL_YL_ZL:
            case XL_YH_ZL:
            case XL_YL_ZH:
            case XL_YH_ZH:
                return l;
            case XH_YL_ZL:
            case XH_YH_ZL:
            case XH_YL_ZH:
            case XH_YH_ZH:
                return h;
        }
        throw new IllegalStateException();
    }

    public double splitY (double l, double h) {
        switch (this) {
            case XL_YL_ZL:
            case XL_YL_ZH:
            case XH_YL_ZL:
            case XH_YL_ZH:
                return l;
            case XL_YH_ZL:
            case XL_YH_ZH:
            case XH_YH_ZL:
            case XH_YH_ZH:
                return h;
        }
        throw new IllegalStateException();
    }

    public double splitZ (double l, double h) {
        switch (this) {
            case XL_YL_ZL:
            case XH_YL_ZL:
            case XL_YH_ZL:
            case XH_YH_ZL:
                return l;
            case XL_YL_ZH:
            case XH_YL_ZH:
            case XL_YH_ZH:
            case XH_YH_ZH:
                return h;
        }
        throw new IllegalStateException();
    }

}
