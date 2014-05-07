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
public class CanvasPanel extends JPanel implements Segmentation.Passive {
    private int width, height;
    private BufferedImage buffer;

    private RawImage imageOrigianl;

    private Segmentation segmentation;
    private Segmentation.Status segmentationStatus;

    public CanvasPanel() {
        addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent mouseEvent) {
                        openImage();
                    }
                }
        );
        addComponentListener(
                new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent componentEvent) {
                        resize();
                    }
                }
        );
    }

    public void openImage() {
        JFileChooser chooser = new JFileChooser(new File(".").getAbsolutePath() + "/images");
        if( chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION ) {
            File file = chooser.getSelectedFile();

            try {
                /*
                 * Load image logic.
                 * ImageIO.read returns a BufferedImage of TYPE_BYTE_BINARY,
                 * so here we convert the type into TYPE_INT_RGB for convenience.
                 */
                BufferedImage byteImage = ImageIO.read(file);
                BufferedImage intImage = new BufferedImage(
                        byteImage.getWidth(),
                        byteImage.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                intImage.getGraphics().drawImage(byteImage, 0, 0, null);

                imageOrigianl = new RawImage(intImage);
            } catch (Exception e) {
                System.out.println("Load Image Error");
                e.printStackTrace();
                return;
            }

            /*
             *  Start region segmentation
             */
            segmentation = new Segmentation(this, imageOrigianl);
            segmentation.execute();
            repaint();
        }
    }

    public void resize() {
        this.width = getWidth();
        this.height = getHeight();

        buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        repaint();
    }

    public void paint(Graphics _g) {
        if( buffer != null ) {
            Graphics g = buffer.getGraphics();

            g.clearRect(0, 0, width, height);

            if( segmentation != null ) {
                g.drawImage(segmentation.getCurrentImage(), 0, 0, null);
            }

            _g.drawImage(buffer, 0, 0, null);
        }
    }

    @Override
    public void call(Segmentation.Status st) {
        this.segmentationStatus = st;
        repaint();
    }
}
