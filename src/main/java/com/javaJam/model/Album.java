package com.javaJam.model;

import java.util.List;

public class Album {
    public static int counter=1;

    private int id;
    private String name;
    private List<Music> music;
    private String artist;

    public Album(String name) {
        this.name = name;
        this.id =counter++;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Music> getMusic() {
        return music;
    }

    public void setMusic(List<Music> music) {
        this.music = music;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    @Override
    public String toString() {
        return "Album{" +
                "id=" + id +
                ", Name='" + name + '\'' +
                ", Musics=" + music +
                ", Artist='" + artist + '\'' +
                '}';
    }
}
