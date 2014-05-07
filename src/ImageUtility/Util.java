package ImageUtility;

import java.awt.*;

/**
 * Created by kou on 2014/05/07.
 */
public class Util {
    final public static int ID(int x, int y) {
        return x << 16 | y;
    }

    final public static Point XY(int id) {
        return new Point(id >> 16, id & 0xffff);
    }
}
