package com.javaJam.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.javaJam.controller.MusicController;
import com.javaJam.model.Music;
import com.sun.glass.ui.View;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import static com.javaJam.service.Theme.*;

public class MusicView extends JFrame {
    private JPanel browserPanel;
    private MusicController musicController;
    private DefaultListModel<Music> listModel;
    private JList<Music> musicListUI;

    private JButton playPauseButton;
    private JButton repeatButton;
    private JSlider seekSlider;
    private JLabel currentTimeLabel;
    private JLabel durationLabel;
    private JLabel nowPlayingLabel;
    private JTextField searchField;
    private JTextField clientCountField;
    private JButton profileButton;
    private JButton prevButton;
    private JButton nextButton;
    private JButton shuffleButton;
    private JButton jamButton;
    private JSlider volumeSlider;
    private final JMenuItem profileMenuItem = new JMenuItem("Profile");
    private final JMenuItem settingsMenuItem = new JMenuItem("Settings");
    private JPopupMenu profileMenu;
    private JPanel mainContentPanel;


    public final Color bgDark = BG_DARK;
    public final Color panelDark = PANEL_DARK;
    public final Color textLight = TEXT_LIGHT;
    public final Color buttonDark = BUTTON_DARK;
    public final Color accentColor = ACCENT_COLOR;


    public MusicView() {
        setTitle("JavaJam");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(bgDark);

        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(bgDark);

        //TOP NAV
        JPanel topNavPanel = new JPanel(new BorderLayout());
        topNavPanel.setBackground(panelDark);
        topNavPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JPanel leftNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftNav.setBackground(panelDark);

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setForeground(textLight);

        searchField = new JTextField(20);
        searchField.setBackground(bgDark);
        searchField.setForeground(textLight);
        searchField.setCaretColor(textLight);
        searchField.setFocusable(true);
        searchField.setRequestFocusEnabled(true);

        searchField.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (musicController != null) {
                        musicController.releaseBrowserFocus();
                    }
                    searchField.requestFocusInWindow();
                    searchField.grabFocus();
                    System.out.println("[SWING] Search focus: " + searchField.isFocusOwner());
                });
            }
        });

        leftNav.add(searchLabel);
        leftNav.add(searchField);
        topNavPanel.add(leftNav, BorderLayout.WEST);

        //Profile menu
        JPanel rightNav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightNav.setBackground(panelDark);

        profileButton = createStyledButton("icons/profile.svg", buttonDark, textLight);
        profileMenu = new JPopupMenu();
        profileMenu.add(profileMenuItem);
        profileMenu.add(settingsMenuItem);

        rightNav.add(profileButton);
        topNavPanel.add(rightNav, BorderLayout.EAST);
        add(topNavPanel, BorderLayout.NORTH);
        mainContentPanel.add(topNavPanel, BorderLayout.NORTH);

        //List view n Web player
        listModel = new DefaultListModel<>();
        musicListUI = new JList<>(listModel);
        musicListUI.setBackground(bgDark);
        musicListUI.setForeground(textLight);
        musicListUI.setSelectionBackground(accentColor);
        musicListUI.setSelectionForeground(Color.WHITE);
        musicListUI.setFixedCellHeight(36);

        musicListUI.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {

                super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus
                );

                if (value instanceof Music) {
                    setText("   " + ((Music) value).getName());
                }

                setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

                return this;
            }
        });

        browserPanel = new JPanel(new BorderLayout());
        browserPanel.setBackground(Color.BLACK);

        JPanel rightContainer = new JPanel(new BorderLayout());
        rightContainer.add(browserPanel, BorderLayout.CENTER);
        rightContainer.setBackground(bgDark);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(musicListUI),
                rightContainer
        );
        splitPane.setDividerLocation(300);
        splitPane.setBackground(bgDark);
        add(splitPane, BorderLayout.CENTER);
        mainContentPanel.add(splitPane, BorderLayout.CENTER);

        //BOTTOM MUSIC PLAYER
        JPanel bottomBar = new JPanel(new BorderLayout(10, 5));
        bottomBar.setBackground(panelDark);
        bottomBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        nowPlayingLabel = new JLabel("Nothing playing");
        nowPlayingLabel.setForeground(textLight);
        nowPlayingLabel.setHorizontalAlignment(SwingConstants.CENTER);

        bottomBar.add(nowPlayingLabel, BorderLayout.NORTH);

        JPanel controlsPanel = new JPanel(new BorderLayout(10, 5));
        controlsPanel.setBackground(panelDark);

        JPanel seekPanel = new JPanel(new BorderLayout(8, 0));
        seekPanel.setBackground(panelDark);

        currentTimeLabel = new JLabel("00:00");
        currentTimeLabel.setForeground(textLight);

        durationLabel = new JLabel("00:00");
        durationLabel.setForeground(textLight);

        seekSlider = new JSlider(0, 1000, 0);
        seekSlider.setBackground(panelDark);
        seekSlider.setForeground(accentColor);
        seekSlider.setPreferredSize(new Dimension(400, 25));

        seekPanel.add(currentTimeLabel, BorderLayout.WEST);
        seekPanel.add(seekSlider, BorderLayout.CENTER);
        seekPanel.add(durationLabel, BorderLayout.EAST);

        controlsPanel.add(seekPanel, BorderLayout.NORTH);

        //Buttons Panel
        JPanel buttonsPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 6, 5)
        );
        buttonsPanel.setBackground(panelDark);

        shuffleButton = createStyledButton("icons/shuffle.svg", buttonDark, textLight);
        prevButton = createStyledButton("icons/prev.svg", buttonDark, textLight);
        playPauseButton = createStyledButton("icons/play.svg", accentColor, Color.WHITE, 24, 24, new Insets(12, 12, 12, 12));
        nextButton = createStyledButton("icons/next.svg", buttonDark, textLight);
        repeatButton = createStyledButton("icons/repeat.svg", buttonDark, textLight);
        jamButton = createStyledButton("icons/jam.svg", buttonDark, textLight);


        JPanel clientIndicatorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        clientIndicatorPanel.setBackground(panelDark);

        JLabel clientIconLabel = new JLabel();
        clientIconLabel.setIcon(new com.formdev.flatlaf.extras.FlatSVGIcon("icons/eye.svg", 16, 16));
        clientIconLabel.setToolTipText("Connected Jam Clients");

        clientCountField = new JTextField("0", 2);
        clientCountField.setEditable(false);
        clientCountField.setFocusable(false);
        clientCountField.setBackground(panelDark);
        clientCountField.setBorder(null);
        clientCountField.setHorizontalAlignment(JTextField.CENTER);
        clientCountField.setToolTipText("Connected Jam Clients");

        clientIndicatorPanel.add(clientCountField);
        clientIndicatorPanel.add(clientIconLabel);

        buttonsPanel.add(shuffleButton);
        buttonsPanel.add(prevButton);
        buttonsPanel.add(playPauseButton);
        buttonsPanel.add(nextButton);
        buttonsPanel.add(repeatButton);
        buttonsPanel.add(jamButton);
        buttonsPanel.add(clientIndicatorPanel);

        //Volume Panel
        JPanel volumePanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 16, 8)
        );
        volumePanel.setBackground(panelDark);

        JLabel volumeLabel = new JLabel(new FlatSVGIcon("icons/volume.svg", 18, 18));
        volumeLabel.setForeground(textLight);

        volumeSlider = new JSlider(0, 100, 50);
        volumeSlider.setBackground(panelDark);
        volumeSlider.setPreferredSize(new Dimension(120, 25));

        volumePanel.add(volumeLabel);
        volumePanel.add(volumeSlider);

        //Clock Panel
        JPanel clockPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 20));
        clockPanel.setBackground(panelDark);
        clockPanel.setPreferredSize(volumePanel.getPreferredSize());

        JLabel clockLabel = new JLabel();
        clockLabel.setForeground(textLight);
        clockLabel.setFont(clockLabel.getFont().deriveFont(Font.PLAIN, 20f));
        clockPanel.add(clockLabel);


        Timer clockTimer = new Timer(1000, e -> {
            String timeStr = java.time.LocalTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("hh:mm:ss")
            );
            clockLabel.setText(timeStr);
        });
        clockTimer.setInitialDelay(0);
        clockTimer.start();

        JPanel lowerControlsPanel = new JPanel(new BorderLayout());
        lowerControlsPanel.setBackground(panelDark);
        lowerControlsPanel.add(clockPanel, BorderLayout.WEST);
        lowerControlsPanel.add(buttonsPanel, BorderLayout.CENTER);
        lowerControlsPanel.add(volumePanel, BorderLayout.EAST);

        controlsPanel.add(lowerControlsPanel, BorderLayout.CENTER);
        bottomBar.add(controlsPanel, BorderLayout.CENTER);
        mainContentPanel.add(bottomBar, BorderLayout.SOUTH);

        add(mainContentPanel, BorderLayout.CENTER);


        //DATA LISTENER
        listModel.addListDataListener(new javax.swing.event.ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                //System.out.println("Music item(s) added to the list!");
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                //System.out.println("Music item(s) removed from the list!");
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                //System.out.println("Music list contents changed/updated!");
            }
        });

        setupSearchFocus();
        setupFocusHandling();
    }


    private void setupSearchFocus() {

        searchField.setFocusable(true);
        searchField.setEditable(true);
        searchField.setEnabled(true);

        searchField.addMouseListener(new java.awt.event.MouseAdapter() {

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {

                SwingUtilities.invokeLater(() -> {

                    MusicView.this.requestFocus();

                    searchField.requestFocus();
                    searchField.grabFocus();

                    System.out.println(
                            "[SWING] Search focus requested: " +
                                    searchField.isFocusOwner()
                    );
                });
            }
        });
    }


    private JButton createStyledButton(String iconPath, Color bg, Color fg) {
        return createStyledButton(iconPath, bg, fg, 18, 18, new Insets(8, 8, 8, 8));
    }

    private JButton createStyledButton(String iconPath, Color bg, Color fg, int width, int height, Insets margin) {
        FlatSVGIcon icon = new FlatSVGIcon(iconPath, width, height);

        JButton button = new JButton(icon);

        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("JComponent.arc", 16);

        if (bg != null) {
            button.setBackground(bg);
        }

        if (fg != null) {
            button.setForeground(fg);
        }

        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setMargin(margin);

        return button;
    }


    private void setupFocusHandling() {

        setFocusable(true);
        searchField.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    searchField.requestFocus();
                    searchField.grabFocus();
                });
            }
        });

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(event -> {

                    if (!isActive()) {
                        return false;
                    }

                    if (event.getID() != KeyEvent.KEY_PRESSED) {
                        return false;
                    }

                    return false;
                });
    }


    // GETTERS

    public JTextField getSearchField() {
        return searchField;
    }

    public JButton getProfileButton() {
        return profileButton;
    }

    public JMenuItem getProfileMenuItem(){return profileMenuItem;}

    public JMenuItem getSettingsMenuItem(){return settingsMenuItem;}

    public JPopupMenu getProfileMenu() {return profileMenu;}

    public JButton getPrevButton() {
        return prevButton;
    }

    public JButton getNextButton() {
        return nextButton;
    }

    public JButton getShuffleButton() {
        return shuffleButton;
    }

    public JButton getJamButton() {
        return jamButton;
    }

    public JSlider getVolumeSlider() {
        return volumeSlider;
    }

    public JList<Music> getMusicListUI() {
        return musicListUI;
    }

    public DefaultListModel<Music> getListModel() {
        return listModel;
    }

    public JButton getPlayPauseButton() {
        return playPauseButton;
    }

    public JButton getRepeatButton() {
        return repeatButton;
    }

    public JSlider getSeekSlider() {
        return seekSlider;
    }

    public void setNowPlaying(String name) {
        SwingUtilities.invokeLater(() ->
                nowPlayingLabel.setText(name)
        );
    }

    public void focusSearchField() {
        SwingUtilities.invokeLater(() -> {
            searchField.requestFocusInWindow();
        });
    }

    public void setMusicController(MusicController controller) {
        this.musicController = controller;
    }

    public JLabel getCurrentTimeLabel() {
        return currentTimeLabel;
    }

    public JPanel getBrowserPanel() {
        return browserPanel;
    }

    public JLabel getDurationLabel() {
        return durationLabel;
    }

    public JTextField getClientCountField() {return  clientCountField;}


    public void showMainView() {
        getContentPane().removeAll();
        add(mainContentPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void showNewView(JPanel view) {
        getContentPane().removeAll();
        add(view, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}