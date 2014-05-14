package ImageUtility;

import java.awt.*;
import java.util.HashSet;
import java.util.Stack;

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

    /**
     * Flood filling class
     */
    public abstract static class FloodFill {

        int width, height;

        public FloodFill(int canvas_width, int canvas_height) {
            this.width = canvas_width;
            this.height = canvas_height;
        }

        /**
         * Define if point(x, y) is a wall or not
         * @param x X value
         * @param y Y value
         * @return true if point(x, y) is a wall
         */
        abstract public boolean isWall(int x, int y);

        /**
         * Define an extra operation for filled pixels
         * @param x X value
         * @param y Y value
         * @return
         */
        public void doOperation(int x, int y) {};

        public HashSet<Integer> execute(Point origin) {
            HashSet<Integer> filled = new HashSet<Integer>();
            Stack<Point> stack = new Stack<Point>();
            stack.push(origin);

            while( !stack.empty() ) {
                Point p = stack.pop();

                // already filled pixel
                if( filled.contains(ID(p.x, p.y)) ) continue;

                // search for the left and the right terminal
                int left, right;
                for (left=p.x; left>0; left--) {
                    if( isWall(left-1, p.y) ) {
                        break;
                    }
                }
                for (right=p.x; right<width-1; right++) {
                    if( isWall(right+1, p.y) ) {
                        break;
                    }
                }

                // fill from left to right
                for(int x=left; x<=right; x++) {
                    filled.add(ID(x, p.y));

                    // do user-defined operation
                    doOperation(x, p.y);
                }

                // scan new points
                for(int d=-1; d<=1; d+=2) {
                    boolean cont = false;
                    int y = p.y + d;
                    if( y >= 0 && y < height ) {
                        for(int x=left; x<=right; x++) {
                            if( cont && isWall(x, y) ) {
                                stack.push(new Point(x-1, y));
                                cont = false;
                            }
                            else if( !isWall(x, y) ) {
                                if( x == right ) {
                                    stack.push(new Point(x, y));
                                } else {
                                    cont = true;
                                }
                            }
                        }
                    }
                }
            }

            return filled;
        }
    }
}
