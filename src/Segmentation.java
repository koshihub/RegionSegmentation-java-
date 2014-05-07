import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.List;

import ImageUtility.ColorConverter.*;
import ImageUtility.Filter;

/**
 * Created by kou on 2014/05/03.
 */
public class Segmentation extends SwingWorker<Integer, Segmentation.Status> {
    public static enum Status {
        ORIGINAL, BILATERAL, EDGEENHANCED, THRESHOLD,
    }

    public interface Passive {
        public void call(Status st);
    }

    private Passive passive;
    private Status currentStatus;

    private int width, height;
    private RawImage imageOriginal;
    private RawImage imageBilateral;
    private RawImage imageEdgeEnhanced;

    private Region border;


    public Segmentation(Passive passive, RawImage imageOrigianl) {
        this.currentStatus = Status.ORIGINAL;
        this.passive = passive;
        this.imageOriginal = imageOrigianl;
        this.width = imageOrigianl.getWidth();
        this.height = imageOrigianl.getHeight();

        // initialize Region class
        Region.setCanvasSize(this.width, this.height);
    }

    public BufferedImage getCurrentImage() {
        return getImage(currentStatus);
    }

    public BufferedImage getImage(Status st) {
        switch (st) {
            case ORIGINAL:
                return imageOriginal.getImage();
            case BILATERAL:
                return imageBilateral.getImage();
            case EDGEENHANCED:
                return imageEdgeEnhanced.getImage();
            case THRESHOLD:
                BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                border.drawImage(temp.getGraphics());
                return temp;
        }

        return null;
    }

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

    private void floodFill() {

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

        this.border = createBorderRegion(imageEdgeEnhanced.getRGBData());
        this.border = Region.doDilationOperation(border, 8);
        currentStatus = Status.THRESHOLD;
        publish(currentStatus);

        return 0;
    }

    @Override
    protected void process(List<Status> mes) {
        passive.call(mes.get(mes.size() - 1));
    }
}
