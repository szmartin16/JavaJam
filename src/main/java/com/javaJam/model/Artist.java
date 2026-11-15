package com.javaJam.model;

import java.util.List;

public class Artist {
    public static int counter=1;

    private int id;
    private String name;
    private List<Music> musics;
    private List<Album> albums;

    public Artist(String name) {
        this.name = name;
        this.id = counter++;
    }
}
