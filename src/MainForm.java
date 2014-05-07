import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Created by kou on 2014/05/02.
 */
public class MainForm extends JFrame implements Segmentation.Passive, ActionListener {
    private JPanel rootPanel;
    private JButton button_original;
    private JButton button_bilateral;
    private JButton button_edgeenhanced;
    private JButton button_threshold;
    private JButton button_segmented;
    private CanvasPanel canvas;

    private RawImage imageOrigianl;

    private Segmentation segmentation;
    private Segmentation.Status segmentationStatus = Segmentation.Status.ORIGINAL;

    public MainForm(int w, int h) {
        super("Region Segmentation");
        setContentPane(rootPanel);

        setMinimumSize(new Dimension(200, 200));
        setSize(w, h);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent mouseEvent) {
                        openImage();
                    }
                }
        );

        button_original.addActionListener(this);
        button_bilateral.addActionListener(this);
        button_edgeenhanced.addActionListener(this);
        button_threshold.addActionListener(this);
        button_segmented.addActionListener(this);
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
            showImageOnCanvas();
            enableButton(Segmentation.Status.ORIGINAL);
        }
    }

    private void showImageOnCanvas() {
        if( canvas != null ) {
            canvas.setImage(this.segmentation.getImage(this.segmentationStatus));
            canvas.repaint();
        }
    }

    private void enableButton(Segmentation.Status status) {
        Icon icon = new ImageIcon(segmentation.getImage(status).getScaledInstance(-1, 90, Image.SCALE_SMOOTH));
        JButton target = button_original;

        switch (status) {
            case ORIGINAL:
                target = button_original;
                break;
            case BILATERAL:
                target = button_bilateral;
                break;
            case EDGEENHANCED:
                target = button_edgeenhanced;
                break;
            case THRESHOLD:
                target = button_threshold;
                break;
            case SEGMENTED:
                target = button_segmented;
                break;
        }

        target.setEnabled(true);
        target.setIcon(icon);
    }

    @Override
    public void call(Segmentation.Status st) {
        this.segmentationStatus = st;
        showImageOnCanvas();
        enableButton(st);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JButton source = (JButton)e.getSource();
        if (source == button_original) {
            this.segmentationStatus = Segmentation.Status.ORIGINAL;
        }
        else if( source == button_bilateral) {
            this.segmentationStatus = Segmentation.Status.BILATERAL;
        }
        else if( source == button_edgeenhanced) {
            this.segmentationStatus = Segmentation.Status.EDGEENHANCED;
        }
        else if( source == button_threshold) {
            this.segmentationStatus = Segmentation.Status.THRESHOLD;
        }
        else if( source == button_segmented) {
            this.segmentationStatus = Segmentation.Status.SEGMENTED;
        }
        showImageOnCanvas();
    }
}
