package com.example.administrator.music_player.Data;

import java.util.ArrayList;

/**
 * MusicList类采用单一实例，即此类的实例只能有一个
 * 该类的实例为唯一的私有的静态的 ArrayList<Music> 实例
 * 要获取实例中的信息，只能通过函数（getMusicList）
 **/

public class MusicList {
    private static ArrayList<Music> mmusicArray = new ArrayList<>();

    private  MusicList(){}

    public static ArrayList<Music> getMusicList(){
        return mmusicArray;
    }
}
