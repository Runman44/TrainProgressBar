import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

class TrainIcon implements Icon {
    private final int width;
    private final int height;
    private final Image icon;

    public TrainIcon(int width, int height, @NotNull Image icon) {
        this.width = width;
        this.height = height;
        this.icon = icon;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.drawImage(icon, x, y, width, height, null);
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }
}