package com.example.administrator.music_player.Activity;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.administrator.music_player.R;

public class ChooseColor extends Activity{

    private View mview1;
    private View mview2;
    private View mview3;
    private View mview4;
    private View mview5;
    private View mview6;
    private View mview7;
    private View mview8;
    private View mview9;
    private Button msure;
    private Button munSure;
    private int mcolor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_color);
        findView();
        initListener();
    }

    /**关联相关组件**/
    private void findView(){
        mview1 = findViewById(R.id.v1);
        mview2 = findViewById(R.id.v2);
        mview3 = findViewById(R.id.v3);
        mview4 = findViewById(R.id.v4);
        mview5 = findViewById(R.id.v5);
        mview6 = findViewById(R.id.v6);
        mview7 = findViewById(R.id.v7);
        mview8 = findViewById(R.id.v8);
        mview9 = findViewById(R.id.v9);
        msure = (Button) findViewById(R.id.sure);
        munSure = (Button) findViewById(R.id.unsure);
    }

    /**按钮监听**/
    private void initListener(){
        msure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                if(mcolor != 0){
                    intent.putExtra("color",mcolor);
                    setResult(1,intent);
                }
                else setResult(2,intent);
                finish();
            }
        });

        munSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mcolor = 0;
            }
        });

        /**取色**/

        mview1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mcolor = ChooseColor.this.getResources().getColor(R.color.black);
            }
        });

        mview2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mcolor = ChooseColor.this.getResources().getColor(R.color.gray);
            }
        });

        mview3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mcolor = ChooseColor.this.getResources().getColor(R.color.red);
            }
        });

        mview4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mcolor = ChooseColor.this.getResources().getColor(R.color.green);
            }
        });

        mview5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mcolor = ChooseColor.this.getResources().getColor(R.color.deepBlue);
            }
        });

        mview6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mcolor = ChooseColor.this.getResources().getColor(R.color.lightBlue);
            }
        });

        mview7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mcolor = ChooseColor.this.getResources().getColor(R.color.orange);
            }
        });

        mview8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mcolor = ChooseColor.this.getResources().getColor(R.color.pink);
            }
        });

        mview9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mcolor = ChooseColor.this.getResources().getColor(R.color.purple);
            }
        });
    }
}
