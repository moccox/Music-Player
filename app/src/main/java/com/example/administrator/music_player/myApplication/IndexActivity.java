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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.music_player.R;
import com.example.administrator.music_player.data.Music;
import com.example.administrator.music_player.data.MusicList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页
 * 包括：音乐列表、播放模式（未完善）、播放/暂停按钮、播放音乐信息
 */

public class IndexActivity extends Activity {
    //各个界面返回值
    private final int GO_TO_MAIN = 1; //界面1（MainActivity）返回值
    private final int GO_TO_COLOR = 2;//界面2（ChooseColor）返回值

    private ImageButton mplayOrPause;   //播放/暂停按钮
    private ImageButton msettingBt;     //设置按钮
    private ListView mlistView; //歌曲列表
    private TextView mtitle;    //正在播放的歌曲标题
    private TextView martist;   //正在播放的歌曲演唱者

    private ArrayList<Music> mmusicArrayList;           //装Music类的数组列表
    private MediaPlayer mplayer = new MediaPlayer();    // 媒体播放类
    private int mmusicId = 0; //记录播放歌曲的序号，初始化为0
    private boolean flag = true;
    private ListView msettingList; //关于设置的具体菜单
    private int mcolor; //主题颜色
    private LinearLayout mplayingBar; //播放栏
    private RelativeLayout msettingBar; //设置栏


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);
        findViews();
        initMusicList();
        initListView();
        checkMusicfile();
        initSettingList();
        initListener();
    }

    /**组件关联**/
    private void findViews(){
        mplayOrPause = (ImageButton) findViewById(R.id.index_playOrPause);
        mlistView = (ListView) findViewById(R.id.index_listView);
        mtitle = (TextView) findViewById(R.id.index_title);
        mtitle.setSelected(true);   //View太多获取不到焦点，不设置这个不会滚动
        martist = (TextView) findViewById(R.id.index_artist);
        martist.setSelected(true);
        msettingBt = (ImageButton) findViewById(R.id.setting); //设置按钮
        msettingList = (ListView) findViewById(R.id.setting_listView); //设置菜单
        mplayingBar = (LinearLayout) findViewById(R.id.playingBar);
        msettingBar = (RelativeLayout) findViewById(R.id.settingBar);
    }

    /**设置按钮监听**/
    private void initListener(){

        mplayOrPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**播放或者暂停**/

                if(mplayer != null && mplayer.isPlaying()){ //播放改暂停
                    pause();
                    mplayOrPause.setBackgroundResource(R.drawable.index_button_pause);  //按钮外观改成暂停
                }else{
                    if(flag == true){
                        //一开始的播放
                        play(mmusicId);
                        flag = false;
                    }else{
                        //暂停改播放
                        resume();
                        mplayOrPause.setBackgroundResource(R.drawable.index_button_play);   //按钮外观改回播放
                    }
                }
            }
        });

        mlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /**单击该项播放该项**/
                mmusicId = position;
                play(mmusicId);
            }
        });

        msettingBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**单击 弹开/收起 设置列表**/
                if(msettingList. getVisibility() == View.GONE || msettingList.getVisibility() == View.INVISIBLE){
                    //隐藏改显示
                    msettingList.setVisibility(View.VISIBLE);
                }else{
                    //显示改隐藏
                    msettingList.setVisibility(View.INVISIBLE);
                }
            }
        });

        msettingList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0){
                    //设置主题颜色
                    Intent intent = new Intent();
                    intent.setAction("com.example.administrator.music_player.action.Color");
                    intent.addCategory("android.intent.category.DEFAULT");
                    startActivityForResult(intent,GO_TO_COLOR);
                    pause();
                }
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
        mlistView.setAdapter(simpleAapter);     //写入ListView
    }

    /**初始化设置列表**/
    private void initSettingList(){
        /**往设置列表添加item**/
       HashMap<String,String> map = new HashMap<String,String>();
        List<Map<String,String>> listMap = new ArrayList<>();
        map.put("Setting","主题颜色");
        listMap.add(map);
        String TAG = "initSettingList()";
        map = new HashMap<String,String>();
        map.put("Setting","均衡器");
        listMap.add(map);
        String[] from =new String[]{"Setting"};
        int[] to = {R.id.setting_item};
        SimpleAdapter simpleAdapter = new SimpleAdapter(this,listMap,R.layout.settinglist_item,from,to);
        msettingList.setAdapter(simpleAdapter);
    }

    /**检查MusicList是否有歌曲**/
    private void checkMusicfile(){
        if(mmusicArrayList.isEmpty()){  //MusicList中没有Music

            mplayOrPause.setEnabled(false);
            mtitle.setText("");
            martist.setText("");

            //提示没有歌曲
            Toast.makeText(this, "没有歌曲文件", Toast.LENGTH_LONG).show();
        }else{  //MusicList中有Music
            //不管之前有没有设置为不可用，都将按钮重新设置为可用
            mplayOrPause.setEnabled(true);
        }
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

        //将标题和演唱者写入
        String title = mmusicArrayList.get(mmusicId).getMmusicName().toString().trim();
        mtitle.setText(title);
        String artist = mmusicArrayList.get(mmusicId).getMmusicArtist().toString().trim();
        martist.setText(artist);
    }

    /**暂停播放**/
    private void pause(){
        if(mplayer.isPlaying()) mplayer.pause();
    }

    /**继续播放**/
    private void resume(){
        mplayer.start();
    }

    /**重新播放本歌曲（播放完成后）**/
    private void replay(){
        mplayer.start();
    }

    /**界面跳转1：跳转到MainActivity**/
    public void goToMain(View view) {
        Intent intent = new Intent();
        intent.setAction("com.example.administrator.music_player.action.Main");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.putExtra("id",mmusicId);
        int position = mplayer.getCurrentPosition();    //获取播放点
        intent.putExtra("position",position);
        mplayer.stop();
        startActivityForResult(intent,GO_TO_MAIN);
    }

    /**回调结果处理**/
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        if(requestCode == GO_TO_MAIN){   //从播放界面返回
            mmusicId = data.getIntExtra("id",mmusicId);
            int position = data.getIntExtra("position",0);
            play(mmusicId);
            mplayer.seekTo(position);
        }

        if(requestCode == GO_TO_COLOR){    //从选色界面返回
            if(resultCode == 1){
                //有东西
                mcolor = data.getIntExtra("color",mcolor);
                String TAG = "result1";
                Log.i(TAG, "onActivityResult: "+mcolor);
                mplayingBar.setBackgroundColor(mcolor);
                msettingBar.setBackgroundColor(mcolor);
                msettingList.setBackgroundColor(mcolor);
                changeColorOfList(msettingList,mcolor);
            }else{}
            resume();
        }
    }

    /**改变设置选项菜单颜色**/
    private void changeColorOfList(ListView listview,int color){
        for(int position=0;position<listview.getChildCount();++position){
            //获取每一个item，先关联（没有关联不会改变颜色），再改变背景颜色
            listview.getChildAt(position).findViewById(R.id.setting_item).setBackgroundColor(color);
        }
    }
}
