/**
 * Created by kou on 2014/05/12.
 */
public class RegionIdentity {

    private static int __counter = 0;
    private int id;
    private Region region;

    public RegionIdentity() {
        __counter ++;
        this.id = __counter;
    }

    public void setID(int id) {
        this.id  = id;
    }

    public int getID() {
        return this.id;
    }

    public void assignRegion(Region region) {
        this.region = region;
    }
}
