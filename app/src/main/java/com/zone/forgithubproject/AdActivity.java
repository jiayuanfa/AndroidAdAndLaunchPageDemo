package com.zone.forgithubproject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.zone.forgithubproject.myApplication.MyApplication;
import com.zone.forgithubproject.base.BaseActivity;
import com.zone.forgithubproject.contract.AdContract;
import com.zone.forgithubproject.model.bean.LoginCheckBean;
import com.zone.forgithubproject.presenter.AdPresenterImpl;
import com.zone.forgithubproject.utils.L;
import com.zone.forgithubproject.utils.NetUtils;
import com.zone.forgithubproject.utils.SPUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;



/**
 * Created by zone on 2016/7/18.
 */
public class AdActivity extends BaseActivity implements AdContract.View {

    @InjectView(R.id.tv_second)
    TextView tvSecond;
    @InjectView(R.id.layout_skip)
    LinearLayout layoutSkip;
    int timeCount = 0;

//  是否继续读秒
    boolean continueCount = true;
    @InjectView(R.id.iv_advertising)
    ImageView ivAdvertising;
    private LoginCheckBean loginCheckBean;

//  广告页面数秒
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            countNum();
            if (continueCount) {

//          每隔1秒钟调用一次即可
                handler.sendMessageDelayed(handler.obtainMessage(-1),1000);
            }
        }
    };

    private Bitmap bitmap;
    private AdPresenterImpl pAd;
    private int initTimeCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ad);

        ButterKnife.inject(this);
        pAd = new AdPresenterImpl();
        pAd.attachView(this);

        initTimeCount = 6;
        loginCheckBean = new LoginCheckBean();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (NetUtils.isConnected(AdActivity.this)) {
            pAd.getLoginCheck();
        }
        layoutSkip.setVisibility(View.INVISIBLE);
        handler.sendMessageDelayed(handler.obtainMessage(-1),1000);
    }


//    Bind 广告ImageView 跳过Button
    @OnClick({R.id.iv_advertising, R.id.layout_skip})

    public void onClick(View view) {
        switch (view.getId()) {

//            广告点击
            case R.id.iv_advertising:
                SPUtils.put(this,"adUrl","https://www.baidu.com");
                String url = (String) SPUtils.get(this, "adUrl", "null");
                if (!url.equals("null")) {
                    continueCount = false;
                    Intent intent = new Intent(AdActivity.this, WebViewActivity.class);
                    intent.putExtra("title", "广告详情页");
                    intent.putExtra("url", url);
                    intent.putExtra("from", "advertising");
                    startActivity(intent);
                    finish();
                }
                break;

            case R.id.layout_skip:
                continueCount = false;
                toNextActivity();
                finish();
                break;
        }
    }

    /**
     * 广告页面数秒调用的方法
     * @return
     */
    private int countNum() {
        //数秒、
        timeCount++;

        if (timeCount == 3) {

            //数秒，超过3秒后如果没有网络，则进入下一个页面
            if (!NetUtils.isConnected(AdActivity.this)) {
                continueCount = false;
                toNextActivity();
                finish();
            }

            if (!loginCheckBean.isPlayAd()) {

                //如果服务端不允许播放广告，则直接进入下一个页面
                continueCount = false;
                toNextActivity();
                finish();

            } else {

                //如果播放，则获取本地的图片
                ivAdvertising.setVisibility(View.VISIBLE);
                layoutSkip.setVisibility(View.VISIBLE);
            }
        }

//        超过广告显示的最长时间 进图下一个界面
        if (timeCount == initTimeCount) {
            continueCount = false;
            toNextActivity();
            finish();
        }
        return timeCount;
    }


    /**
     * 去登录界面、主页、或者引导页面
     */
    public void toNextActivity() {

        //根据是否保存有 token 判断去登录界面还是主界面
        L.d("到下一个界面");
        Intent intent = null;
        String token = (String) SPUtils.get(AdActivity.this, "token", "");

//       判断是去登录界面、引导页、主页
        if (TextUtils.isEmpty(token)) {
            intent = new Intent(AdActivity.this, LoginActivity.class);
        } else {
            intent = new Intent(AdActivity.this, MainActivity.class);
            MyApplication.setToken(token);
        }
        startActivity(intent);
        finish();
    }




    @Override
    public void setAdTime(int count) {
        initTimeCount = count + 3;
    }

    @Override
    public void setLoginCheckBean(LoginCheckBean loginCheckBean) {
        this.loginCheckBean = loginCheckBean;
    }

    @Override
    public void setAdImg(Bitmap bitmap) {
        if (bitmap != null) {
            ivAdvertising.setImageBitmap(bitmap);
        } else {//加强用户体验，如果是获取到的bitmap为null，则直接跳过
            continueCount = false;
            toNextActivity();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bitmap != null) {//进行bitmap回收
            bitmap.recycle();
        }
        pAd.detachView();
    }
}
