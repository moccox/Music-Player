package com.example.administrator.music_player.Service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.example.administrator.music_player.Data.MusicList;

import java.io.IOException;
import java.util.Random;


public class MusicService extends Service {

    /**操作命令定义**/
    public static final int commandUnknown = -1; //未知命令
    public static final int commandPlay = 0; //播放
    public static final int commandPause = 1; //暂停
    public static final int commandResume = 2; //继续播放
    public static final int commandStop = 3; //停止
    public static final int commandPrevious = 4; //播放上一首
    public static final int commandNext = 5; //播放下一首
    public static final int commandCheckedIsPlaying = 6; //检查是否正在播放
    public static final int commandSeekTo = 7; //从某进度播放
    public static final int commandGetPosition = 8; //获取音乐播放进度
    public static final int commandChangeModel = 9; //改变播放模式
    /**播放状态定义**/
    public static final int statusPlaying = 0; //播放状态
    public static final int statusPaused = 1; //暂停状态
    public static final int statusStoped = 2; //停止状态

    /**播放模式定义**/
    public static final int modelLoop = 0;  //列表循环
    public static final int modelSingleCycle = 1;  //单曲循环
    public static final int modelShufflePlayback = 2;  //随机播放

    private boolean phoneFlag = false;  //来电处理标志

    /**广播标识**/
    public static final String broadcastMusicServiceControl = "MusicService.ACTION_CONTROL"; //控制命令
    public static final String broadcastMusicServiceUpdateStatus = "MusicService.ACTION_UPDATE_STATUS";  //更新状态
    public static final String broadcastMusicServiceUpdateId = "MusicService.ACTION_UPDATE_ID";  //更新musicID


    private int status; //播放状态
    private int model;  //播放模式
    private int musicId = 0;    //当前音乐ID
    public static int musicPosition = 0;

    /**媒体播放类**/
    private MediaPlayer mplayer = new MediaPlayer();

    private CommandReceiver commandReceiver;   //命令广播接收器
    private HeadsetPlusReceiver headsetPlusReceiver;    //耳机拔插接收器

    @Override
    public void onCreate(){
        super.onCreate();
        bindCommandReciver(); //绑定广播接收器，可以接收广播
        status = MusicService.statusStoped;  //初始化播放状态
        model = MusicService.modelLoop; //初始化播放模式
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(new MyPhoneListener(), PhoneStateListener.LISTEN_CALL_STATE);   //监听来电
    }

    @Override
    public void onDestroy(){
        if(mplayer != null){
            mplayer.release();  //释放播放器资源
        }
        unregisterReceiver(commandReceiver);    //解绑广播接收器
        unregisterReceiver(headsetPlusReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind (Intent arg0){
        return null;
    }

    //——类、方法定义——//
    /**命令广播接收类定义**/
    class CommandReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            int command = intent.getIntExtra("command",commandUnknown);//获取命令
            switch (command){
                case commandPlay:   //播放
                    musicId = intent.getIntExtra("id",0);   // 获取歌曲ID
                    Play(musicId);
                    break;
                case commandPause:  //暂停
                    Pause();
                    break;
                case commandResume: //继续播放
                    Resume();
                    break;
                case commandPrevious:   //播放上一首
                    PlayPrevious();
                    sendBroadcastOnIdChanged(musicId);
                    break;
                case commandNext:   //播放下一首
                    PlayNext();
                    sendBroadcastOnIdChanged(musicId);
                    break;
                case commandStop:   //停止播放
                    Stop();
                    break;

                case commandSeekTo: //从某进度播放
                    musicId = intent.getIntExtra("id",0);
                    musicPosition = intent.getIntExtra("position",musicPosition);
                    Play(musicId);
                    mplayer.seekTo(musicPosition);
                    sendBroadcastOnIdChanged(musicId);
                    break;

                case commandCheckedIsPlaying: //检查是否正在播放
                    if(mplayer != null && mplayer.isPlaying()){
                        sendBroadcastOnStatusChanged(MusicService.statusPlaying);
                    }
                    break;
                case commandGetPosition:    //获取播放进度
                    getMusicPosition();
                    break;
                case commandChangeModel:
                    model = intent.getIntExtra("model",model);
                case commandUnknown:    //未知命令处理、默认处理
                default:
                    break;
            }
        }

    }

    /**耳机拔插状态检测广播接收器**/
    class HeadsetPlusReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE); //声音管理
            if(audioManager.isWiredHeadsetOn()){    //耳机插入
                if(status == statusPaused) Resume();    //暂停改播放
            }else{  //拔出耳机
                if(status == statusPlaying) Pause(); //播放改暂停
            }
        }
    }

    /**绑定广播接收器**/
    private void bindCommandReciver(){
        commandReceiver = new CommandReceiver();
        headsetPlusReceiver = new HeadsetPlusReceiver();
        IntentFilter filter1 = new IntentFilter(broadcastMusicServiceControl); //消息过滤
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(commandReceiver,filter1);
        registerReceiver(headsetPlusReceiver,filter2);
    }

    /**发送广播 提醒改变状态**/
    private void sendBroadcastOnStatusChanged(int status){
        Intent intent = new Intent(broadcastMusicServiceUpdateStatus);
        intent.putExtra("status",status);
        intent.putExtra("duration",mplayer.getDuration());
        intent.putExtra("time",mplayer.getCurrentPosition());
        sendBroadcast(intent);
    }

    /**发送广播，提醒音乐ID改变**/
    private void sendBroadcastOnIdChanged(int id){
        Intent intent = new Intent(broadcastMusicServiceUpdateId);
        intent.putExtra("id",id);
        sendBroadcast(intent);
    }

    /**来电监听**/
    private final class MyPhoneListener extends PhoneStateListener{
        @Override
        public void onCallStateChanged(int state,String incommingNumber){
            switch (state){
                case TelephonyManager.CALL_STATE_RINGING:   //来电
                    if(status == MusicService.statusPlaying){   //播放转暂停
                        Pause();
                        phoneFlag = true;
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:   //通话结束
                    if(phoneFlag == true){
                        Resume();
                        phoneFlag = false;
                    }
                    break;
            }
        }
    }

    //——音乐播放相关操作——//

    /**读取音乐文件（文件序号：number）**/
    private void Load(int number){
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

        //注册监听器
        mplayer.setOnCompletionListener(completionListener);
    }

    /**播放结束监听器**/
    MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            if(model == MusicService.modelSingleCycle) {    //单曲循环特判
                Stop();
                Play(musicId);
            }else  {
                PlayNext();
                sendBroadcastOnIdChanged(musicId);
            }
        }
    };

    /**播放音乐文件（文件序号：number）**/
    private void Play(int number){
        //停止播放当前歌曲
        String TAG = "play";
        if(mplayer != null && mplayer.isPlaying()){
            mplayer.stop();
            Log.i(TAG, "play: 停止播放当前歌曲");
        }
        Load(number);   //先读取音乐文件
        Log.i(TAG, "play: 读取音乐文件成功");
        mplayer.start();
        status = MusicService.statusPlaying; //状态改为播放中
        sendBroadcastOnStatusChanged(MusicService.statusPlaying);    //发送状态给Activity
        Log.i(TAG, "play: 播放音乐，id："+musicId);
    }


    /**暂停播放**/
    private void Pause(){
        if(mplayer.isPlaying()){
            mplayer.pause();
            status = MusicService.statusPaused; //状态改为已暂停
            sendBroadcastOnStatusChanged(MusicService.statusPaused); //给Activity发送状态
        }
    }

    /**继续播放**/
    private void Resume(){
        mplayer.start();
        status = MusicService.statusPlaying; //状态改回播放中
        sendBroadcastOnStatusChanged(MusicService.statusPlaying);    //将状态发给Activity
    }

    /**停止播放**/
    private void Stop(){
        mplayer.stop();
        status = MusicService.statusStoped;  //状态改为停止
        sendBroadcastOnStatusChanged(MusicService.statusStoped); //发送状态给Activity
        musicId = 0;
    }
    /**播放上一首歌曲**/
    private void PlayPrevious(){
        if(model == MusicService.modelShufflePlayback){ //随机播放
            if(musicId == 0)  ShufflePlayback(true); //触顶反弹 向后随机
            else ShufflePlayback(false);    //尚未到顶 向前随机
        }else{
            if(musicId == 0){   //第一首
                switch (model){
                    case MusicService.modelLoop://列表循环模式，播放最后一首
                        musicId = MusicList.getMusicList().size()-1;
                        Play(musicId);
                        break;
                    case MusicService.modelSingleCycle: //单曲循环，提示以是第一首歌曲
                        Toast.makeText(MusicService.this, "已是第一首歌曲", Toast.LENGTH_LONG).show();
                }
            }else {
                --musicId;
                Play(musicId);
            }
        }

    }

    /**播放下一首歌曲**/
    private void PlayNext(){
        if(model == MusicService.modelShufflePlayback){ //随机播放
            if(musicId == (MusicList.getMusicList().size()-1)) ShufflePlayback(false);  //触底反弹 向前随机
            else ShufflePlayback(true);     //尚未到底 向后随机
        }else{  //列表循环、单曲循环 播放下一首
            if(musicId == (MusicList.getMusicList().size()-1)){ //最后一首
                switch (model){
                    case  MusicService.modelLoop://列表循环模式，从第一首起播
                        musicId = 0;
                        Play(musicId);
                        break;
                    case MusicService.modelSingleCycle: //单曲循环，提示已是最后一首
                        Toast.makeText(MusicService.this, "已是最后一首歌曲", Toast.LENGTH_LONG).show();
                        break;
                }
            }else{
                ++musicId;
                Play(musicId);
            }
        }

    }

    /**获取播放进度**/
    private void getMusicPosition(){
        musicPosition = mplayer.getCurrentPosition();   //获取播放进度
    }

    /**随机播放歌曲**/
    private void ShufflePlayback( boolean flag){
        int i;
        if(flag == true){   //向后随机

            i = new Random().nextInt((MusicList.getMusicList().size()-1-musicId)); //产生一个从0到id-1之间的随机数
            musicId++;
            musicId += i;
            Play(musicId);
        }
        else{   //向前随机
            i = new Random().nextInt(musicId); //产生一个从0到目前id之间的随机数
            musicId = i;
            Play(musicId);
        }
    }
}
