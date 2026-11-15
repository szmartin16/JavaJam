package com.javaJam.model;

public class Music {
    public static int counter=1;

    private int id;
    private String name;
    private String album;
    private String artist;
    private String videoUrl;
    private int length;

    public Music(String name, String album, String artist) {
        this.name = name;
        this.album = album;
        this.artist = artist;
        this.id = counter++;
    }

    public Music(String name, String
                 videoUrl) {
        this.name = name;
        this.videoUrl = videoUrl;
        this.id = counter++;
    }

    public String getUrl() {
        return videoUrl;
    }

    public void setUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public Music(String name, String album, String artist, int length) {
        this.name = name;
        this.album = album;
        this.artist = artist;
        this.id = counter++;
        this.length =length;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public String toString() {
        return "Music{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", album='" + album + '\'' +
                ", artist='" + artist + '\'' +
                ", length=" + length +
                '}';
    }
}
