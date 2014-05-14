import java.util.HashSet;

/**
 * Created by kou on 2014/05/14.
 */
public class Border {

    HashSet<Integer> pixels;
    int width, height;

    public Border(int width, int height, HashSet<Integer> pixels) {
        this.width = width;
        this.height = height;
        this.pixels = pixels;
        System.out.println(pixels.size());
    }
}
