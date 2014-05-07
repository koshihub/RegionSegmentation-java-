import ImageUtility.ColorConverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;

import static ImageUtility.Util.*;

/**
 * Created by kou on 2014/05/07.
 */
public class Region {
    static int regionCounter = 0;
    static int canvas_width = -1, canvas_height = -1;

    HashSet<Integer> pixels = new HashSet<Integer>();
    int left, right, top, bottom;
    BufferedImage image;
    int color;

    public Region() throws IllegalStateException {
        if (canvas_width < 0 || canvas_height < 0) {
            throw new IllegalStateException
                    ("Canvas size has not set appropriately.");
        }

        // initialize position values
        this.left = canvas_width + 1;
        this.top = canvas_height + 1;
        this.right = -1;
        this.bottom = -1;

        this.color = ColorConverter.RGB.rgb(
                (int) (Math.random() * 255),
                (int) (Math.random() * 255),
                (int) (Math.random() * 255)
        );
    }

    public static void setCanvasSize(int w, int h) {
        canvas_width = w;
        canvas_height = h;
    }

    public static Region doDilationOperation(Region original, int r) {
        Region region = new Region();

        /*
         * Create a relative distance list of a radius 'r' circle
         */
        ArrayList<Point> rdlist = new ArrayList<Point>();
        for (int x=-r; x<=r; x++) {
            for (int y=-r; y<=r; y++) {
                if (x * x + y * y <= r * r) {
                    rdlist.add(new Point(x, y));
                }
            }
        }

        /*
         * Dilation operation
         */
        int x_from = Math.max(original.left - r, 0);
        int x_to   = Math.min(original.right + r, canvas_width - 1);
        int y_from = Math.max(original.top - r, 0);
        int y_to   = Math.min(original.bottom - r, canvas_height - 1);
        for (int x = x_from; x < x_to; x++) {
            for (int y = y_from; y < y_to; y++) {
                boolean flag = false;

                // check if there is a region pixel around (x, y)
                for(Point p : rdlist) {
                    if (original.contains(x + p.x, y + p.y )) {
                        flag = true;
                        break;
                    }
                }

                // point (x, y) is added by dilation operation
                if (flag) {
                    region.addPixel(x, y);
                }
            }
        }

        return region;
    }

    public void addPixel(int x, int y) {

        // set position values
        if (x < this.left) this.left = x;
        if (x > this.right) this.right = x;
        if (y < this.top) this.top = y;
        if (y > this.bottom) this.bottom = y;

        // add pixel
        this.pixels.add(ID(x, y));
    }

    public boolean contains(int x, int y) {
        return pixels.contains(ID(x, y));
    }

    public void drawImage(Graphics g) {
        if (image == null) {
            image = new BufferedImage(
                    right - left + 1,
                    bottom - top + 1,
                    BufferedImage.TYPE_INT_ARGB);
            for (int id : pixels) {
                Point p = XY(id);
                image.setRGB(p.x, p.y, this.color);
            }
        }

        g.drawImage(image, left, top, null);
    }
}
