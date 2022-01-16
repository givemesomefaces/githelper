package gitlab.settings;

import com.github.lvlifeng.githelper.icons.Icons;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import git4idea.DialogManager;
import gitlab.bean.GitlabServer;
import gitlab.bean.ReadOnlyTableModel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;


public class SettingsView extends DialogWrapper implements SearchableConfigurable {

    public static final String DIALOG_TITLE = "GitLab Settings";
    GitLabSettingsState settingsState = GitLabSettingsState.getInstance();

    private JPanel mainPanel;
    private JTable serverTable;
    private JLabel addButton;
    private JLabel editButton;
    private JLabel deleteButton;

    public SettingsView(@Nullable Project project) {
        super(project);
        setTitle(DIALOG_TITLE);
        init();
        setup();
    }

    @Override
    protected void init() {
        super.init();
        initButton();
        reset();
    }

    private void initButton() {
        addButton.setIcon(Icons.Add);
        addButton.setBorder(null);
        editButton.setIcon(Icons.Edit);
        editButton.setBorder(null);
        deleteButton.setIcon(Icons.Delete);
        deleteButton.setBorder(null);
    }

    public void setup() {
        addButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                ServerConfiguration serverConfiguration = new ServerConfiguration(null);
                DialogManager.show(serverConfiguration);
                reset();
            }
        });
        deleteButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                GitlabServer server = getSelectedServer();
                if (server != null) {
                    settingsState.deleteServer(server);
                    reset();
                }
            }
        });
        editButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                GitlabServer server = getSelectedServer();
                if (server == null) {
                    return;
                }
                ServerConfiguration serverConfiguration = new ServerConfiguration(server);
                DialogManager.show(serverConfiguration);
                reset();
            }
        });
    }

    private GitlabServer getSelectedServer() {
        if(serverTable.getSelectedRow() >= 0) {
            return (GitlabServer) serverTable.getValueAt(serverTable.getSelectedRow(), 0);
        }
        return null;
    }

    private TableModel serverModel(Collection<GitlabServer> servers) {
        Object[] columnNames = {"", "Server", "Token", "Checkout Method"};
        Object[][] data = new Object[servers.size()][columnNames.length];
        int i = 0;
        for (GitlabServer server : servers) {
            if (server == null) {
                continue;
            }
            Object[] row = new Object[columnNames.length];
            row[0] = server;
            row[1] = server.getApiUrl();
            row[2] = server.getApiToken();
            row[3] = server.getPreferredConnection().name();
            data[i] = row;
            i++;
        }
        return new ReadOnlyTableModel(data, columnNames);
    }

    @NotNull
    @Override
    public String getId() {
        return DIALOG_TITLE;
    }

    @Nullable
    @Override
    public Runnable enableSearch(String s) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return DIALOG_TITLE;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        reset();
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }

    @Override
    public void reset() {
        fill(settingsState);
    }

    @Override
    public void disposeUIResources() {

    }

    public void fill(GitLabSettingsState settingsState) {
        serverTable.setModel(serverModel(settingsState.getGitlabServers()));
        serverTable.getColumnModel().getColumn(0).setMinWidth(0);
        serverTable.getColumnModel().getColumn(0).setMaxWidth(0);
        serverTable.getColumnModel().getColumn(0).setWidth(0);
        serverTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serverTable.getSelectionModel().addListSelectionListener(event -> {
            editButton.setEnabled(true);
            deleteButton.setEnabled(true);
        });

    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return mainPanel;
    }
}