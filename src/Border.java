import ImageUtility.ColorConverter;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;

import static ImageUtility.Util.*;

/**
 * Created by kou on 2014/05/14.
 */
public class Border extends Region{

    HashMap<Integer, BorderPixel> borderPixels = new HashMap<Integer, BorderPixel>();

    public void addNeighborRegion(int x, int y, RegionIdentity regionIdentity) {
        int id = ID(x, y);
        if (borderPixels.containsKey(id)) {
            borderPixels.get(id).neighborRegions.add(regionIdentity);
        }
    }

    /**
     * Thin the borders considering the necessity of each border pixels
     * @param labData The color map of the original image
     */
    public void doThinning(int[] labData) {

    }

    @Override
    public void addPixel(int x, int y) {
        super.addPixel(x, y);

        borderPixels.put(ID(x, y), new BorderPixel(x, y));
    }

    @Override
    public void drawImage(Graphics g) {
        super.drawImage(g);

        /*
        for (BorderPixel bp : borderPixels.values()) {
            if (!bp.neighborRegions.isEmpty() && bp.x >= left && bp.x <= right && bp.y >= top && bp.y <= bottom) {
                image.setRGB(bp.x, bp.y, ColorConverter.RGB.rgb(0, 0, 0));
            }
        }
        g.drawImage(image, left, top, null);
        */
    }

    public class BorderPixel {

        int x, y;
        HashSet<RegionIdentity> neighborRegions = new HashSet<RegionIdentity>();
        HashSet<BorderPixel> neighborBorderPixels = new HashSet<BorderPixel>();

        public BorderPixel(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
