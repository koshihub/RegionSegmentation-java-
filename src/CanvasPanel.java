import ImageUtility.ColorConverter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Created by kou on 2014/05/03.
 */
public class CanvasPanel extends JPanel {
    private int width, height;
    private BufferedImage buffer;
    private BufferedImage image = null;

    public CanvasPanel() {
        addComponentListener(
                new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent componentEvent) {
                        resize();
                    }
                }
        );
    }

    private void resize() {
        this.width = getWidth();
        this.height = getHeight();

        buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        repaint();
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    @Override
    public void paint(Graphics _g) {
        if( buffer != null ) {
            Graphics g = buffer.getGraphics();

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);

            if( image != null ) {
                g.drawImage(image, 0, 0, null);
            }

            _g.drawImage(buffer, 0, 0, null);
        }
    }

}
