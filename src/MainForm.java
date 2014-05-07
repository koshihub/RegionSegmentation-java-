import javax.swing.*;
import java.awt.*;

/**
 * Created by kou on 2014/05/02.
 */
public class MainForm extends JFrame {
    private JPanel rootPanel;
    private CanvasPanel canvas;

    public MainForm(int w, int h) {
        super("Region Segmentation");
        setContentPane(rootPanel);

        setMinimumSize(new Dimension(200, 200));
        setSize(w, h);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }
}
