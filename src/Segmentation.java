import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

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

    private Region border;
    private ArrayList<Region> regions = new ArrayList<Region>();

    public Segmentation(Passive passive, RawImage imageOriginal) {
        this.currentStatus = Status.ORIGINAL;
        this.passive = passive;
        this.imageOriginal = imageOriginal;
        this.width = imageOriginal.getWidth();
        this.height = imageOriginal.getHeight();

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
                return temp;
        }

        return null;
    }

    /**
     * Create a border region
     * @param data Edge enhanced image data (gray scale)
     * @return A border region
     */
    private Region createBorderRegion(int[] data) {
        Region region = new Region();
        for(int x=0; x<width; x++) {
            for(int y=0; y<height; y++) {
                int index = y * width + x;
                /*
                 * check only one value since
                 * 'data' is gray scale format
                 */
                if( RGB.r(data[index]) > 100 ) {
                    region.addPixel(x, y);
                }
            }
        }
        return region;
    }

    private ArrayList<Region> growSeedRegions(final Stack<HashSet<Integer>> seeds, final HashSet<Integer> remain) {
        ArrayList<Region> result = new ArrayList<Region>();

        /*
         * Create hash table of regions.
         */
        final HashSet<Integer>[][] table = new HashSet[width][height];
        for (HashSet<Integer> seed : seeds) {
            for (int id : seed) {
                Point p = XY(id);
                table[p.x][p.y] = seed;
            }
        }

        while (!seeds.isEmpty()) {
            /*
             * The stack of connected regions.
             */
            final Stack<HashSet<Integer>> connected = new Stack<HashSet<Integer>>();
            connected.push(seeds.pop());

            /*
             * Continue connecting new regions until connected regions become empty.
             */
            HashSet<Integer> grown = null;
            while (!connected.isEmpty()) {
                final HashSet<Integer> neighbor = connected.pop();

                // Get the average color of a neighbor region.
                final int averageColor;
                final int[] labData = imageBilateral.getLabData();
                int sum_l = 0, sum_a = 0, sum_b = 0;
                for (int id : neighbor) {
                    Point p = XY(id);
                    int lab = labData[p.y * width + p.x];
                    sum_l += Lab.l(lab);
                    sum_a += Lab.a(lab);
                    sum_b += Lab.b(lab);
                }
                averageColor = Lab.lab(
                        Math.round(sum_l / neighbor.size()),
                        Math.round(sum_a / neighbor.size()),
                        Math.round(sum_b / neighbor.size())
                );

                // Grow the neighbor region.
                HashSet<Integer> grownNeighbor = (
                        new Util.FloodFill(width, height) {
                            @Override
                            public boolean isWall(int x, int y) {
                                if ( !remain.contains(ID(x,y)) ||
                                    (!neighbor.contains(ID(x, y)) &&
                                        (border.contains(x, y) ||
                                         Lab.distance(labData[y * width + x], averageColor) >= 10))
                                ) {
                                    return true;
                                } else {
                                    // Reached to neighbor region
                                    if (seeds.contains(table[x][y])) {
                                        connected.push(table[x][y]);
                                        seeds.remove(table[x][y]);
                                        return true;
                                    }

                                    return false;
                                }
                            }
                        }
                ).execute(XY(neighbor.iterator().next()));

                // Append
                if (grown == null) {
                    grown = grownNeighbor;
                }
                else {
                    grown.addAll(grownNeighbor);
                }
            }

            result.add(new Region(grown));
        }

        return result;
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

        // Hash table of regions
        HashSet<Integer>[][] regionTable = new HashSet[width][height];

        // Iterate loop while decreasing the radius of the trapped ball.
        Region prevDilatedBorder = new Region();
        for (int ballR = 5; ballR >= 0; ballR--) {
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
            Stack<HashSet<Integer>> seeds = new Stack<HashSet<Integer>>();
            while (!remain.isEmpty()) {
                Point origin = XY(remain.iterator().next());

                // Get seed region by flood filling
                final HashSet<Integer> remainPixel = allPixel;
                HashSet<Integer> newSeed = (
                        new Util.FloodFill(width, height) {
                            @Override
                            public boolean isWall(int x, int y) {
                                if (dilatedBorder.contains(x, y) || !remainPixel.contains((ID(x,y)))) {
                                    return true;
                                }
                                else {
                                    return false;
                                }
                            }
                        }
                ).execute(origin);

                // Remove seed pixels from 'remain'
                remain.removeAll(newSeed);

                // Add to stack
                seeds.push(newSeed);
            }

            /*
             * Grow all seed regions, and connect neighbor regions.
             * Append regions to 'regions' and remove pixels from all pixel.
             */
            ArrayList<Region> grownRegions = growSeedRegions(seeds, allPixel);
            regions.addAll(grownRegions);
            for (Region r : grownRegions) {
                allPixel.removeAll(r.getPixels());
            }

            System.out.println("BallR: " + ballR);
            System.out.println("Appended regions: " + grownRegions.size());
            break;
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
