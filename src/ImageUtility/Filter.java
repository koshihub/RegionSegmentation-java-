package ImageUtility;

import java.awt.*;
import java.util.ArrayList;

import static ImageUtility.ColorConverter.*;

/**
 * Created by kou on 2014/05/03.
 */
public class Filter {

    /**
     * Apply a bilateral filter
     * (Lab) -> (Lab)
     */
    public static int[] bilateralFilter(int[] data, int w, int h) {
        int[] newData = new int[w * h];

        // apply filter
        int kernel = 3;            // kernel size
        double s1 = 100, s2 = 10;    // sigma values
        double i2ss1 = 0.5 / s1 / s1, i2ss2 = 0.5 / s2 / s2;

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                double deno_sum = 0d;
                double nume_sum_l = 0d, nume_sum_a = 0d, nume_sum_b = 0d;

                for (int i = -kernel; i <= kernel; i++) {
                    for (int j = -kernel; j <= kernel; j++) {
                        int xx = x + i, yy = y + j;
                        if (xx >= 0 && xx < w && yy >= 0 && yy < h) {
                            int c = data[yy * w + xx];
                            double ii = i * i, jj = j * j;
                            double S = Math.exp(-(ii + jj) * i2ss1) * Math.exp(-Lab.distance_not_sqrt(data[y * w + x], c) * i2ss2);
                            deno_sum += S;
                            nume_sum_l += Lab.l(c) * S;
                            nume_sum_a += Lab.a(c) * S;
                            nume_sum_b += Lab.b(c) * S;
                        }
                    }
                }

                newData[y * w + x] = Lab.lab(
                                (int) (nume_sum_l / deno_sum),
                                (int) (nume_sum_a / deno_sum),
                                (int) (nume_sum_b / deno_sum));
            }
        }

        return newData;
    }

    /**
     * Apply a laplacian of gaussian filter
     * (RGB) -> (RGB)
     */
    public static int[] LoGFilter(int[] data, int w, int h) {
        int[] newData = new int[w * h];

        // apply filter
        int kernel_size = 3;        // kernel size
        double s = 1.1;                // sigma value
        double i2ss = 0.5 / s / s, issss = 1d / s / s / s / s;

        double[][] kernel = new double[kernel_size * 2 + 1][kernel_size * 2 + 1];
        for (int i = -kernel_size; i <= kernel_size; i++) {
            for (int j = -kernel_size; j <= kernel_size; j++) {
                double ss = (i * i + j * j) * i2ss;
                kernel[i + kernel_size][j + kernel_size] = (ss - 1d) * Math.exp(-ss) * issss / Math.PI;
            }
        }

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                double sum = 0d;
                for (int i = -kernel_size; i <= kernel_size; i++) {
                    for (int j = -kernel_size; j <= kernel_size; j++) {
                        int xx = x + i, yy = y + j;
                        if (xx >= 0 && xx < w && yy >= 0 && yy < h) {
                            sum += kernel[i + kernel_size][j + kernel_size] * RGB.luminance(data[yy * w + xx]);
                        }
                    }
                }

                newData[y * w + x] = (int) sum;
            }
        }

        return newData;
    }

    /**
     * Apply edge enhancing filter
     * (Lab) -> (RGB Grayscale)
     */
    public static int[] edgeEnhanceFilter(int[] data, int[] LoGdata, int w, int h) {
        int[] newData = new int[w * h];

        int radius = 3;
        double g = Math.PI * radius * radius / 2d;

        // relative distance list of radius r
        ArrayList<Point> rdists = new ArrayList<Point>();
        for (int rx = -radius; rx <= radius; rx++) {
            for (int ry = -radius; ry <= radius; ry++) {
                if (rx * rx + ry * ry <= (radius + 0.5) * (radius + 0.5)) {
                    // within the circle
                    rdists.add(new Point(rx, ry));
                }
            }
        }

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int index = y * w + x;

                // calculate mthres
                double mthres = 0;
                for (Point p : rdists) {
                    int xx = x + p.x, yy = y + p.y;
                    if (xx >= 0 && xx < w && yy >= 0 && yy < h) {
                        double sechx = Lab.distance(data[index], data[(y + p.y) * w + (x + p.x)]) / 8d;
                        mthres += Math.pow(2d / (Math.exp(sechx) + Math.exp(-sechx)), 5d);
                    }
                }

                if (LoGdata[index] >= -3) {
                    mthres /= rdists.size();
                    mthres = Math.pow(mthres, 3d);
                    mthres *= rdists.size();
                }

                int val = 0;
                if (mthres <= g) {
                    val = (int) ((g - mthres) / g * 255d);
                }

                newData[index] = RGB.rgb(val, val, val);
            }
        }
        return newData;
    }

}
