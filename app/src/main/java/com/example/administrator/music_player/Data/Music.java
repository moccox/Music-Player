package com.example.administrator.music_player.Data;


public class Music {
    private String mmusicName;
    private String mmusicArtist;
    private String mmusicPath;
    private String mmusicDuration;  //音乐时长

    /**构造函数，初始化音乐标题、艺术家、文件路径、时长**/
    public Music(String musicName,String musicArtist,String musicPath,String musicDuration){
        this.mmusicName = musicName;
        this.mmusicArtist = musicArtist;
        this.mmusicPath = musicPath;
        this.mmusicDuration = musicDuration;
    }

    /**获取各项属性**/

    public  String getMmusicName(){
        return  mmusicName;
    }

    public String getMmusicArtist(){
        return  mmusicArtist;
    }

    public  String getMmusicPath(){
        return mmusicPath;
    }

    public String getMmusicDuration(){
        return mmusicDuration;
    }
}
