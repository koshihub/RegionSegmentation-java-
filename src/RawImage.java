import static ImageUtility.ColorConverter.*;

import java.awt.*;
import java.awt.image.*;

/**
 * Created by kou on 2014/05/03.
 */
public class RawImage {

    private int width, height;
    private BufferedImage buffer;
    private int[] raw_lab, raw_rgb;

    public RawImage(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public RawImage(int width, int height, int[] rgb, int[] lab) {
        this(width, height);

        if (rgb != null) {
            this.raw_rgb = rgb;
            createLabData();
        }
        else if (lab != null) {
            this.raw_lab = lab;
            createRGBData();
        }
    }

    /**
     * Create RawImage by BufferedImage
     * @param image BufferedImage whose type is TYPE_INT_RGB
     */
    public RawImage(BufferedImage image) {
        this.buffer = image;
        this.width = image.getWidth();
        this.height = image.getHeight();

        this.raw_rgb = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        createLabData();
    }

    public int[] getLabData() {
        return raw_lab;
    }

    public int[] getRGBData() {
        return raw_rgb;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public BufferedImage getImage() {
        if (buffer == null) {
            createBufferedImage();
        }
        return buffer;
    }

    private void createLabData() {
        if (raw_lab == null) {
            raw_lab = new int[width * height];
        }

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                raw_lab[j * width + i] = rgb_to_lab(raw_rgb[j * width + i]);
            }
        }
    }

    private void createRGBData() {
        if (raw_rgb == null) {
            raw_rgb = new int[width * height];
        }

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                raw_rgb[j*width + i] = lab_to_rgb(raw_lab[j * width + i]);
            }
        }
    }

    private void createBufferedImage() {
        int[] bitMasks = new int[]{0xFF0000, 0xFF00, 0xFF, 0xFF000000};
        SinglePixelPackedSampleModel sm = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, width, height, bitMasks);
        DataBufferInt db = new DataBufferInt(raw_rgb, raw_rgb.length);
        WritableRaster wr = Raster.createWritableRaster(sm, db, new Point());
        this.buffer = new BufferedImage(ColorModel.getRGBdefault(), wr, false, null);
    }
}
