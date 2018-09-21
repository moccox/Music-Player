package com.example.administrator.music_player.myApplication;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;

import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.example.administrator.music_player.R;
import com.example.administrator.music_player.data.Music;
import com.example.administrator.music_player.data.MusicList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private ImageButton mpreviousBt;
    private ImageButton mplayOrPauseBt;
    private ImageButton mstopBt;
    private ImageButton mnextBt;
    private ListView mlist;

    private ArrayList<Music> mmusicArrayList;           //装Music类的数组列表
    private MediaPlayer mplayer = new MediaPlayer();    // 媒体播放类
    private int mmusicId = 0;   //记录播放歌曲的序号，初始化为0
    private int mmusicPosition; //音乐播放点，记录歌曲播放进度

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        initListener();
        initMusicList();
        initListView();
        checkMusicfile();
        initPosition();
    }

    /**组件关联**/
    private void findViews(){
        mpreviousBt = (ImageButton) findViewById(R.id.previousButton);
        mplayOrPauseBt = (ImageButton) findViewById(R.id.playButton);
        mstopBt = (ImageButton) findViewById(R.id.stopButton);
        mnextBt = (ImageButton) findViewById(R.id.nextButton);
        mlist = (ListView) findViewById(R.id.myListView);
    }

    /**设置按钮监听**/
    private void initListener(){
        mpreviousBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**播放上一首歌曲**/
                if(mmusicId == 0){
                    Toast.makeText(MainActivity.this, "已是第一首歌曲", Toast.LENGTH_LONG).show();
                }else {
                    --mmusicId;
                    play(mmusicId);
                }
            }
        });

        mplayOrPauseBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**播放或者暂停**/
                if(mplayer != null && mplayer.isPlaying()){ //播放改暂停
                    pause();
                    mplayOrPauseBt.setBackgroundResource(R.drawable.button_pause);  //按钮外观改成暂停
                }else{
                    if(mmusicId == 0){
                        //一开始的播放
                        play(mmusicId);
                    }else{
                        //暂停改播放
                        resume();
                        mplayOrPauseBt.setBackgroundResource(R.drawable.button_play);   //按钮外观改回播放
                    }
                }
            }
        });

        mstopBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**停止播放**/
                stop();
            }
        });

        mnextBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**播放下一首歌曲**/
                if(mmusicId == (MusicList.getMusicList().size()-1)){
                    Toast.makeText(MainActivity.this, "已是最后一首歌曲", Toast.LENGTH_LONG).show();
                }else{
                    ++mmusicId;
                    play(mmusicId);
                }
            }
        });

        mlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /**单击该项播放该项**/
                mmusicId = position;
                play(mmusicId);
                mplayOrPauseBt.setBackgroundResource(R.drawable.button_play);   //按钮外观改回播放
            }
        });
    }

    /**初始化音乐列表（ArrayList<Music>）**/
    private void initMusicList(){
        mmusicArrayList = MusicList.getMusicList();
        String TAG = "initMusicList";

        if(mmusicArrayList.isEmpty()){
            /**使用游标Cursor获取文件中所有音乐文件的信息**/

            Log.i(TAG, "initMusicList: MusicList为空，开始查找音乐文件");

            Cursor mmusicCursor = this.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.DURATION,
                            MediaStore.Audio.Media.ALBUM,
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.DATA,
                            MediaStore.Audio.Media.DISPLAY_NAME
                    }, null, null,
                    MediaStore.Audio.AudioColumns.TITLE);

            /**获取每项信息的下标**/
            Log.i(TAG,"initMusicList: 查找音乐文件成功");

            if(mmusicCursor != null){
                Log.i(TAG, "Cursor不为空，开始获取各个文件的下标");
                int indexTitle = mmusicCursor.getColumnIndex( MediaStore.Audio.AudioColumns.TITLE); //标题
                int indexArtist = mmusicCursor.getColumnIndex( MediaStore.Audio.AudioColumns.ARTIST);   //艺术家
                int indexTotalTime = mmusicCursor.getColumnIndex( MediaStore.Audio.AudioColumns.DURATION);  //总时长
                int indexPath = mmusicCursor.getColumnIndex( MediaStore.Audio.AudioColumns.DATA);   //路径
                /**将游标中的每一个音乐文件的信息逐个填入Music类中**/
                for(mmusicCursor.moveToFirst();!mmusicCursor.isAfterLast();mmusicCursor.moveToNext()){ //判断条件为未指向尾部的下一位(NULL)
                    Log.i(TAG, "initMusicList: "+mmusicCursor.getString(indexTitle));
                    String strTitle = mmusicCursor.getString(indexTitle);
                    String strArtist = mmusicCursor.getString(indexArtist);
                    String strTotalTime = mmusicCursor.getString(indexTotalTime);
                    String strPath = mmusicCursor.getString(indexPath);

                    if (strArtist.equals("<unknown>")) strArtist = "无艺术家"; //艺术家特判

                    Music music = new Music(strTitle,strArtist,strPath,strTotalTime);
                    mmusicArrayList.add(music);
                }
            }

        }
    }

    /**初始化ListView（将musicArrayList的内容写入ListView中）**/
    private void initListView(){
        List<Map<String, String>> listMap = new ArrayList<>();  //存放Map键值对的数组列表
        HashMap<String, String> map;    //键值对
        SimpleAdapter simpleAapter; //适配器



        for(Music music : mmusicArrayList){ //遍历已知长度的数组（musicArrayList）
            map = new HashMap<String, String>(); //每次循环都必须新建一个map，不然listMap中的内容为都为最后一次添加进map中的内容
            map.put("musicName", music.getMmusicName());
            map.put("musicArtist",music.getMmusicArtist());
            listMap.add(map);
        }

        String[] from = new String[]{"musicName","musicArtist"};
        int[] to = {R.id.itemTitle,R.id.itemArtist};
        simpleAapter = new SimpleAdapter(this,listMap,R.layout.listview_item,from,to);  //数组列表写入适配器
        mlist.setAdapter(simpleAapter);     //写入ListView
    }

    /**检查MusicList是否有歌曲**/
    private void checkMusicfile(){
        if(mmusicArrayList.isEmpty()){  //MusicList中没有Music
            //各按钮设置不可用
            mnextBt.setEnabled(false);
            mplayOrPauseBt.setEnabled(false);
            mpreviousBt.setEnabled(false);
            mstopBt.setEnabled(false);

            //提示没有歌曲
            Toast.makeText(this, "没有歌曲文件", Toast.LENGTH_LONG).show();
        }else{  //MusicList中有Music
            //不管之前有没有设置为不可用，都将按钮重新设置为可用
            mnextBt.setEnabled(true);
            mplayOrPauseBt.setEnabled(true);
            mstopBt.setEnabled(true);
            mpreviousBt.setEnabled(true);
        }
    }

    /**初始化歌曲播放进度**/
    private void initPosition(){
        Intent intent = getIntent();
        mmusicId = intent.getIntExtra("id",0);
        mmusicPosition = intent.getIntExtra("position",0);
        play(mmusicId);
        mplayer.seekTo(mmusicPosition);
    }

    /**按下返回键，返回首页**/
    @Override
    public void onBackPressed(){
        Intent intent = new Intent();
        intent.putExtra("id",mmusicId);
        mmusicPosition = mplayer.getCurrentPosition();
        intent.putExtra("position",mmusicPosition);
        setResult(1,intent);
        mplayer.stop();
        finish();
    }

    /**读取音乐文件（文件序号：number）**/
    private void load(int number){
        String TAG = "load";
        try{
            Log.i(TAG, "load: 开始读取音乐文件");
            mplayer.reset();
            mplayer.setDataSource(MusicList.getMusicList().get(number).getMmusicPath());
            mplayer.prepare();
            Log.i(TAG, "load: 读取音乐文件完毕");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**播放音乐文件（文件序号：number）**/
    private void play(int number){
        //停止播放当前歌曲
        String TAG = "play";
        if(mplayer != null && mplayer.isPlaying()){
            mplayer.stop();
            Log.i(TAG, "play: 停止播放当前歌曲");
        }
        load(number);   //先读取音乐文件
        Log.i(TAG, "play: 读取音乐文件成功");
        mplayer.start();
        Log.i(TAG, "play: 播放音乐，id："+mmusicId);
    }

    /**暂停播放**/
    private void pause(){
        if(mplayer.isPlaying()) mplayer.pause();
    }

    /**继续播放**/
    private void resume(){
        mplayer.start();
    }

    /**停止播放**/
    private void stop(){
        mplayer.stop();
        mmusicId = 0;
    }

    /**重新播放本歌曲（播放完成后）**/
    private void replay(){
        mplayer.start();
    }
}
