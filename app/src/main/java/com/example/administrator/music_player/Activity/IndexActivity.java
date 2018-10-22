package com.example.administrator.music_player.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.Switch;
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
import java.util.Timer;
import java.util.TimerTask;

/**
 * 首页
 * 包括：音乐列表、播放模式（未完善）、播放/暂停按钮、播放音乐信息
 */

public class IndexActivity extends Activity {
    //各个界面返回值
    private final int GO_TO_MAIN = 1; //界面1（MainActivity）返回值
    private final int GO_TO_COLOR = 2;//界面2（ChooseColor）返回值

    private static boolean isExit = false;  //确定退出程序标志
    private static boolean isSleep =false;  //启动定时器标志

    private Timer msleepTimer;  //定时器定时
    private int sleepMinue = 20;    //默认定时关闭时间20分钟

    private ImageButton mplayOrPause;   //播放/暂停按钮
    private ImageButton mplayModel; //播放模式按钮
    private ImageButton msettingBt;     //设置按钮
    private ImageButton msearchBt;  //搜索按钮
    private ImageButton mcloseClock;    //关闭定时器按钮

    private EditText msearchText;   //搜索键入

    private ListView mlistView; //歌曲列表
    private TextView mtitle;    //正在播放的歌曲标题
    private TextView martist;   //正在播放的歌曲演唱者

    private ArrayList<Music> mmusicArrayList;           //装Music类的数组列表
    private int mmusicId = 0; //记录播放歌曲的序号，初始化为0
    private ListView msettingList; //关于设置的具体菜单
    private int mcolor = -587202560; //主题颜色
    private int status; //播放状态
    private int mmodel = MusicService.modelLoop;    //播放模式
    private LinearLayout mplayingBar; //播放栏
    private RelativeLayout msettingBar; //设置栏

    private IndexActivity.StatusChangeReceiver statusChangeReceiver; //状态改变广播接收器
    private IndexActivity.IdChangeReceiver idChangeReceiver; //音乐ID改变接收广播



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);
        status=MusicService.statusStoped;
        findViews();
        initMusicList();
        initListView();
        checkMusicfile();
        initSettingList();
        initListener();
        bindStatusChangeReceiver();
        startService(new Intent(this,MusicService.class));
    }

    @Override
    protected void onDestroy(){
        unregisterReceiver(statusChangeReceiver);
        unregisterReceiver(idChangeReceiver);
        super.onDestroy();
    }

    /**组件关联**/
    private void findViews(){
        mplayOrPause = (ImageButton) findViewById(R.id.index_playOrPause);  //播放/暂停按钮
        mplayModel = (ImageButton) findViewById(R.id.playModele);   //播放模式按钮
        mlistView = (ListView) findViewById(R.id.index_listView);
        mtitle = (TextView) findViewById(R.id.index_title);
        mtitle.setSelected(true);   //View太多获取不到焦点，不设置这个不会滚动
        martist = (TextView) findViewById(R.id.index_artist);
        martist.setSelected(true);
        msearchText = (EditText) findViewById(R.id.search_text); //搜索键入
        msearchBt = (ImageButton) findViewById(R.id.search_button); //搜索按钮
        mcloseClock = (ImageButton) findViewById(R.id.close_clock); //关闭定时按钮，开启定时关闭时激活
        msettingBt = (ImageButton) findViewById(R.id.setting); //设置按钮
        msettingList = (ListView) findViewById(R.id.setting_listView); //设置菜单
        mplayingBar = (LinearLayout) findViewById(R.id.playingBar); //播放栏
        msettingBar = (RelativeLayout) findViewById(R.id.settingBar);   //设置栏
    }

    //——类、方法定义——//

    /**状态改变广播接收器定义**/
    class StatusChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            status = intent.getIntExtra("status",-1);
            switch(status){
                case MusicService.statusPlaying:    //播放中，将播放按钮改成暂停按钮
                    mplayOrPause.setBackgroundResource(R.drawable.button_pause_index);  //按钮外观改成暂停
                    String title = mmusicArrayList.get(mmusicId).getMmusicName().toString().trim();
                    mtitle.setText(title);
                    String artist = mmusicArrayList.get(mmusicId).getMmusicArtist().toString().trim();
                    martist.setText(artist);
                    break;
                case MusicService.statusPaused:     //暂停、停止，将播放按钮改回播放按钮
                case MusicService.statusStoped:
                    mplayOrPause.setBackgroundResource(R.drawable.button_play_index);   //按钮外观改回播放
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

    /**绑定广播接收器**/
    private void bindStatusChangeReceiver(){
        statusChangeReceiver = new IndexActivity.StatusChangeReceiver();
        idChangeReceiver = new IndexActivity.IdChangeReceiver();
        IntentFilter filter1 = new IntentFilter(MusicService.broadcastMusicServiceUpdateStatus); //消息过滤
        IntentFilter filter2 = new IntentFilter(MusicService.broadcastMusicServiceUpdateId);    //消息过滤
        registerReceiver(statusChangeReceiver,filter1);
        registerReceiver(idChangeReceiver,filter2);
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
                break;
            case MusicService.commandChangeModel:   //改变为某种播放模式，加装该播放模式
                intent.putExtra("model",mmodel);
                break;
            case MusicService.commandPrevious: //其他功能皆不需要加装内容
            case MusicService.commandNext:
            case MusicService.commandPause:
            case MusicService.commandStop:
            case MusicService.commandResume:
            case MusicService.commandCheckedIsPlaying:
            case MusicService.commandGetPosition:
            default:
                break;
        }
        sendBroadcast(intent);
    }

    /**设置按钮监听**/
    private void initListener(){
        /**改变播放模式**/
        mplayModel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mmodel){
                    case MusicService.modelLoop:    //列表循环改单曲循环
                        mmodel = MusicService.modelSingleCycle;
                        sendBroadcastOnCommand(MusicService.commandChangeModel);
                        mplayModel.setBackgroundResource(R.drawable.button_single_cycle);   //外观改为单曲循环
                        break;
                    case MusicService.modelSingleCycle: //单曲循环改随机播放
                        mmodel = MusicService.modelShufflePlayback;
                        sendBroadcastOnCommand(MusicService.commandChangeModel);
                        mplayModel.setBackgroundResource(R.drawable.button_shuffle_playback);      //外观改为随机播放
                        break;
                    case MusicService.modelShufflePlayback: //随机播放改列表循环
                        mmodel = MusicService.modelLoop;
                        sendBroadcastOnCommand(MusicService.commandChangeModel);
                        mplayModel.setBackgroundResource(R.drawable.button_loop);   //外观改为列表循环
                        break;
                }
            }
        });

        mplayOrPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**播放或者暂停**/
                //将标题和演唱者写入
                String title = mmusicArrayList.get(mmusicId).getMmusicName().toString().trim();
                mtitle.setText(title);
                String artist = mmusicArrayList.get(mmusicId).getMmusicArtist().toString().trim();
                martist.setText(artist);
                switch(status){
                    case MusicService.statusPaused: //暂停改播放
                        sendBroadcastOnCommand(MusicService.commandResume);
                        break;
                    case MusicService.statusPlaying:    //播放改暂停
                        sendBroadcastOnCommand(MusicService.commandPause);
                        break;
                    case MusicService.statusStoped:     //第一次播放
                        sendBroadcastOnCommand(MusicService.commandPlay);
                        break;
                    default:
                        break;
                }
            }
        });

        mlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /**单击该项播放该项**/
                mmusicId = position;
                //将标题和演唱者写入
                String title = mmusicArrayList.get(mmusicId).getMmusicName().toString().trim();
                mtitle.setText(title);
                String artist = mmusicArrayList.get(mmusicId).getMmusicArtist().toString().trim();
                martist.setText(artist);
                sendBroadcastOnCommand(MusicService.commandPlay);
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
                switch (position){
                    case 0:         //设置主题颜色
                        Intent intent = new Intent();
                        intent.setAction("com.example.administrator.music_player.action.Color");
                        intent.addCategory("android.intent.category.DEFAULT");
                        startActivityForResult(intent,GO_TO_COLOR);
                        break;
                    case 1:         //调整均衡器
                        /**未完善**/
                        break;
                    case 2:         //设置定时关闭
                        showSleepDialog();
                        break;
                }

            }
        });

        mplayingBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //跳转到播放界面
                sendBroadcastOnCommand(MusicService.commandGetPosition);    //让Service保存当前歌曲的进度
                goToMain();
            }
        });

        mcloseClock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSleep = false;
                if(msleepTimer != null) msleepTimer.cancel();
                mcloseClock.setVisibility(View.INVISIBLE);
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

        map = new HashMap<String,String>();
        map.put("Setting","均衡器");
        listMap.add(map);

        map = new HashMap<String,String>();
        map.put("Setting","定时关闭");
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

    /**界面跳转1：跳转到MainActivity**/
    public void goToMain() {
        Intent intent = new Intent();
        intent.setAction("com.example.administrator.music_player.action.Main");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.putExtra("id",mmusicId);
        intent.putExtra("color",mcolor);
        startActivityForResult(intent,GO_TO_MAIN);
    }

    /**回调结果处理**/
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        if(requestCode == GO_TO_MAIN){   //从播放界面返回
            mmusicId = data.getIntExtra("id",mmusicId);
            //将标题和演唱者写入
            String title = mmusicArrayList.get(mmusicId).getMmusicName().toString().trim();
            mtitle.setText(title);
            String artist = mmusicArrayList.get(mmusicId).getMmusicArtist().toString().trim();
            martist.setText(artist);
            sendBroadcastOnCommand(MusicService.commandSeekTo);
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

        }
    }

    /**改变设置选项菜单颜色**/
    private void changeColorOfList(ListView listview,int color){
        for(int position=0;position<listview.getChildCount();++position){
            //获取每一个item，先关联（没有关联不会改变颜色），再改变背景颜色
            listview.getChildAt(position).findViewById(R.id.setting_item).setBackgroundColor(color);
        }
    }

    /**连续按两下返回键退出程序**/
    @Override
    public void onBackPressed(){
        Timer timer = null;
        if(isExit ==false){
            isExit = true;  //【1】先将isExit改成true
            Toast.makeText(this, "请再按一次返回键退出程序", Toast.LENGTH_LONG).show();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false;
                }
            },2000);    //【2】计时2秒后将isExit改回false
        }else{
            //因为上一次按返回键时【1】执行了，【2】还没到2秒就按了第二次返回键
            // 所以此时第二次调用方法onBackPressed()并且isExit为true
            System.exit(0);
        }
    }

    /**定时关闭选择弹窗**/
    private void showSleepDialog()
    {
        final View userview = this.getLayoutInflater().inflate(R.layout.dialog_sleep,null); //获取布局
        final TextView minuteText = (TextView) userview.findViewById(R.id.dialog_textView);
        final Switch sleepSwitch = (Switch) userview.findViewById(R.id.dialog_switch);
        final SeekBar sleepSeekbar = (SeekBar) userview.findViewById(R.id.dialog_seekBar);

        minuteText.setText("睡眠"+sleepMinue+"分钟");
        sleepSwitch.setChecked(isSleep);    //根据是否打开睡眠设置开关状态
        sleepSeekbar.setMax(60);
        sleepSeekbar.setProgress(sleepMinue);
        /*设置监听*/
        sleepSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sleepMinue = progress;
                minuteText.setText("睡眠"+sleepMinue+"分钟");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sleepSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isSleep = isChecked;    //开关打开，睡眠模式为true；开关关闭，睡眠模式为false
            }
        });

        /*设置定时器任务*/
        final TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                System.exit(0);
            }
        };
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("选择睡眠时间(0~60分钟)");
        dialog.setView(userview);

        /*设置按钮及其响应事件*/
        dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                arg0.dismiss();
            }
        });
        dialog.setNeutralButton("重置", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                if(isSleep)
                {
                    timerTask.cancel();
                    msleepTimer.cancel();
                }
                isSleep = false;
                sleepMinue = 20;
                mcloseClock.setVisibility(View.INVISIBLE);
            }
        });
        dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                if(isSleep)
                {
                    msleepTimer = new Timer();
                    int time = sleepSeekbar.getProgress();
                    //启动任务，time*60*1000毫秒后执行
                    msleepTimer.schedule(timerTask, time*60*1000);
                    mcloseClock.setVisibility(View.VISIBLE);
                }
                else
                {   //可通过switch改变isSleep状态，此时isSleep==false，即不打算开启了
                    timerTask.cancel();
                    if(msleepTimer != null) msleepTimer.cancel();
                    arg0.dismiss();
                    mcloseClock.setVisibility(View.INVISIBLE);
                }
            }
        });

        dialog.show();
    }
}
