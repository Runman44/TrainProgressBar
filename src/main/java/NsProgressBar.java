import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.ui.JBColor;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import static javax.imageio.ImageIO.read;

public class NsProgressBar extends BasicProgressBarUI {

    private volatile float velocity = 1.0f;
    private volatile int pos = 0;

    @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "UnusedDeclaration"})
    public static ComponentUI createUI(JComponent c) {
        c.setBorder(JBUI.Borders.empty().asUIResource());
        return new NsProgressBar();
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(super.getPreferredSize(c).width, JBUI.scale(20));
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        progressBar.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                super.componentShown(e);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                super.componentHidden(e);
            }
        });
    }

    @Override
    protected int getBoxLength(int availableLength, int otherDimension) {
        return availableLength;
    }

    @Override
    protected void paintIndeterminate(Graphics g, JComponent component) {
        if (isUnsupported(g, component)) {
            super.paintIndeterminate(g, component);
            return;
        }

        final Graphics2D graphics = (Graphics2D) g;

        final GraphicsConfig config = GraphicsUtil.setupAAPainting(graphics);
        Insets barInsets = progressBar.getInsets(); // area for border
        int w = progressBar.getWidth();
        int h = progressBar.getPreferredSize().height;
        if (!isEven(component.getHeight() - h)) {
            h++;
        }

        int barRectWidth = w - (barInsets.right + barInsets.left);
        int barRectHeight = h - (barInsets.top + barInsets.bottom);

        if (barRectWidth <= 0 || barRectHeight <= 0) {
            return;
        }

        int amountOfProgressBarThatIsFilled = pos;

        fillBackgroundOfProgressBar(graphics, component, w, h);

        final RoundRectangle2D rectangle = getRoundRectangle(w, h);

        drawTypePaint(h, amountOfProgressBarThatIsFilled, graphics, rectangle);
        drawIcon(amountOfProgressBarThatIsFilled, graphics, rectangle, h);
        drawBorder(rectangle, graphics);
        if (progressBar.isStringPainted()) {
            graphics.translate(0, -(component.getHeight() - h) / 2);
            paintString(graphics,
                    barInsets.left,
                    barInsets.top,
                    barRectWidth,
                    barRectHeight,
                    amountOfProgressBarThatIsFilled,
                    barInsets);
        }

        config.restore();
        updatePosition();
    }

    @Override
    protected void paintDeterminate(Graphics g, JComponent component) {
        resetPositionAndVelocity();

        if (isUnsupported(g, component)) {
            super.paintDeterminate(g, component);
            return;
        }

        final Graphics2D graphics = (Graphics2D) g;

        final GraphicsConfig config = GraphicsUtil.setupAAPainting(graphics);
        Insets barInsets = progressBar.getInsets(); // area for border
        int w = progressBar.getWidth();
        int h = progressBar.getPreferredSize().height;
        if (!isEven(component.getHeight() - h)) {
            h++;
        }

        int barRectWidth = w - (barInsets.right + barInsets.left);
        int barRectHeight = h - (barInsets.top + barInsets.bottom);

        if (barRectWidth <= 0 || barRectHeight <= 0) {
            return;
        }

        int amountOfProgressBarThatIsFilled = getAmountFull(barInsets, barRectWidth, barRectHeight);

        fillBackgroundOfProgressBar(graphics, component, w, h);

        final RoundRectangle2D rectangle = getRoundRectangle(w, h);

        drawTypePaint(h, amountOfProgressBarThatIsFilled, graphics, rectangle);
        drawIcon(amountOfProgressBarThatIsFilled, graphics, rectangle, h);
        drawBorder(rectangle, graphics);

        if (progressBar.isStringPainted()) {
            graphics.translate(0, -(component.getHeight() - h) / 2);
            paintString(graphics,
                    barInsets.left,
                    barInsets.top,
                    barRectWidth,
                    barRectHeight,
                    amountOfProgressBarThatIsFilled,
                    barInsets);
        }

        config.restore();
    }

    private void fillBackgroundOfProgressBar(final Graphics graphics, final JComponent component, final int w, final int h) {
        Container parent = component.getParent();
        Color background = parent != null ? parent.getBackground() : UIUtil.getPanelBackground();

        graphics.setColor(background);

        if (component.isOpaque()) {
            graphics.fillRect(0, 0, w, h);
        }
    }

    private boolean isUnsupported(final Graphics g, final JComponent c) {
        return !(g instanceof Graphics2D) || progressBar.getOrientation() != SwingConstants.HORIZONTAL
                || !c.getComponentOrientation().isLeftToRight();
    }

    private void drawTypePaint(final int height, final int amountOfProgressBarThatIsFilled, final Graphics2D graphics2D,
                               final RoundRectangle2D rectangle) {
        final Paint paint = graphics2D.getPaint();
        final Shape clip = graphics2D.getClip();
        final boolean movingRight = velocity >= 0;

        var tp = new TexturePaint(loadBackground(this.getClass().getResource("/background.png")),
                new Rectangle2D.Double(0, 1, height - 2f * JBUI.scale(1f) - JBUI.scale(1f), height - 2f * JBUI.scale(1f) - JBUI.scale(1f)));
        graphics2D.setPaint(tp);
        graphics2D.setClip(movingRight ? new Rectangle(0, 0, amountOfProgressBarThatIsFilled, height) : new Rectangle(amountOfProgressBarThatIsFilled, 0, progressBar.getWidth(), height));

        graphics2D.fill(rectangle);
        graphics2D.setPaint(paint);
        graphics2D.setClip(clip);
    }

    private void drawIcon(final int amountOfProgressBarThatIsFilled, final Graphics2D graphics2D, final RoundRectangle2D roundedProgressBarShape, final int defaultHeightProgressBar) {
        final Shape previousClip = graphics2D.getClip();
        graphics2D.setClip(roundedProgressBarShape);

        final var resource = velocity >= 0 ? "/train.png" : "/train_reverse.png";
        BufferedImage image = loadBackground(this.getClass().getResource(resource));
        var icon = new TrainIcon(17,17, image);
        icon.paintIcon(progressBar, graphics2D, amountOfProgressBarThatIsFilled, JBUI.scale(1));

        graphics2D.setClip(previousClip);
    }

    private void drawBorder(final RoundRectangle2D rectangle, final Graphics2D graphics2D) {
        final Color color = graphics2D.getColor();
        final Stroke stroke = graphics2D.getStroke();

        graphics2D.setColor(new JBColor(new Color(255, 204, 51), new Color(255, 204, 51)));
        graphics2D.setStroke(new BasicStroke(2));
        graphics2D.draw(rectangle);

        graphics2D.setColor(color);
        graphics2D.setStroke(stroke);
    }

    private RoundRectangle2D getRoundRectangle(final int width, final int height) {
        final float arcLength = JBUIScale.scale(9f);
        final float offset = JBUIScale.scale(2f);

        return new RoundRectangle2D.Float(JBUIScale.scale(1f),
                JBUIScale.scale(1f),
                width - offset,
                height - offset,
                arcLength,
                arcLength);
    }

    private static BufferedImage loadBackground(URL url) {
        BufferedImage image = null;
        try {
            image = read(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    private void resetPositionAndVelocity() {
        pos = 0;
        velocity = 1;
    }

    private void updatePosition() {
        final float v = velocity;
        final int p = pos;
        final float acceleration = 0.4f;

        if (velocity < 0) {
            if (pos <= 0) {
                velocity = 1.0f;
                pos = 0;
            } else {
                pos = p + (int) JBUIScale.scale(velocity);
                velocity = v - acceleration;
            }
        } else if (velocity > 0) {
            if (pos >= progressBar.getWidth()) {
                velocity = -1.0f;
                pos = progressBar.getWidth();
            } else {
                pos = p + (int) JBUIScale.scale(velocity);
                velocity = v + acceleration;
            }
        }
    }

    private static boolean isEven(int value) {
        return value % 2 == 0;
    }
}
