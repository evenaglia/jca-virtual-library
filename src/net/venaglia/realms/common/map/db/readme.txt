All DB files are in a directory like "world-04x09", where "04x09" is the world identity

All DB file names end in ".data".

All DB files begin with a text preamble, followed by 0x1A. All data in the DB file is offset from the byte following this 0x1A byte.

Files:
"acre.data" contains, for each acre:
    graph relationships to all neighboring acres (max 6)
    graph relationships to all connecting pockets (max 3)
    a rotation/translation matrix to convert this acre to local coordinates
    a list of points, in local coordinates, that define the boundaries of the acre
    low resolution topographical data (20-24 points)
    a reference to medium and high resolution topographical data in "geo.data"
"geo.data" contains:
    medium resolution data at 1 sample per 5 meters, and high at 1 per meter
    height map values representing topographical data
    colors and color maps, as applied to areas of the surface at 10x the sample rate
    water level, if above surface level
    cavern portals
    mineral composition and layering
    1 sample per 10 meters area, per 0.5 meters depth up to 10 meters, per 10 meters up to 1km depth

"pocket.data" contains, for each pocket:
    graph relationships to all neighboring subterranean pockets (max 6)
    graph relationships to all connecting acres (max 3)
    a list of x,y,z bounds, in local coordinates, that define the boundaries of the pocket
    a reference to medium and high resolution interior models in "interior.data"
    a rotation/translation matrix to convert this pocket to local coordinates
    ... more to come ...
"interior.data" contains:
    interior surface data
