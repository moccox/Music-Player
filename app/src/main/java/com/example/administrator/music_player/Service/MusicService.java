package com.example.administrator.music_player.Service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.example.administrator.music_player.Data.MusicList;

import java.io.IOException;


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
    /**播放状态定义**/
    public static final int statusPlaying = 0; //播放状态
    public static final int statusPaused = 1; //暂停状态
    public static final int statusStoped = 2; //停止状态
    public static final int statusCompleted = 4; //播放结束
    /**广播标识**/
    public static final String broadcastMusicServiceControl = "MusicService.ACTION_CONTROL"; //控制命令
    public static final String broadcastMusicServiceUpdateStatus = "MusicService.ACTION_UPDATE";  //更新状态给Main


    private int status; //播放状态
    private int musicId = 0;    //当前音乐ID
    public int musicPosition = 0;

    /**媒体播放类**/
    private MediaPlayer mplayer = new MediaPlayer();

    private CommandReceiver receiver;   //命令广播接收器

    @Override
    public void onCreate(){
        super.onCreate();
        bindCommandReciver(); //绑定广播接收器，可以接收广播
        status = MusicService.statusStoped;  //初始化播放状态
    }

    @Override
    public void onDestroy(){
        if(mplayer != null){
            mplayer.release();  //释放播放器资源
        }
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
                    break;
                case commandNext:   //播放下一首
                    PlayNext();
                    break;
                case commandStop:   //停止播放
                    Stop();
                    break;

                case commandSeekTo: //从某进度播放
                    musicId = intent.getIntExtra("id",0);
                    Play(musicId);
                    mplayer.seekTo(musicPosition);
                    break;

                case commandCheckedIsPlaying: //检查是否正在播放
                    if(mplayer != null && mplayer.isPlaying()){
                        sendBroadcastOnStatusChanged(MusicService.statusPlaying);
                    }
                    break;
                case commandGetPosition:
                    getMusicPosition();
                    break;
                case commandUnknown:    //未知命令处理、默认处理
                default:
                    break;
            }
        }

    }

    /**绑定广播接收器**/
    private void bindCommandReciver(){
        receiver = new CommandReceiver();
        IntentFilter filter = new IntentFilter(broadcastMusicServiceControl); //消息过滤
        registerReceiver(receiver,filter);
    }

    /**发送广播 提醒改变状态**/
    private void sendBroadcastOnStatusChanged(int status){
        Intent intent = new Intent(broadcastMusicServiceUpdateStatus);
        intent.putExtra("status",status);
        sendBroadcast(intent);
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
            if(mp.isLooping()){
                Replay();
            }else{
                sendBroadcastOnStatusChanged(MusicService.statusCompleted);
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
        if(musicId == 0){
            Toast.makeText(MusicService.this, "已是第一首歌曲", Toast.LENGTH_LONG).show();
        }else {
            --musicId;
            Play(musicId);
        }
    }

    /**播放下一首歌曲**/
    private void PlayNext(){
        if(musicId == (MusicList.getMusicList().size()-1)){
            Toast.makeText(MusicService.this, "已是最后一首歌曲", Toast.LENGTH_LONG).show();
        }else{
            ++musicId;
            Play(musicId);
        }
    }

    /**获取播放进度**/
    private void getMusicPosition(){
        musicPosition = mplayer.getCurrentPosition();   //获取播放进度
    }
    /**重新播放本歌曲（播放完成后）**/
    private void Replay(){
        mplayer.start();
        status = MusicService.statusPlaying;
        sendBroadcastOnStatusChanged(MusicService.statusPlaying);
    }
}
