import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
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

    private Region extractSegmentedRegion(Point origin, final Region dilatedBorder) {

        /*
         * Get a rough region by flood filling.
         */
        final HashSet<Integer> rough = (
                new Util.FloodFill(width, height) {
                    @Override
                    public boolean isWall(int x, int y) {
                        return dilatedBorder.contains(x, y);
                    }
                }
        ).execute(origin);

        /*
         * Get the average color of a new segmented region.
         */
        final int averageColor;
        final int[] labData = imageBilateral.getLabData();
        int sum_l = 0, sum_a = 0, sum_b = 0;
        for (int id : rough) {
            Point p = XY(id);
            int lab = labData[p.y * width + p.x];
            sum_l += Lab.l(lab);
            sum_a += Lab.a(lab);
            sum_b += Lab.b(lab);
        }
        averageColor = Lab.lab(
                Math.round(sum_l / rough.size()),
                Math.round(sum_a / rough.size()),
                Math.round(sum_b / rough.size())
        );

        /*
         * Get a grown region by flood filling.
         * The edge of a large color difference from average color is regarded as a wall.
         */
        HashSet<Integer> grown = (
                new Util.FloodFill(width, height) {
                    @Override
                    public boolean isWall(int x, int y) {
                        if (!rough.contains(ID(x, y)) &&
                                (border.contains(x, y) || Lab.distance(labData[y * width + x], averageColor) >= 8)) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
        ).execute(origin);

        return new Region(grown);
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
         * Iterate loop while decreasing the radius of the trapped ball.
         */
        HashSet<Integer> allPixel = new HashSet<Integer>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                allPixel.add(ID(x, y));
            }
        }
        for (int ballR = 8; ballR >= 1; ballR--) {
            HashSet<Integer> remain = new HashSet<Integer>(allPixel);

            /*
             * Do dilation operation to the border,
             * and remove the dilated border from 'remain' pixels
             */
            Region dilatedBorder = Region.doDilationOperation(border, ballR);
            remain.removeAll(dilatedBorder.getPixels());

            /*
             * Extract new regions until 'remain' becomes empty
             */
            while (!remain.isEmpty()) {
                Point origin = XY(remain.iterator().next());

                /*
                 * Extract region and remove the region pixels from 'remain'
                 */
                Region region = extractSegmentedRegion(origin, dilatedBorder);
                remain.removeAll(region.getPixels());
                regions.add(region);
            }
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
