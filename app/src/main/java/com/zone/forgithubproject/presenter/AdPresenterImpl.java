package com.zone.forgithubproject.presenter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.zone.forgithubproject.base.PBase;
import com.zone.forgithubproject.contract.AdContract;
import com.zone.forgithubproject.model.AdModelImpl;
import com.zone.forgithubproject.model.bean.AdMessageBean;
import com.zone.forgithubproject.model.bean.LoginCheckBean;
import com.zone.forgithubproject.utils.L;
import com.zone.forgithubproject.utils.Rx.RxSubscribe;
import com.zone.forgithubproject.utils.SPUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by zone on 2017/03/26
 */

public class AdPresenterImpl extends PBase<AdContract.View> {
    private AdModelImpl mAdModel;

    public AdPresenterImpl() {
        mAdModel = new AdModelImpl();
    }


    /**
     * 向服务端请求是否要播放广告
     */

    public void getLoginCheck() {

        //向服务器请求是否要播放广告
        mAdModel.getLoginCheck()
                .subscribeOn(Schedulers.io())                            //发布者在后台线程中运行
                .observeOn(AndroidSchedulers.mainThread())               //订阅者在Android主线程中运行
                .subscribe(new RxSubscribe<LoginCheckBean>() {

                    @Override
                    protected void _onNext(LoginCheckBean loginCheckBean) {
                        getMyView().setLoginCheckBean(loginCheckBean);

//                        获取服务端数据以判断权限
                        if (loginCheckBean.isPlayAd()) {
                            //这里需要添加一个是否已经下载的判断，如果已经下载，则不再进行下载
                            getAdMessage();
                        }
                    }

                    @Override
                    protected void _onError(String message) {

                    }

                    @Override
                    public void onCompleted() {

                    }
                });
    }

    /**
     * 获取广告的图片地址、详情页面链接、广告的播放时间
     */

    public void getAdMessage() {
        mAdModel.getAdMessage()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new RxSubscribe<AdMessageBean>() {

                    @Override
                    protected void _onNext(AdMessageBean adMessageBean) {

//                        设置广告显示时间
                        getMyView().setAdTime(adMessageBean.getAdTime());
                        getAdPicture(adMessageBean.getAdPictureUrl(), "123.jpg");//如果没有下载，直接下载
                    }

                    @Override
                    protected void _onError(String message) {

                    }

                    @Override
                    public void onCompleted() {

                    }
                });
    }

    /**
     * 获取本地图片
     */
    private void getLocalPicture(String localUrl) {
        Bitmap bitmap = BitmapFactory.decodeFile(localUrl);
        getMyView().setAdImg(bitmap);
    }

    /**
     * 获取网络广告
     * @param fileUrl
     * @param fileName
     */
    public void getAdPicture(final String fileUrl, final String fileName) {

        //获取要展示的广告图片 如果本地的Url等于网络的Url 在直接从本地获取
        if (SPUtils.get((Context) getMyView(), "adPictureUrl", "").equals(fileUrl)) {

            L.d("从本地获取图片");
            getLocalPicture((String) SPUtils.get((Context) getMyView(),"adPictureAddress",""));

        } else {

            L.d("从网络中获取图片");
            mAdModel.downLoadFile(fileUrl)
                    .subscribeOn(Schedulers.newThread())                            //发布者在后台线程中运行
                    .observeOn(AndroidSchedulers.mainThread())               //订阅者在Android主线程中运行
                    .map(new Func1<ResponseBody, Bitmap>() {
                        @Override
                        public Bitmap call(ResponseBody responseBody) {
                            if (responseBody != null) {
                                L.d("收到的responseBody不为空！");
                            }

//              图片下载成功后 存储到磁盘 以及Url
                            if (writeResponseBodyToDisk(responseBody, fileName, fileUrl)) {
                                Bitmap bitmap = BitmapFactory.decodeFile(((Context) getMyView()).getExternalFilesDir(null) + File.separator + fileName);
                                return bitmap;
                            }
                            return null;
                        }
                    }).subscribe(new RxSubscribe<Bitmap>((Context) getMyView()) {

                @Override
                protected void _onNext(Bitmap bitmap) {

//              得到广告之后，设置
                    getMyView().setAdImg(bitmap);
                }

                @Override
                protected void _onError(String message) {

                }

                @Override
                public void onCompleted() {

                }
            });
        }

    }


    /**
     * 图片存储到磁盘
     * @param body
     * @param fileName
     * @param fileUrl
     * @return
     */
    private boolean writeResponseBodyToDisk(ResponseBody body, String fileName, String fileUrl) {

        //保存图片到本地

        try {
            // todo change the file location/name according to your needs

            File futureStudioIconFile = new File(((Context) getMyView()).getExternalFilesDir(null) + File.separator + fileName);
            L.d("文件的保存地址为：" + ((Context) getMyView()).getExternalFilesDir(null) + File.separator + fileName);
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];
                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;
                inputStream = body.byteStream();
                outputStream = new FileOutputStream(futureStudioIconFile);
                while (true) {
                    int read = inputStream.read(fileReader);
                    if (read == -1) {
                        break;
                    }
                    outputStream.write(fileReader, 0, read);
                    fileSizeDownloaded += read;

                    L.d("file download: " + fileSizeDownloaded / fileSize * 100);
                    L.d("file download: " + fileSizeDownloaded + " of " + fileSize);
                }
                outputStream.flush();

//                把Url存储到 SPUtils 避免重复下载
                SPUtils.put((Context) getMyView(), "adPictureAddress", ((Context) getMyView()).getExternalFilesDir(null) + File.separator + fileName);
                SPUtils.put((Context) getMyView(), "adPictureUrl", fileUrl);

                return true;

            } catch (IOException e) {

                return false;

            } finally {

                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }

            }

        } catch (IOException e) {

            return false;
        }
    }
}