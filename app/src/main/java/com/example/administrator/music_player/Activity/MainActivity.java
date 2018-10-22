package com.example.administrator.music_player.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.music_player.R;
import com.example.administrator.music_player.Data.Music;
import com.example.administrator.music_player.Data.MusicList;
import com.example.administrator.music_player.Service.MusicService;

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
    private int mmusicId;   //记录播放歌曲的序号，初始化为0
    private int status = MusicService.statusStoped; //播放状态
    private int mcolor; //主题颜色
    private int mduration;   //当前歌曲的时长
    private int mtime;   //当前歌曲播放进度

    //进度条控制常量
    private static final int PROGRESS_INCREASE = 0; //自增
    private static final int PROGRESS_PAUSE = 1;    //暂停自增
    private static final int PROGRESS_RESET = 2;    //重新自增


    private TextView mtitle;    //当前歌曲
    private TextView martist;   //当前演唱者
    private SeekBar mvoiceSeekBar;  //音量控制条
    private LinearLayout mplayingBar;    //进度条及时间
    private LinearLayout mcontrolBar;   //播放栏
    private LinearLayout mimformationBar; //显示栏（放当前歌曲和演唱者）
    private SeekBar mseekBar;   //进度条
    private Handler mseekBarHandler; //进度条线程
    private TextView textViewTime;  //显示播放进度
    private TextView textViewDuration;  //显示歌曲时长

    private boolean putTime = false;    //第一次初始化进度flag，不需要把mtime放进seekTo的包（让MusicService跳转到从index过来时记录下的进度，而不是mtime）
    private AudioManager maudioManager;
    private StatusChangeReceiver statusChangeReceiver; //状态改变广播接收器
    private IdChangeReceiver idChangeReceiver; //音乐ID改变接收广播
    private VolumeChangeReciver voiceChangeReciver; //音量硬件改变接收广播

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        initVoiceSeekBar(); //初始化音量进度条应放在设置音量进度条监听前，不然设置监听时由于进度条一开始都是从0开始，所以会将音量改成0
        initListener();
        initMusicList();
        initListView();
        checkMusicfile();
        bindStatusChangeReceiver();
        initPositionAndColor();
        initSeekBarHandler();
    }

    @Override
    protected void onDestroy(){
        unregisterReceiver(statusChangeReceiver);
        unregisterReceiver(idChangeReceiver);
        unregisterReceiver(voiceChangeReciver);
        super.onDestroy();
    }
    //——类、方法定义——//

    /**状态改变广播接收器定义**/
    class StatusChangeReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            mseekBarHandler.removeMessages(PROGRESS_INCREASE);
            mtime = intent.getIntExtra("time", MusicService.musicPosition);
            mduration = intent.getIntExtra("duration", 0);
            status = intent.getIntExtra("status",-1);
            switch(status){
                case MusicService.statusPlaying:    //播放中
                    mseekBar.setProgress(mtime);
                    mseekBar.setMax(mduration);
                   // textViewTime.setText(formatTime(mtime));
                    textViewDuration.setText(formatTime(mduration));
                    mseekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000L);
                    mplayOrPauseBt.setBackgroundResource(R.drawable.button_pause_main);  //按钮外观改成暂停
                    break;
                case MusicService.statusPaused:     //暂停
                    mseekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);   //进度条暂停
                    mplayOrPauseBt.setBackgroundResource(R.drawable.button_play_main);   //按钮外观改回播放
                    break;
                case MusicService.statusStoped:     //停止
                    mtime = 0;
                    mduration = 0;
                    textViewTime.setText(formatTime(mtime));
                    textViewDuration.setText(formatTime(mduration));
                    mseekBarHandler.sendEmptyMessage(PROGRESS_RESET);
                    mplayOrPauseBt.setBackgroundResource(R.drawable.button_play_main);   //按钮外观改回播放
                    break;
                default:    //其他情况
                    break;
            }
        }
    }

    /**音乐ID改变广播接收器定义**/
    class IdChangeReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            mmusicId=intent.getIntExtra("id",mmusicId);
            String title = mmusicArrayList.get(mmusicId).getMmusicName().toString().trim();
            mtitle.setText(title);
            String artist = mmusicArrayList.get(mmusicId).getMmusicArtist().toString().trim();
            martist.setText(artist);
        }
    }

    /**硬件调节音量接收器定义**/
    private class VolumeChangeReciver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")){
                int currentVolume = maudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);   //获取当前音量
                mvoiceSeekBar.setProgress(currentVolume);   //将音量设置为当前进度
            }
        }
    }

    /**绑定广播接收器**/
    private void bindStatusChangeReceiver(){
        statusChangeReceiver = new StatusChangeReceiver();
        idChangeReceiver = new IdChangeReceiver();
        voiceChangeReciver = new VolumeChangeReciver();
        IntentFilter filter1 = new IntentFilter(MusicService.broadcastMusicServiceUpdateStatus); //消息过滤
        IntentFilter filter2 = new IntentFilter(MusicService.broadcastMusicServiceUpdateId);    //消息过滤
        IntentFilter filter3 = new IntentFilter("android.media.VOLUME_CHANGED_ACTION"); //消息过滤
        registerReceiver(statusChangeReceiver,filter1);
        registerReceiver(idChangeReceiver,filter2);
        registerReceiver(voiceChangeReciver,filter3);
    }

    /**发送命令广播**/
    private void sendBroadcastOnCommand(int command){
        Intent intent = new Intent(MusicService.broadcastMusicServiceControl);
        intent.putExtra("command",command);
        switch(command){
            case MusicService.commandPlay:  //播放功能需加装音乐ID
                intent.putExtra("id",mmusicId);
                break;
            case MusicService.commandSeekTo:    //从某进度开始播放，加装音乐ID和进度position
                intent.putExtra("id",mmusicId);
                if(putTime == true) intent.putExtra("position",mtime);
                break;
            case MusicService.commandPrevious: //其他功能皆不需要加装内容
            case MusicService.commandNext:
            case MusicService.commandPause:
            case MusicService.commandStop:
            case MusicService.commandResume:
            case MusicService.commandCheckedIsPlaying:
            default:
                break;
        }
        sendBroadcast(intent);
    }

    /**组件关联**/
    private void findViews(){
        mpreviousBt = (ImageButton) findViewById(R.id.previousButton);  //上一首按钮
        mplayOrPauseBt = (ImageButton) findViewById(R.id.playButton);   //播放/暂停按钮
        mstopBt = (ImageButton) findViewById(R.id.stopButton);          //停止按钮
        mnextBt = (ImageButton) findViewById(R.id.nextButton);          //下一首按钮
        mlist = (ListView) findViewById(R.id.myListView);               //列表
        mtitle = (TextView) findViewById(R.id.main_title);              //歌曲名字
        mtitle.setSelected(true);
        martist = (TextView) findViewById(R.id.main_artist);            //演唱者
        martist.setSelected(true);
        mvoiceSeekBar = (SeekBar) findViewById(R.id.voiceSeekBar);      //音量条件进度条
        mplayingBar = (LinearLayout) findViewById(R.id.playingSeek);    //播放栏（包含进度条和时间显示）
        mcontrolBar = (LinearLayout) findViewById(R.id.controlBar);     //控制栏（包含四个按钮）
        mimformationBar = (LinearLayout) findViewById(R.id.show);       //信息栏（包含歌曲名字和演唱者）
        mseekBar = (SeekBar) findViewById(R.id.playingSeekBar);         //进度条
        textViewTime = (TextView) findViewById(R.id.time);                      //显示当前进度时间
        textViewDuration = (TextView) findViewById(R.id.duration);              //显示歌曲时长
    }

    /**设置按钮监听**/
    private void initListener(){
        mpreviousBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**播放上一首歌曲**/
                if(mmusicId == 0);
                else --mmusicId;
                sendBroadcastOnCommand(MusicService.commandPrevious);
                String title = mmusicArrayList.get(mmusicId).getMmusicName().toString().trim();
                mtitle.setText(title);
                String artist = mmusicArrayList.get(mmusicId).getMmusicArtist().toString().trim();
                martist.setText(artist);
            }
        });

        mplayOrPauseBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**播放或者暂停**/
                switch(status){
                    case MusicService.statusPaused: //暂停改播放
                        sendBroadcastOnCommand(MusicService.commandResume);
                        break;
                    case MusicService.statusPlaying:    //播放改暂停
                        sendBroadcastOnCommand(MusicService.commandPause);
                        break;
                    case MusicService.statusStoped:     //第一次播放
                        sendBroadcastOnCommand(MusicService.commandPlay);
                        String title = mmusicArrayList.get(mmusicId).getMmusicName().toString().trim();
                        mtitle.setText(title);
                        String artist = mmusicArrayList.get(mmusicId).getMmusicArtist().toString().trim();
                        martist.setText(artist);
                        break;
                    default:
                        break;
                }
            }
        });

        mstopBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**停止播放**/
                sendBroadcastOnCommand(MusicService.commandStop);
            }
        });

        mnextBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**播放下一首歌曲**/
                if(mmusicId == (MusicList.getMusicList().size()-1));
                else ++mmusicId;
                sendBroadcastOnCommand(MusicService.commandNext);
                String title = mmusicArrayList.get(mmusicId).getMmusicName().toString().trim();
                mtitle.setText(title);
                String artist = mmusicArrayList.get(mmusicId).getMmusicArtist().toString().trim();
                martist.setText(artist);
            }
        });

        /**列表项目点击事件**/
        mlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /**单击该项播放该项**/
                mmusicId = position;
               sendBroadcastOnCommand(MusicService.commandPlay);
                String title = mmusicArrayList.get(mmusicId).getMmusicName().toString().trim();
                mtitle.setText(title);
                String artist = mmusicArrayList.get(mmusicId).getMmusicArtist().toString().trim();
                martist.setText(artist);
            }
        });

        /**进度条状态改变事件**/
        mseekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {    //进度条正在拖动
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {     //进度条开始拖动
                mseekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);   //进度条暂停自增
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {   //进度条拖动结束
                Log.i("stop seeking", "onStopTrackingTouch: "+ mmusicId);
                if(status != MusicService.statusStoped){
                    mtime =seekBar.getProgress();
                    textViewTime.setText(formatTime(mtime));
                    sendBroadcastOnCommand(MusicService.commandSeekTo);
                    if(status == MusicService.statusPaused){//如果此时歌曲是暂停状态，先恢复播放（原因如下↓），然后再暂停播放
                        sendBroadcastOnCommand(MusicService.commandResume); //每次拖动进度条后mtime会变成0，只有resume才恢复
                        sendBroadcastOnCommand(MusicService.commandPause);
                    }
                }
                if(status == MusicService.statusPlaying){
                    //如果此时歌曲正在播放，进度条恢复自增
                    sendBroadcastOnCommand(MusicService.commandResume);
                    mseekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE,1000);
                }
            }
        });

        /**音量调节进度条状态改变事件**/
        mvoiceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {    //拖动过程改变音量
                    maudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,progress,0); //将当前进度设置为系统音量
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {  //进度条拖动结束
                    int currentVolume = maudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);   //获取当前音量
                    mvoiceSeekBar.setProgress(currentVolume);   //将音量设置为当前进度
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


    /**初始化歌曲播放进度和主题颜色**/
    private void initPositionAndColor(){
        Intent intent = getIntent();
        mmusicId = intent.getIntExtra("id",0);
        mcolor = intent.getIntExtra("color",-587202560);
        mtime = MusicService.musicPosition;
        textViewTime.setText(formatTime(mtime));
        sendBroadcastOnCommand(MusicService.commandSeekTo);
        mseekBar.setProgress(mtime);
        sendBroadcastOnCommand(MusicService.commandResume);
        putTime = true; //已经初始化进度条了，以后使用seekTo命令都要将进度mtime附加进去
        String title = mmusicArrayList.get(mmusicId).getMmusicName().toString().trim();
        mtitle.setText(title);
        String artist = mmusicArrayList.get(mmusicId).getMmusicArtist().toString().trim();
        martist.setText(artist);

        mvoiceSeekBar.setBackgroundColor(mcolor);
        mplayingBar.setBackgroundColor(mcolor);
        mcontrolBar.setBackgroundColor(mcolor);
        mimformationBar.setBackgroundColor(mcolor);
    }

    /**创建菜单**/
    public boolean onCreateOptionsMenu(Menu menu){
        this.getMenuInflater().inflate(R.menu.main,menu);
        return true;
    }

    /**菜单点击事件**/
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.menu_about:   //关于
                new AlertDialog.Builder(MainActivity.this).setTitle("关于播放器").setMessage("这是一个时尚的音乐播放器").show();
                break;
            case R.id.menu_exit:    //退出
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("退出")
                        .setMessage("您即将退出本程序，是否继续？")
                        .setPositiveButton("是",new android.content.DialogInterface.OnClickListener(){   //确定按钮
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                System.exit(0);
                            }
                        })
                        .setNegativeButton("否",new android.content.DialogInterface.OnClickListener(){   //取消按钮
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**按下返回键，返回首页**/
    @Override
    public void onBackPressed(){
        Intent intent = new Intent();
        sendBroadcastOnCommand(MusicService.commandGetPosition);
        intent.putExtra("id",mmusicId);
        setResult(1,intent);
        finish();
    }

    /**时间转换为标准时间**/
    private String formatTime(int msec) {
        int minute = msec / 1000 / 60;  //分钟
        int second = msec / 1000 % 60;  //秒
        String minuteString;
        String secondString;
        if (minute < 10) {
            minuteString = "0" + minute;    //凑齐两位数字
        } else {
            minuteString = "" + minute;
        }
        if (second < 10) {
            secondString = "0" + second;
        } else {
            secondString = "" + second;
        }
        return minuteString + ":" + secondString;
    }

    /**初始化进度条**/
    private void initSeekBarHandler() {
        mseekBarHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case PROGRESS_INCREASE: //自增
                        if (mseekBar.getProgress() < mduration) {
                            mseekBar.setProgress(mtime);    //进度条从某音乐进度开始
                            mseekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);   //100毫秒（1秒）后发送自增命令
                            textViewTime.setText(formatTime(mtime));
                            mtime += 1000;  //进度+1000毫秒（1秒）
                        }
                        break;
                    case PROGRESS_PAUSE:    //暂停
                        mseekBarHandler.removeMessages(PROGRESS_INCREASE);
                        break;
                    case PROGRESS_RESET:    //重新开始
                        //重置进度条画面
                        mseekBarHandler.removeMessages(PROGRESS_INCREASE);
                        mseekBar.setProgress(0);
                        textViewTime.setText("00:00");
                        break;
                }
            }
        };
    }

    /**初始化音量调节进度条**/
    private void initVoiceSeekBar(){
        maudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = maudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC); //获取系统最大音量
        mvoiceSeekBar.setMax(maxVolume);    //将音量调节进度条的最大值设置为最大音量
        int currentVolum = maudioManager.getStreamVolume(AudioManager.STREAM_MUSIC); //获取当前音量
        mvoiceSeekBar.setProgress(currentVolum);    //将音量进度设置为当前音量
    }
}
