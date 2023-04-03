package gitlab.settings;


import com.github.lvlifeng.githelper.Bundle;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class SettingsComponent implements SearchableConfigurable {

    private final GitLabSettingsState gitLabSettingsState;

    private final Project project;

    @Nullable
    private SettingsView settingsView;

    public SettingsComponent(Project project) {
        this.project = project;
        this.gitLabSettingsState = GitLabSettingsState.getInstance();
    }

    public JComponent createComponent() {
        if (settingsView == null) {
            settingsView = new SettingsView(project);
        }
        return settingsView.createComponent();
    }

    @Override
    public boolean isModified() {
        return settingsView.isModified();
    }

    @Override
    public void disposeUIResources() {
        settingsView = null;
    }

    @Override
    public String getHelpTopic() {
        return Bundle.message("gitlabSettingsId");
    }

    public void apply() throws ConfigurationException {
        if (settingsView != null) {
            try {
                settingsView.init();
            } catch (Exception e) {
                throw new ConfigurationException(e.getMessage());
            }
        }
    }

    @Nls
    public String getDisplayName() {
        return Bundle.message("gitlabSettingsId");
    }

    @Override
    public void reset() {
        if (settingsView != null) {
            settingsView.init();
        }
    }

    @NotNull
    @Override
    public String getId() {
        return Bundle.message("gitlabSettingsId");
    }
}

