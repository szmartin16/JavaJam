package com.javaJam.controller;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.javaJam.view.ProfileView;
import com.javaJam.view.SettingsView;
import com.javaJam.windows.WindowsTaskbarThumbnail;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler;
import com.javaJam.model.Music;
import com.javaJam.network.JamManager;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefMessageRouterHandler;
import com.javaJam.service.YoutubeScraperService;
import com.javaJam.view.MusicView;
import org.json.JSONException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MusicController {

    private CefApp cefApp;
    private CefClient cefClient;
    private CefBrowser cefBrowser;
    private CefMessageRouter messageRouter;
    private boolean isCefInitialized = false;
    private boolean trackStarted = false;

    private volatile double currentVideoTime = 0;
    private volatile double videoDuration = 0;

    private boolean playerPageLoaded = false;
    private CefMessageRouterHandler progressHandler;

    private HttpServer playerServer;
    private int playerServerPort;

    private final MusicView musicView;
    private final SettingsView settingsView;
    private final ProfileView profileView;

    private final YoutubeScraperService scraper;
    private JamManager jamManager;
    private String loadedVideoId = null;
    private boolean isAdvancingTrack = false;
    private boolean progressRequestPending = false;
    private int playbackGeneration = 0;

    private List<Music> tracks;
    private int currentTrackIndex = -1;

    private boolean isPlaying = false;
    private boolean repeatEnabled = false;
    private boolean isSeeking = false;
    private boolean shuffleEnabled = false;
    private Timer playbackTimer;
    private double cachedDuration = 0;
    private String currentVideoId = null;
    private String currentTrackName = "Nothing playing";
    private boolean syncingFromNetwork = false;

    private WindowsTaskbarThumbnail taskbarThumbnail;

    private final FlatSVGIcon playIcon = new FlatSVGIcon("icons/play.svg", 18, 18);
    private final FlatSVGIcon pauseIcon = new FlatSVGIcon("icons/pause.svg", 18, 18);

    public MusicController(MusicView view, SettingsView settingsView, ProfileView profileView) {
        this.musicView = view;
        this.settingsView = settingsView;
        this.profileView = profileView;
        this.musicView.setMusicController(this);
        this.scraper = new YoutubeScraperService();
        this.jamManager = new JamManager(this);

        startPlayerServer();
        initController();
        startPlaybackTimer();
        startupSequnce();
    }


    private void startupSequnce() {
        System.out.println("[CONTROLLER] Executing startup sequence...");

        if (musicView.getMusicListUI() != null) {
            System.out.println("[CONTROLLER] Music list UI found. Searching default tracks: 'Top 10 music today'");
            searchTracks("Top 10 music today");
        } else {
            System.out.println("[CONTROLLER] Music list UI is null on startup, skipping default search.");
        }

        String osName = System.getProperty("os.name").toLowerCase();
        System.out.println("[CONTROLLER] Operating System detected: " + osName);

        if (osName.contains("win")) {
            System.out.println("[CONTROLLER] Windows environment detected. Registering WindowListener for taskbar controls.");

            musicView.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    System.out.println("[CONTROLLER] Window opened event fired. Initializing taskbar thumbnails on EDT...");

                    SwingUtilities.invokeLater(() -> {
                        try {
                            taskbarThumbnail = new WindowsTaskbarThumbnail(
                                    musicView,
                                    MusicController.this::playPreviousTrack,
                                    MusicController.this::togglePlayPause,
                                    MusicController.this::playNextTrack,
                                    MusicController.this
                            );

                            taskbarThumbnail.initialize();
                            System.out.println("[CONTROLLER] Windows Taskbar Thumbnail successfully initialized.");
                        } catch (Exception ex) {
                            System.err.println("[CONTROLLER] ERROR: Failed to initialize WindowsTaskbarThumbnail: " + ex.getMessage());
                            ex.printStackTrace();
                        }
                    });
                }

                @Override
                public void windowClosed(WindowEvent e) {
                    System.out.println("[CONTROLLER] Window closed event fired. Cleaning up taskbar resources...");

                    if (taskbarThumbnail != null) {
                        taskbarThumbnail.dispose();
                        System.out.println("[CONTROLLER] Taskbar thumbnail resources disposed.");
                    } else {
                        System.out.println("[CONTROLLER] Taskbar thumbnail was null during window close.");
                    }
                }
            });
        } else {
            System.out.println("[CONTROLLER] Non-Windows OS detected. Skipping taskbar thumbnail initialization.");
        }
    }


    private void initCefPlayer() {
        if (isCefInitialized) {
            System.out.println("[CEF] Player already initialized. Skipping...");
            return;
        }
        Thread cefThread = new Thread(() -> {
            try {
                CefAppBuilder builder = new CefAppBuilder();

                builder.setInstallDir(
                        new java.io.File("jcef-bundle")
                );

                builder.setProgressHandler(
                        new ConsoleProgressHandler()
                );

                builder.getCefSettings()
                        .windowless_rendering_enabled = false;

                builder.addJcefArgs(
                        "--autoplay-policy=no-user-gesture-required"
                );

                cefApp = builder.build();
                isCefInitialized = true;

                cefClient = cefApp.createClient();

                //Create browser
                cefBrowser = cefClient.createBrowser(
                        "about:blank",
                        false,
                        false
                );

                //Create message router
                messageRouter = CefMessageRouter.create(
                        new CefMessageRouter.CefMessageRouterConfig(
                                "cefQuery",
                                "cefQueryCancel"
                        )
                );

                if (messageRouter == null) {
                    throw new IllegalStateException(
                            "Failed to create CefMessageRouter"
                    );
                }


                progressHandler = new CefMessageRouterHandler() {

                    @Override
                    public void setNativeRef(String identifer, long nativeRef) {

                    }

                    @Override
                    public long getNativeRef(String identifer) {
                        return 0;
                    }

                    @Override
                    public boolean onQuery(
                            CefBrowser browser,
                            CefFrame frame,
                            long queryId,
                            String request,
                            boolean persistent,
                            CefQueryCallback callback) {

                        //System.out.println("[JCEF QUERY] " + request);

                        try {
                            org.json.JSONObject json =
                                    new org.json.JSONObject(request);

                            if ("playing".equals(json.optString("type"))) {

                                SwingUtilities.invokeLater(() -> {
                                    trackStarted = true;
                                });
                            }

                            if ("progress".equals(json.optString("type"))) {
                                double current=0;
                                double duration=0;
                                try {
                                    current = json.getDouble("current");
                                    duration = json.getDouble("duration");
                                }catch (JSONException e)
                                {
                                   System.out.println("Json error: "+ e);
                                }


                                currentVideoTime = current;
                                videoDuration = duration;

                              //  System.out.println("[JAVA PROGRESS] " + current + " / " + duration);
                            }

                            callback.success("OK");
                            return true;

                        } catch (Exception e) {
                            e.printStackTrace();
                            callback.failure(1, e.getMessage());
                            return true;
                        }
                    }

                    @Override
                    public void onQueryCanceled(
                            CefBrowser browser,
                            CefFrame frame,
                            long queryId) {

                        System.out.println("[JCEF] Query canceled: " + queryId);
                    }
                };

                messageRouter.addHandler(progressHandler, true);

                cefClient.addMessageRouter(messageRouter);

                cefClient.addLoadHandler(new CefLoadHandlerAdapter() {
                    @Override
                    public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                        if (frame.isMain() && httpStatusCode == 200) {
                            String currentUrl = browser.getURL();
                            frame.executeJavaScript(
                                    "if (navigator.mediaDevices) {" +
                                            "  navigator.mediaDevices.getUserMedia = function() {" +
                                            "    return Promise.reject(new Error('Camera access is disabled by this application.'));" +
                                            "  };" +
                                            "  navigator.mediaDevices.webkitGetUserMedia = navigator.mediaDevices.getUserMedia;" +
                                            "  navigator.mediaDevices.mozGetUserMedia = navigator.mediaDevices.getUserMedia;" +
                                            "}",
                                    frame.getURL(), 0
                            );
                            if (currentUrl != null && currentUrl.contains("/player/")) {
                                System.out.println("[JCEF] Player page load complete. Sending initial JS start command...");

                                SwingUtilities.invokeLater(() -> {
                                    if (isPlaying) {
                                        executePlayerCommand("if(typeof playVideo === 'function') { playVideo(); }");
                                        if (taskbarThumbnail != null) {
                                            taskbarThumbnail.setPlaying();
                                        }
                                    }
                                    if (musicView.getVolumeSlider() != null) {
                                        setPlayerVolume(musicView.getVolumeSlider().getValue());
                                    }
                                });
                            }
                        }
                    }
                });

                cefClient.addDisplayHandler(new CefDisplayHandlerAdapter() {

                    @Override
                    public boolean onConsoleMessage(
                            CefBrowser browser,
                            CefSettings.LogSeverity level,
                            String message,
                            String source,
                            int line) {
                        return false;
                    }
                });

                cefClient.addFocusHandler(
                        new org.cef.handler.CefFocusHandlerAdapter() {

                            @Override
                            public void onGotFocus(CefBrowser browser) {
                                System.out.println("[JCEF] Browser got focus");
                            }

                            @Override
                            public void onTakeFocus(CefBrowser browser, boolean next) {
                                System.out.println("[JCEF] Browser released focus");
                            }
                        }
                );

                System.out.println("[JCEF] Message router initialized");

                SwingUtilities.invokeLater(() -> {

                    Component browserComponent = cefBrowser.getUIComponent();

                    musicView.getBrowserPanel().add(
                            browserComponent,
                            BorderLayout.CENTER
                    );

                    musicView.getBrowserPanel().revalidate();
                    musicView.getBrowserPanel().repaint();

                });

            } catch (Throwable e) {
                e.printStackTrace();
            }
        }, "JCEF-Initialization");

        cefThread.start();
    }


    private void startPlayerServer() {
        try {
            playerServer = HttpServer.create(
                    new InetSocketAddress("localhost", 0), 0
            );

            playerServerPort = playerServer.getAddress().getPort();

            playerServer.createContext("/player/", exchange -> {

                try {
                    String path = exchange.getRequestURI().getPath();
                    String videoId = path.substring("/player/".length());

                    if (!videoId.matches("[a-zA-Z0-9_-]{11}")) {
                        exchange.sendResponseHeaders(400, -1);
                        return;
                    }

                    String origin = "http://localhost:" + playerServerPort;


                    String html;
                    try (InputStream input = getClass()
                            .getResourceAsStream("/html/player.html")) {

                        if (input == null) {
                            throw new IOException("player.html not found!");
                        }

                        html = new String(
                                input.readAllBytes(),
                                StandardCharsets.UTF_8
                        );
                    }

                    html = html
                            .replace("VIDEO_ID", videoId)
                            .replace("__PLAYER_ORIGIN__", origin);

                    byte[] response = html.getBytes(StandardCharsets.UTF_8);

                    Headers headers = exchange.getResponseHeaders();

                    headers.set("Content-Type", "text/html; charset=UTF-8");
                    headers.set("Access-Control-Allow-Origin", "*");
                    headers.set(
                            "Referrer-Policy",
                            "strict-origin-when-cross-origin"
                    );
                    headers.set("Cache-Control", "no-store");
                    headers.set("X-Content-Type-Options", "nosniff");

                    exchange.sendResponseHeaders(200, response.length);

                    try (OutputStream output = exchange.getResponseBody()) {
                        output.write(response);
                    }

                } catch (Exception e) {
                    e.printStackTrace();

                    if (exchange.getResponseCode() == -1) {
                        exchange.sendResponseHeaders(500, -1);
                    }

                } finally {
                    exchange.close();
                }
            });

            playerServer.start();

            System.out.println(
                    "YouTube player server started: " + playerServerPort
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void initController() {
        // Search
        musicView.getSearchField().addActionListener(e -> {
            String query = musicView.getSearchField().getText().trim();
            if (!query.isEmpty()) {
                searchTracks(query);
            }
        });

        // Select a track from list
        musicView.getMusicListUI().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || syncingFromNetwork) return;

            Music selected = musicView.getMusicListUI().getSelectedValue();
            if (selected == null) return;

            currentTrackIndex = musicView.getMusicListUI().getSelectedIndex();

            if (jamManager.isConnected() && !jamManager.isHost()) {
                String videoId = extractVideoId(selected.getUrl());
                if (videoId != null) {
                    jamManager.sendSelectTrackCommand(videoId, selected.getName());
                }
                return;
            }

            handleTrackSelection(selected);
        });

        // Play
        musicView.getPlayPauseButton().addActionListener(e -> togglePlayPause());

        // Previous
        musicView.getPrevButton().addActionListener(e -> {

            if (jamManager.isConnected() && !jamManager.isHost()) {
                jamManager.sendControlCommand("PREVIOUS", 0);
            } else {
                playPreviousTrack();
            }
        });

        // Next
        musicView.getNextButton().addActionListener(e -> {

            if (jamManager.isConnected() && !jamManager.isHost()) {
                jamManager.sendControlCommand("NEXT", 0);
            } else {
                playNextTrack();
            }
        });

        // Shuffle
        musicView.getShuffleButton().addActionListener(e -> toggleShuffle());

        // Repeat
        musicView.getRepeatButton().addActionListener(e -> toggleRepeat());

        musicView.getJamButton().addActionListener(e -> handleJamAction());

        // Volume
        musicView.getVolumeSlider().addChangeListener(e -> {
            if (!musicView.getVolumeSlider().getValueIsAdjusting()) {
                setPlayerVolume(musicView.getVolumeSlider().getValue());
            }
        });

        musicView.getSeekSlider().addChangeListener(e -> {

            JSlider slider = musicView.getSeekSlider();

            if (slider.getValueIsAdjusting()) {

                isSeeking = true;

                double duration = cachedDuration;

                if (duration > 0) {
                    double target = duration * slider.getValue() / 1000.0;

                    musicView.getCurrentTimeLabel().setText(formatTime(target));
                }

            } else if (isSeeking) {

                isSeeking = false;

                double target = cachedDuration *
                        (slider.getValue() / 1000.0);

                if (jamManager.isConnected() && !jamManager.isHost()) {

                    jamManager.sendControlCommand("SEEK", target);

                } else {

                    seekVideo(target);
                    currentVideoTime = target;

                    broadcastCurrentState();
                }
            }
        });

        musicView.getProfileMenuItem().addActionListener(e -> {
            musicView.showNewView(profileView);

        });

        musicView.getSettingsMenuItem().addActionListener(e -> {
            musicView.showNewView(settingsView);
        });

        musicView.getProfileButton().addActionListener(e -> {
            musicView.getProfileMenu().show(musicView.getProfileButton(), 0, musicView.getProfileButton().getHeight());
        });
    }

    public void playVideo() {
        executePlayerCommand("playVideo()");
    }

    public void pauseVideo() {
        executePlayerCommand("pauseVideo()");
    }

    public void stopVideo() {
        executePlayerCommand("stopVideo()");
    }

    public void seekVideo(double seconds) {
        executePlayerCommand("seekVideo(" + seconds + ")");
    }

    public void setPlayerVolume(int volume) {
        int safeVolume = Math.max(0, Math.min(100, volume));
        executePlayerCommand("setVolume(" + safeVolume + ")");
    }

    public void setRepeatEnabledInJs(boolean enabled) {
        executePlayerCommand("setRepeatEnabled(" + enabled + ")");
    }

    private void executePlayerCommand(String javascript) {

        if (cefBrowser == null) {
            System.err.println("[JCEF] Browser unavailable");
            return;
        }

        cefBrowser.executeJavaScript(
                javascript,
                cefBrowser.getURL(),
                0
        );
    }

    public void getVideoInformation(
            java.util.function.BiConsumer<Double, Double> callback) {

        double current = currentVideoTime;
        double duration = videoDuration;

        SwingUtilities.invokeLater(() ->
                callback.accept(current, duration)
        );
    }

    private void openYouTube(String videoId) {
        try {
            Desktop.getDesktop().browse(
                    new java.net.URI(
                            "https://www.youtube.com/watch?v=" + videoId
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadVideoInApp(String videoId) {

        if (videoId == null ||
                !videoId.matches("[a-zA-Z0-9_-]{11}")) {
            return;
        }

        if (cefBrowser == null) {
            System.err.println("[JCEF] Browser not initialized");
            return;
        }

        loadedVideoId = videoId;
        currentVideoId = videoId;

        if (!playerPageLoaded) {

            String url = "http://localhost:"
                    + playerServerPort
                    + "/player/"
                    + videoId;

            System.out.println("[JCEF] Initial loading: " + url);

            playerPageLoaded = true;
            cefBrowser.loadURL(url);

        } else {

            System.out.println("[JCEF] Switching video: " + videoId);

            executePlayerCommand(
                    "window.loadNewVideo('" + videoId + "')"
            );
            executePlayerCommand(
                    "window.playVideo()"
            );
        }
    }

    private void handleJamAction() {
        String[] options = {"Host Session", "Join with Code", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
                musicView,
                "Choose Jam Mode:",
                "Jam Session",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) {
            String sessionId = jamManager.generateSessionId();
            jamManager.startHosting(sessionId);

            JTextArea textArea = new JTextArea("Hosting Session via Relay!\nYour Session Code: " + sessionId);
            textArea.setEditable(false);
            textArea.setOpaque(false);
            textArea.setFocusable(true);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);

            JOptionPane.showMessageDialog(
                    musicView,
                    textArea,
                    "Jam Host Active",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } else if (choice == 1) {
            String sessionId = JOptionPane.showInputDialog(musicView, "Enter Session Code: ");
            if (sessionId != null && !sessionId.trim().isEmpty()) {
                jamManager.joinHost(sessionId.trim());
            }
        }
    }

    private void startPlaybackTimer() {

        playbackTimer = new Timer(500, e -> {

            if (isSeeking ||
                    isAdvancingTrack ||
                    progressRequestPending ||
                    musicView.getSeekSlider().getValueIsAdjusting()) {
                return;
            }

            int generation = playbackGeneration;

            progressRequestPending = true;

            getVideoInformation((currentTime, duration) -> {

                progressRequestPending = false;

                if (generation != playbackGeneration) {
                    return;
                }

                if (duration <= 0 || !isPlaying || isAdvancingTrack) {
                    return;
                }

                cachedDuration = duration;

                musicView.getCurrentTimeLabel().setText(formatTime(currentTime));
                musicView.getDurationLabel().setText(formatTime(duration));

                if (currentVideoId != null && isPlaying && !syncingFromNetwork) {

                    broadcastCurrentState();
                }

                if (isSeeking || musicView.getSeekSlider().getValueIsAdjusting()) {
                    return;
                }

                if (trackStarted &&
                        currentTime > 0 &&
                        duration - currentTime <= 10 &&
                        tracks != null &&
                        !tracks.isEmpty()) {

                    isAdvancingTrack = true;
                    isPlaying = false;
                    trackStarted = false;

                    System.out.println("[PLAYER] Track finished. Advancing...");

                    SwingUtilities.invokeLater(() -> {

                        musicView.getSeekSlider().setValue(0);
                        musicView.getCurrentTimeLabel().setText("00:00");

                        playNextTrack();
                    });

                    return;
                }

                int progress = (int) (currentTime / duration * 1000);

                musicView.getSeekSlider().setValue(
                        Math.max(0, Math.min(1000, progress))
                );
            });
        });
        playbackTimer.start();
    }

    private String formatTime(double seconds) {

        int totalSeconds = (int) Math.max(0, seconds);

        int minutes = totalSeconds / 60;
        int remainingSeconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    private void searchTracks(String query) {
        new SwingWorker<List<Music>, Void>() {
            @Override
            protected List<Music> doInBackground() {
                return scraper.searchTracks(query);
            }

            @Override
            protected void done() {
                try {
                    tracks = get();
                    musicView.getListModel().clear();

                    for (Music track : tracks) {
                        musicView.getListModel().addElement(track);
                    }

                    currentTrackIndex = -1;
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(musicView, "Unable to search YouTube tracks.", "Search Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();

        if(cefClient == null) {
            initCef();
        }
    }

    private void handleTrackSelection(Music track) {

        if (syncingFromNetwork) return;

        playbackGeneration++;

        trackStarted = false;
        isAdvancingTrack = false;
        isPlaying = true;
        cachedDuration = 0;
        currentVideoTime = 0;

        currentTrackName = track.getName();

        musicView.getSeekSlider().setValue(0);
        musicView.getCurrentTimeLabel().setText("00:00");

        musicView.setNowPlaying(track.getName());
        musicView.getPlayPauseButton().setIcon(pauseIcon);

        String videoId = extractVideoId(track.getUrl());

        if (videoId != null) {
            currentVideoId = videoId;

            loadVideoInApp(videoId);
            broadcastCurrentState();
        }
    }

    public void syncFromHost(
            String videoId,
            double position,
            boolean playing,
            String trackName
    ) {
        syncingFromNetwork = true;
        try {
            playbackGeneration++;
            trackStarted = false;
            isAdvancingTrack = false;
            currentTrackName = trackName;

            musicView.setNowPlaying(trackName);
            musicView.getPlayPauseButton().setIcon(playing ? pauseIcon : playIcon);
            taskbarThumbnail.setPlaying();

            if (videoId != null && (!videoId.equals(currentVideoId) || !playerPageLoaded || loadedVideoId == null)) {
                //System.out.println("[JAM SYNC] Initializing/Switching to track from host: " + videoId);
                loadVideoInApp(videoId);
            }

            double timeDiff = Math.abs(currentVideoTime - position);
            if (timeDiff > 2.0) {
                //System.out.println("[JAM SYNC] Drift detected (" + timeDiff + "s), seeking to: " + position);
                seekVideo(position);
            }

            currentVideoTime = position;
            isPlaying = playing;

            if (playing) {
                playVideo();
            } else {
                pauseVideo();
            }
        } finally {
            syncingFromNetwork = false;
        }
    }

    private void broadcastCurrentState() {
        if (currentVideoId == null) return;

        //host should broadcast states
        if (jamManager.isConnected() && jamManager.isHost()) {
            jamManager.broadcastState(
                    currentVideoId,
                    currentVideoTime,
                    isPlaying,
                    currentTrackName
            );
        }
    }


    private void togglePlayPause() {
        if (jamManager.isConnected() && !jamManager.isHost()) {
            if (isPlaying) {
                jamManager.sendControlCommand("PAUSE", currentVideoTime);
            } else {
                jamManager.sendControlCommand("PLAY", currentVideoTime);
            }
            return;
        }


        if (isPlaying) {
            pauseVideo();
            isPlaying = false;
            musicView.getPlayPauseButton().setIcon(playIcon);
            jamManager.sendControlCommand("PAUSE", currentVideoTime);
        } else {
            playVideo();
            isPlaying = true;
            musicView.getPlayPauseButton().setIcon(pauseIcon);
            jamManager.sendControlCommand("PLAY", currentVideoTime);
        }

        if (taskbarThumbnail != null) {
            taskbarThumbnail.setPlaying();
        }
    }

    private void toggleRepeat() {
        repeatEnabled = !repeatEnabled;
        if (repeatEnabled) {
            musicView.getRepeatButton().setBackground(musicView.accentColor);
        } else {
            musicView.getRepeatButton().setBackground(musicView.buttonDark);
        }
        musicView.getRepeatButton().repaint();
        setRepeatEnabledInJs(repeatEnabled);
    }

    private void playNextTrack() {
        if (repeatEnabled){
            seekVideo(1);
            currentVideoTime = 1;
            isPlaying = true;
            trackStarted = true;
            isAdvancingTrack = false;
            return;
        }
        if (!shuffleEnabled){
            int size = musicView.getListModel().size();
            if (size == 0) {
                isAdvancingTrack = false;
                return;
            }

            currentTrackIndex++;
            if (currentTrackIndex >= size) {
                currentTrackIndex = 0;
            }
            selectTrackAt(currentTrackIndex);
        }
        else { shuffleTrack();}


    }

    private void playPreviousTrack() {
        int size = musicView.getListModel().size();
        if (size == 0) return;

        currentTrackIndex--;
        if (currentTrackIndex < 0) {
            currentTrackIndex = size - 1;
        }
        selectTrackAt(currentTrackIndex);
    }

    private void toggleShuffle() {
        shuffleEnabled = !shuffleEnabled;

        if (shuffleEnabled) {
            musicView.getShuffleButton().setBackground(musicView.accentColor);
        } else {
            musicView.getShuffleButton().setBackground(musicView.buttonDark);
        }
        musicView.getShuffleButton().repaint();
    }

    private void shuffleTrack() {
        if (jamManager.isConnected() && !jamManager.isHost()) {
            jamManager.sendControlCommand("SHUFFLE", 0);
        }
        else {

            int size = musicView.getListModel().size();
            if (size <= 1) return;

            int randomIndex;
            do {
                randomIndex = (int) (Math.random() * size);
            } while (randomIndex == currentTrackIndex);

            selectTrackAt(randomIndex);
        }
    }

    private void selectTrackAt(int index) {
        musicView.getMusicListUI().setSelectedIndex(index);
        musicView.getMusicListUI().ensureIndexIsVisible(index);
        Music track = musicView.getListModel().getElementAt(index);
        if (track != null) {
            String videoId = extractVideoId(track.getUrl());
            if (videoId != null) {
                if (jamManager.isConnected() && !jamManager.isHost()) {
                    jamManager.sendSelectTrackCommand(videoId, track.getName());
                } else {
                    handleTrackSelection(track);
                    if (jamManager.isConnected() && jamManager.isHost()) {
                        jamManager.sendSelectTrackCommand(videoId, track.getName());
                    }
                }
            }
        }
        isAdvancingTrack = false;
    }

    private String extractVideoId(String url) {
        if (url == null) return null;

        try {
            java.net.URI uri = java.net.URI.create(url);
            String host = uri.getHost();
            if (host == null) return null;

            String id = null;
            if (host.equals("youtu.be")) {
                id = uri.getPath().substring(1);
            } else if (host.endsWith("youtube.com")) {
                String query = uri.getRawQuery();
                if (query != null) {
                    for (String parameter : query.split("&")) {
                        if (parameter.startsWith("v=")) {
                            id = parameter.substring(2);
                            break;
                        }
                    }
                }
            }

            if (id != null && id.matches("[a-zA-Z0-9_-]{11}")) {
                return id;
            }
        } catch (Exception e) {
            System.err.println("Invalid YouTube URL: " + url);
        }

        return null;
    }


    public void remotePlay() {
        isPlaying = true;
        playVideo();
        musicView.getPlayPauseButton().setIcon(pauseIcon);
    }

    public void remotePause() {
        isPlaying = false;
        pauseVideo();
        musicView.getPlayPauseButton().setIcon(playIcon);
    }

    public void remoteSeek(double position) {
        seekVideo(position);
        currentVideoTime = position;
    }

    public void remoteNext() {
        playNextTrack();
    }

    public void remotePrevious() {
        playPreviousTrack();
    }

    public void remoteShuffle() {
        toggleShuffle();
    }

    public void remoteStop() {
        stopVideo();
        isPlaying = false;
        musicView.getPlayPauseButton().setIcon(playIcon);
        musicView.getSeekSlider().setValue(0);
        musicView.getCurrentTimeLabel().setText("00:00");
        musicView.setNowPlaying("Nothing playing");
    }

    public void remoteSelectTrack(String videoId, String trackName) {
        syncingFromNetwork = true;
        try {
            currentTrackName = trackName;
            currentVideoId = videoId;
            isPlaying = true;
            musicView.setNowPlaying(trackName);
            musicView.getPlayPauseButton().setIcon(pauseIcon);
            loadVideoInApp(videoId);
        } finally {
            syncingFromNetwork = false;
        }
    }

    public MusicView getMusicView() {
        return musicView;
    }

    public boolean getPlaybackStatus()
    {
        return isPlaying;
    }

    public void releaseBrowserFocus() {
        if (cefBrowser == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            cefBrowser.setFocus(false);
            System.out.println("[JCEF] Native browser focus released");
        });
    }

    public void initCef()
    {
        initCefPlayer();
    }


    public void shutdown() {
        if (playerServer != null) {
            try {
                playerServer.stop(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            if (cefBrowser != null) {
                cefBrowser.setCloseAllowed();
                cefBrowser.close(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (cefClient != null) {
                cefClient.dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (taskbarThumbnail != null) {
            try {
                taskbarThumbnail.dispose();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}