import com.intellij.ide.ui.LafManager;

import javax.swing.*;

public class ApplicationComponent {
    public ApplicationComponent() {
        LafManager.getInstance().addLafManagerListener(__ -> updateProgressBarUi());
        updateProgressBarUi();
    }

    private void updateProgressBarUi() {
        UIManager.put("ProgressBarUI", NsProgressBar.class.getName());
        UIManager.getDefaults().put(NsProgressBar.class.getName(), NsProgressBar.class);
    }
}
