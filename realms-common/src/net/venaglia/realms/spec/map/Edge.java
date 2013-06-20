package net.venaglia.realms.spec.map;

/**
* User: ed
* Date: 12/1/12
* Time: 10:21 AM
*/
public enum Edge {
    AB, BC, CA;

    public Edge touches(boolean inverted) {
        switch (this) {
            case AB:
                return inverted ? BC : CA;
            case BC:
                return inverted ? CA : AB;
            case CA:
                return inverted ? AB : BC;
        }
        return null;
    }
}
