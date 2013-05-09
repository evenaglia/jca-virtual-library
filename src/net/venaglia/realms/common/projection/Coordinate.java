package net.venaglia.realms.common.projection;

import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.texture.TextureCoordinate;

/**
 * User: ed
 * Date: 3/8/13
 * Time: 8:08 AM
 */
public final class Coordinate {

    private final Point vertex;
    private final Vector normal;
    private final Color color;
    private final TextureCoordinate textureCoordinate;

    public Coordinate(Point vertex,
                      Vector normal,
                      Color color,
                      TextureCoordinate textureCoordinate) {
        this.vertex = vertex;
        this.normal = normal;
        this.color = color;
        this.textureCoordinate = textureCoordinate;
        if (vertex == null) {
            throw new NullPointerException("vertex");
        }
     }

    public Coordinate(Point vertex, Vector normal, Color color) {
        this(vertex, normal, color, null);
    }

    public Coordinate(Point vertex, Vector normal) {
        this(vertex, normal, null, null);
    }

    public Point getVertex() {
        return vertex;
    }

    public Vector getNormal() {
        return normal;
    }

    public Color getColor() {
        return color;
    }

    public TextureCoordinate getTextureCoordinate() {
        return textureCoordinate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Coordinate that = (Coordinate)o;

        if (color != null ? !color.equals(that.color) : that.color != null) return false;
        if (normal != null ? !normal.equals(that.normal) : that.normal != null) return false;
        if (textureCoordinate != null
            ? !textureCoordinate.equals(that.textureCoordinate)
            : that.textureCoordinate != null)
            return false;
        if (!vertex.equals(that.vertex)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = vertex.hashCode();
        result = 31 * result + (normal != null ? normal.hashCode() : 0);
        result = 31 * result + (color != null ? color.hashCode() : 0);
        result = 31 * result + (textureCoordinate != null ? textureCoordinate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Coordinate{vertex=").append(vertex);
        if (normal != null) {
            buffer.append(",normal=").append(normal);
        }
        if (color != null) {
            buffer.append(",color=").append(color);
        }
        if (textureCoordinate != null) {
            buffer.append(",textureCoordinate=").append(textureCoordinate);
        }
        buffer.append("}");
        return buffer.toString();
    }
}
