package ImageUtility;

/**
 * Created by kou on 2014/05/03.
 */

public class ColorConverter {

    public static class RGB {
        public static int r(int c) { return c>>16&0xff; }
        public static int g(int c) { return c>>8&0xff; }
        public static int b(int c) { return c&0xff; }
        public static int rgb(int r,int g,int b) { return 0xff000000 | r <<16 | g <<8 | b; }
        public static double distance(int c1, int c2) {
            return Math.sqrt((r(c1)-r(c2))*(r(c1)-r(c2)) + (g(c1)-g(c2))*(g(c1)-g(c2)) + (b(c1)-b(c2))*(b(c1)-b(c2)));
        }
        public static int luminance(int c) {
            return (int)(0.298912 * RGB.r(c) + 0.586611 * RGB.g(c) + 0.114478 * RGB.b(c));
        }
    }
    public static class Lab {
        /* L : 0 ~ 100
         * a : 0 ~ 255
         * b : 0 ~ 255
         */
        public static int l(int c) { return c>>16&0xff; }
        public static int a(int c) { return c>>8&0xff; }
        public static int b(int c) { return c&0xff; }
        public static int lab(int l,int a,int b) { return 0xff000000 | l <<16 | a <<8 | b; }
        public static double distance(int c1, int c2) {
            return Math.sqrt((l(c1)-l(c2))*(l(c1)-l(c2)) + (a(c1)-a(c2))*(a(c1)-a(c2)) + (b(c1)-b(c2))*(b(c1)-b(c2)));
        }
        public static double distance_not_sqrt(int c1, int c2) {
            return (l(c1)-l(c2))*(l(c1)-l(c2)) + (a(c1)-a(c2))*(a(c1)-a(c2)) + (b(c1)-b(c2))*(b(c1)-b(c2));
        }
    }

    public static class WhiteReference {
        public static double X = 95.047;
        public static double Y = 100.000;
        public static double Z = 108.883;
    }
    public static double Epsilon = 0.008856;
    public static double Kappa = 903.3;

    public static int lab_to_rgb(int lab) {
        double l = Lab.l(lab), a = Lab.a(lab)-128, b = Lab.b(lab)-128;
        double x, y, z, x3, z3, _r, _g, _b;

        // lab to xyz
        y = (l + 16d) / 116d;
        x = a / 500d + y;
        z = y - b / 200d;
        x3 = x * x * x;
        z3 = z * z * z;

        x = ( x3 > Epsilon ? x3 : (x-16d/116d)/7.787 ) * WhiteReference.X;
        y = ( l > Kappa*Epsilon ? Math.pow((l+16d)/116d, 3d) : l/Kappa ) * WhiteReference.Y;
        z = ( z3 > Epsilon ? z3 : (z-16d/116d)/7.787 ) * WhiteReference.Z;

        // xyz to rgb
        x /= 100d;
        y /= 100d;
        z /= 100d;
        _r = x * 3.240970 + y * -1.537383 + z * -0.498611;
        _g = x * -0.969244 + y * 1.875968 + z * 0.041555;
        _b = x * 0.055630 + y * -0.203977 + z * 1.056972;

        _r = ( _r > 0.0031308 ? 1.055*Math.pow(_r, 1d/2.4)-0.055 : 12.92*_r ) * 255d;
        _g = ( _g > 0.0031308 ? 1.055*Math.pow(_g, 1d/2.4)-0.055 : 12.92*_g ) * 255d;
        _b = ( _b > 0.0031308 ? 1.055*Math.pow(_b, 1d/2.4)-0.055 : 12.92*_b ) * 255d;

        if( _r < 0d ) { _r = 0d; } else if( _r > 255d ) _r = 255d;
        if( _g < 0d ) { _g = 0d; } else if( _g > 255d ) _g = 255d;
        if( _b < 0d ) { _b = 0d; } else if( _b > 255d ) _b = 255d;

        return RGB.rgb((int)_r, (int)_g, (int)_b);
    }

    public static int rgb_to_lab(int rgb) {
        double r = RGB.r(rgb)/255d, g = RGB.g(rgb)/255d, b = RGB.b(rgb)/255d;
        double x, y, z;

        // rgb to xyz
        r = ( r > 0.04045 ? Math.pow((r+0.055)/1.055, 2.4) : r/12.92 ) * 100d;
        g = ( g > 0.04045 ? Math.pow((g+0.055)/1.055, 2.4) : g/12.92 ) * 100d;
        b = ( b > 0.04045 ? Math.pow((b+0.055)/1.055, 2.4) : b/12.92 ) * 100d;

        x = (r * 0.412391 + g * 0.357584 + b * 0.180481) / WhiteReference.X;
        y = (r * 0.212639 + g * 0.715169 + b * 0.072192) / WhiteReference.Y;
        z = (r * 0.019331 + g * 0.119195 + b * 0.950532) / WhiteReference.Z;

        // xyz to lab
        x = ( x > Epsilon ? Math.pow(x, 1d/3d) : (Kappa*x+16d)/116d );
        y = ( y > Epsilon ? Math.pow(y, 1d/3d) : (Kappa*y+16d)/116d );
        z = ( z > Epsilon ? Math.pow(z, 1d/3d) : (Kappa*z+16d)/116d );

        return Lab.lab(
                (int)Math.round(Math.max(0, (116d*y-16d))),
                (int)Math.round(500d * (x - y)) + 128,
                (int)Math.round(200d * (y - z)) + 128);
    }
}
