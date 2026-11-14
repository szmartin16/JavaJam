package com.javaJam.network;

import com.javaJam.controller.MusicController;
import com.javaJam.view.MusicView;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JamManager {

    private Socket relaySocket;
    private PrintWriter out;
    private BufferedReader in;

    private MusicView view;
    private final MusicController musicController;
    private String countStr="0";

    private volatile boolean isHost = false;
    private volatile boolean isConnected = false;
    private volatile String latestState = null;
    private static final String RELAY_SERVER_HOST = "switchback.proxy.rlwy.net";
    private static final int RELAY_SERVER_PORT = 39940;

    public JamManager(MusicController controller) {
        this.musicController = controller;
        this.view = controller.getMusicView();
    }

    // HOST MODE
    public void startHosting(String sessionId) {
        isHost = true;

        new Thread(() -> {
            try {
                relaySocket = new Socket(RELAY_SERVER_HOST, RELAY_SERVER_PORT);
                out = new PrintWriter(relaySocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(relaySocket.getInputStream(), StandardCharsets.UTF_8));

                out.println("AUTH|HOST|" + sessionId);
                isConnected = true;

                System.out.println("[JAM HOST] Connected to relay server for session: " + sessionId);
                SwingUtilities.invokeLater(() -> {
                    view.getJamButton().setBackground(view.accentColor);
                    if (view.getClientCountField() != null) {
                        view.getClientCountField().setText("0");
                    }
                });

                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println("[JAM HOST] Received from relay -> " + line);
                    handleIncomingCommand(line);
                }

            } catch (IOException e) {
                System.err.println("[JAM HOST ERROR] Relay connection lost: " + e.getMessage());
            } finally {
                disconnect();
            }
        }, "Jam-Host-Relay-Thread").start();
    }

    // CLIENT MODE
    public void joinHost(String sessionId) {
        isHost = false;

        new Thread(() -> {
            try {
                relaySocket = new Socket(RELAY_SERVER_HOST, RELAY_SERVER_PORT);
                out = new PrintWriter(relaySocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(relaySocket.getInputStream(), StandardCharsets.UTF_8));

                out.println("AUTH|CLIENT|" + sessionId);
                isConnected = true;

                System.out.println("[JAM CLIENT] Successfully joined session via relay: " + sessionId);
                SwingUtilities.invokeLater(() -> {
                    musicController.initCef();
                    view.getJamButton().setBackground(view.accentColor);
                    if (view.getClientCountField() != null) {
                        view.getClientCountField().setText(countStr);
                    }
                });

                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println("[JAM CLIENT] Received from host via relay -> " + line);
                    handleIncomingCommand(line);
                }

            } catch (IOException e) {
                System.err.println("[JAM CLIENT ERROR] Connection dropped: " + e.getMessage());
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(musicController.getMusicView(), "Connection lost to session.")
                );
            } finally {
                disconnect();
            }
        }, "Jam-Client-Relay-Thread").start();
    }

    public void disconnect() {
        isConnected = false;
        try {
            if (relaySocket != null) relaySocket.close();
        } catch (IOException ignored) {}

        SwingUtilities.invokeLater(() -> {
            view.getJamButton().setBackground(view.buttonDark);
            if (view.getClientCountField() != null) {
                view.getClientCountField().setText("0");
            }
        });
        System.out.println("[JAM] Disconnected from relay session.");
    }

    // SEND COMMAND
    public void sendToServer(String command) {
        if (isConnected && out != null) {
            out.println(command);
        }
    }

    public void sendSelectTrackCommand(String videoId, String trackName) {
        String encodedName = Base64.getEncoder().encodeToString(trackName.getBytes(StandardCharsets.UTF_8));
        String command = "CONTROL|SELECT_TRACK|" + videoId + "|" + encodedName;

        // Execute locally first for instant UI responsiveness, then send to relay
        handleControlCommand(command);
        sendToServer(command);
    }

    public void sendControlCommand(String action) {
        sendControlCommand(action, 0.0);
    }

    public void sendControlCommand(String action, double position) {
        String command = "CONTROL|" + action + "|" + position;

        handleControlCommand(command);
        sendToServer(command);
    }

    public void broadcastState(String videoId, double position, boolean playing, String trackName) {
        String encodedName = Base64.getEncoder().encodeToString(trackName.getBytes(StandardCharsets.UTF_8));
        String state = String.join("|", "STATE", videoId, String.valueOf(position), String.valueOf(playing), encodedName);

        if (isHost) {
            latestState = state;
            sendToServer(state);
        } else {
            sendToServer(state);
        }
    }

    // RECEIVE COMMAND
    private void handleIncomingCommand(String cmd) {
        if (cmd.startsWith("STATE|")) {
            String[] parts = cmd.split("\\|", 5);
            if (parts.length != 5) return;

            try {
                String videoId = parts[1];
                double position = Double.parseDouble(parts[2]);
                boolean playing = Boolean.parseBoolean(parts[3]);
                String trackName = new String(Base64.getDecoder().decode(parts[4]), StandardCharsets.UTF_8);

                SwingUtilities.invokeLater(() ->
                        musicController.syncFromHost(videoId, position, playing, trackName)
                );
            } catch (Exception e) {
                System.err.println("[JAM ERROR] Failed to parse state payload: " + e.getMessage());
            }
        } else if (cmd.startsWith("CONTROL|")) {
            handleControlCommand(cmd);

            if (isHost) {
                sendToServer(cmd);
            }
        } else if (cmd.startsWith("STATUS|CLIENT_COUNT|")) {
            String[] parts = cmd.split("\\|");
            if (parts.length >= 3) {
                countStr = parts[2];
                SwingUtilities.invokeLater(() -> {
                    if (view.getClientCountField() != null) {
                        view.getClientCountField().setText(countStr);
                    }
                });
            }
        }
    }

    private void handleControlCommand(String command) {
        String[] parts = command.split("\\|", 4);
        if (parts.length < 2 || !parts[0].equals("CONTROL")) return;

        String action = parts[1];

        if (action.equals("SELECT_TRACK") && parts.length >= 4) {
            String videoId = parts[2];
            String trackName = new String(Base64.getDecoder().decode(parts[3]), StandardCharsets.UTF_8);
            SwingUtilities.invokeLater(() -> musicController.remoteSelectTrack(videoId, trackName));
            return;
        }

        double position = 0.0;
        if (parts.length >= 3) {
            try { position = Double.parseDouble(parts[2]); } catch (NumberFormatException ignored) {}
        }

        final double finalPos = position;
        SwingUtilities.invokeLater(() -> {
            switch (action) {
                case "PLAY": musicController.remotePlay(); break;
                case "PAUSE": musicController.remotePause(); break;
                case "SEEK": musicController.remoteSeek(finalPos); break;
                case "NEXT": musicController.remoteNext(); break;
                case "PREVIOUS": musicController.remotePrevious(); break;
                case "SHUFFLE": musicController.remoteShuffle(); break;
                case "STOP": musicController.remoteStop(); break;
            }
        });
    }

    public String generateSessionId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(8);
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public boolean isConnected() { return isConnected; }
    public boolean isHost() { return isHost; }
}