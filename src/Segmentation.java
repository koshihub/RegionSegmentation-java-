import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;

import ImageUtility.ColorConverter.*;
import ImageUtility.Filter;
import ImageUtility.Util;

import static ImageUtility.Util.*;

/**
 * Created by kou on 2014/05/03.
 */
public class Segmentation extends SwingWorker<Integer, Segmentation.Status> {
    public static enum Status {
        ORIGINAL, BILATERAL, EDGEENHANCED, THRESHOLD, SEGMENTED,
    }

    public interface Passive {
        public void call(Status st);
    }


    // a passive instance which will receive a progress of the segmentation
    private Passive passive;

    // current progress
    private Status currentStatus;

    private int width, height;
    private RawImage imageOriginal;
    private RawImage imageBilateral;
    private RawImage imageEdgeEnhanced;

    private Border border;
    private ArrayList<Region> regions = new ArrayList<Region>();
    private ArrayList<HashSet<Integer>> hash_regions = new ArrayList<HashSet<Integer>>();

    // Hash table of region identities
    RegionIdentity[][] regionIdentityTable;

    // Groups of regions which have same ids
    ArrayList<HashSet<RegionIdentity>> sameIdentityGroups = new ArrayList<HashSet<RegionIdentity>>();

    public Segmentation(Passive passive, RawImage imageOriginal) {
        this.currentStatus = Status.ORIGINAL;
        this.passive = passive;
        this.imageOriginal = imageOriginal;
        this.width = imageOriginal.getWidth();
        this.height = imageOriginal.getHeight();
        this.regionIdentityTable = new RegionIdentity[width][height];
        this.sameIdentityGroups = new ArrayList<HashSet<RegionIdentity>>();

        // initialize Region class
        Region.setCanvasSize(this.width, this.height);
    }

    public BufferedImage getCurrentImage() {
        return getImage(currentStatus);
    }

    public BufferedImage getImage(Status st) {
        BufferedImage temp;

        switch (st) {
            case ORIGINAL:
                return imageOriginal.getImage();
            case BILATERAL:
                return imageBilateral.getImage();
            case EDGEENHANCED:
                return imageEdgeEnhanced.getImage();
            case THRESHOLD:
                temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                border.drawImage(temp.getGraphics());
                return temp;
            case SEGMENTED:
                temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                for (Region r : regions) {
                    r.drawImage(temp.getGraphics());
                }
                border.drawImage(temp.getGraphics());
                return temp;
        }

        return null;
    }

    /**
     * Create a border region
     * @param data Edge enhanced image data (gray scale)
     * @return A border region
     */
    private Border createBorderRegion(int[] data) {
        Border region = new Border();
        for(int x=0; x<width; x++) {
            for(int y=0; y<height; y++) {
                int index = y * width + x;
                /*
                 * check only one value since
                 * 'data' is gray scale format
                 */
                if( RGB.r(data[index]) > 120 ) {
                    region.addPixel(x, y);
                }
            }
        }
        return region;
    }

    private void growSeedRegions(final ArrayList<HashSet<Integer>> seeds, final HashSet<Integer> allPixel) {

        for (HashSet<Integer> seed : seeds) {
            /*
             * Continue connecting new regions until connected regions become empty.
             */
            final HashSet<Integer> currentSeed = seed;
            Point originPoint = XY(currentSeed.iterator().next());
            final RegionIdentity currentSeedIdentity = regionIdentityTable[originPoint.x][originPoint.y];

            /*
             * The sets of connected regions.
             */
            final HashSet<RegionIdentity> connectedRegionIdentity = new HashSet<RegionIdentity>();
            connectedRegionIdentity.add(currentSeedIdentity);

            // Get the average color of the current seed region.
            final int[] labData = imageBilateral.getLabData();
            int sum_l = 0, sum_a = 0, sum_b = 0;
            for (int id : currentSeed) {
                Point p = XY(id);
                int lab = labData[p.y * width + p.x];
                sum_l += Lab.l(lab);
                sum_a += Lab.a(lab);
                sum_b += Lab.b(lab);
            }
            final int averageColor = Lab.lab(
                    Math.round(sum_l / currentSeed.size()),
                    Math.round(sum_a / currentSeed.size()),
                    Math.round(sum_b / currentSeed.size())
            );

            /*
             * Grow the current region.
             * If the region reaches other regions, save the regions as connected regions.
             */
            HashSet<Integer> grownSeed = (
                    new FloodFill(width, height) {
                        @Override
                        public boolean isWall(int x, int y) {
                            boolean wall = false;
                            boolean isBorder = border.contains(x, y);
                            boolean colorEdge = (Lab.distance(labData[y * width + x], averageColor) >= 8);

                            RegionIdentity ci = regionIdentityTable[x][y];

                            // within seed region
                            if (currentSeed.contains(ID(x, y))) {
                                wall = false;
                            }
                            // reached to a border
                            else if (isBorder || colorEdge) {
                                if (isBorder) {
                                    border.addNeighborRegion(x, y, currentSeedIdentity);
                                }
                                wall = true;
                            }
                            // reached to already assigned region
                            else if (ci != null && ci.getID() != currentSeedIdentity.getID()) {
                                connectedRegionIdentity.add(ci);
                                wall = true;
                            }

                            return wall;
                        }

                        @Override
                        public void doOperation(int x, int y) {
                            // assign a region identity
                            regionIdentityTable[x][y] = currentSeedIdentity;
                        }
                    }
            ).execute(originPoint);

            // remove grown pixels from allPixel
            allPixel.removeAll(grownSeed);

            // update the group
            boolean flag = false;
            for (HashSet<RegionIdentity> group : sameIdentityGroups) {
                for (RegionIdentity ri : connectedRegionIdentity) {
                    if (group.contains(ri)) {
                        group.addAll(connectedRegionIdentity);
                        flag = true;
                        break;
                    }
                }
            }
            if (!flag) {
                sameIdentityGroups.add(connectedRegionIdentity);
            }
        }
    }

    @Override
    protected Integer doInBackground() {
        /*
         * Apply a bilateral filter to reduce the noises.
         */
        imageBilateral = new RawImage(
                width,
                height,
                null,
                Filter.bilateralFilter(imageOriginal.getLabData(), width, height));
        currentStatus = Status.BILATERAL;
        publish(currentStatus);

        /*
         * Apply a edge enhancing filter.
         */
        imageEdgeEnhanced = new RawImage(
                width,
                height,
                Filter.edgeEnhanceFilter(
                        imageBilateral.getLabData(),
                        Filter.LoGFilter(imageBilateral.getRGBData(), width, height),
                        width,
                        height),
                null);
        currentStatus = Status.EDGEENHANCED;
        publish(currentStatus);

        /*
         * Create a border region by thresholding the edge enhanced image.
         */
        border = createBorderRegion(imageEdgeEnhanced.getRGBData());
        currentStatus = Status.THRESHOLD;
        publish(currentStatus);

        /*
         * Region segmentation.
         */
        // The pixels which have not been assigned to any regions.
        HashSet<Integer> allPixel = new HashSet<Integer>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                allPixel.add(ID(x, y));
            }
        }
        allPixel.removeAll(border.getPixels());

        // Iterate loop while decreasing the radius of the trapped ball.
        Region prevDilatedBorder = new Region();
        for (int ballR = 8; ballR >= 1; ballR--) {
            HashSet<Integer> remain = new HashSet<Integer>(allPixel);

            /*
             * Do dilation operation to the border,
             * and remove the dilated border from 'remain' pixels
             */
            final Region dilatedBorder = Region.doDilationOperation(border, ballR, prevDilatedBorder.getPixels());
            prevDilatedBorder = dilatedBorder;
            remain.removeAll(dilatedBorder.getPixels());

            /*
             * Extract seed regions until 'remain' becomes empty
             */
            ArrayList<HashSet<Integer>> seeds = new ArrayList<HashSet<Integer>>();
            while (!remain.isEmpty()) {
                Point origin = XY(remain.iterator().next());

                // Region identity for a new seed
                final RegionIdentity ri = new RegionIdentity();

                // Get seed region by flood filling
                final HashSet<Integer> finalRemain = remain;
                HashSet<Integer> newSeed = (
                        new Util.FloodFill(width, height) {
                            @Override
                            public boolean isWall(int x, int y) {
                                boolean wall = false;
                                RegionIdentity ci = regionIdentityTable[x][y];

                                if (dilatedBorder.contains(x, y) || !finalRemain.contains(ID(x, y))) { //(ci != null && ci.getID() != ri.getID())
                                    wall = true;
                                }
                                return wall;
                            }

                            @Override
                            public void doOperation(int x, int y) {
                                // assign a region identity
                                regionIdentityTable[x][y] = ri;
                            }
                        }
                ).execute(origin);

                // Remove seed pixels from 'remain'
                remain.removeAll(newSeed);

                // Add to stack
                seeds.add(newSeed);
            }
            for (HashSet<Integer> seed : seeds) {
                Region r = new Region(seed);
                //regions.add(Region.doDilationOperation(r, 8, null));
                //regions.add(r);
            }

            /*
             * Grow all seed regions, and connect neighbor regions.
             * Append regions to 'regions' and remove pixels from all pixel.
             */
            growSeedRegions(seeds, allPixel);
        }

        if ( true ) {
        /*
         * Regard the remaining small pixels as borders
         */
            for (int id : allPixel) {
                Point p = XY(id);
                border.addPixel(p.x, p.y);
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        int xx = p.x+i, yy = p.y+j;
                        if ( xx >= 0 && xx < width && yy >= 0 && yy < height) {
                            if (regionIdentityTable[xx][yy] != null) {
                                border.addNeighborRegion(p.x, p.y, regionIdentityTable[xx][yy]);
                            }
                        }
                    }
                }
            }

        /*
         * Modify the region ids by looking at the sameIdentityGroups
         */
            for (HashSet<RegionIdentity> group : sameIdentityGroups) {
                int id = -1;
                for (RegionIdentity ri : group) {
                    if (id == -1) {
                        id = ri.getID();
                    }
                    else {
                        ri.setID(id);
                    }
                }
            }

        /*
         * Scan all regions
         */
            HashSet<Integer> borderPixels = new HashSet<Integer>();
            HashMap<Integer, Region> mapping = new HashMap<Integer, Region>();
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (regionIdentityTable[x][y] != null) {
                        // Pixel (x, y) is assigned to a region
                        int id = regionIdentityTable[x][y].getID();
                        Region assignedRegion;
                        if (mapping.containsKey(id)) {
                            assignedRegion = mapping.get(id);
                        } else {
                            assignedRegion = new Region();
                            mapping.put(id, assignedRegion);
                        }
                        assignedRegion.addPixel(x, y);

                        // Assign Region to RegionIdentity
                        regionIdentityTable[x][y].assignRegion(assignedRegion);
                    }
                    else {
                        // Pixel (x, y) is a border
                        borderPixels.add(ID(x, y));
                    }
                }
            }

            // Add regions
            regions.addAll(mapping.values());

        }
        currentStatus = Status.SEGMENTED;
        publish(currentStatus);


        return 0;
    }

    @Override
    protected void process(List<Status> mes) {
        for (Status st : mes) {
            passive.call(st);
        }
    }
}
