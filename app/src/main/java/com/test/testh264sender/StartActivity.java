package com.test.testh264sender;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatButton;

import com.test.testh264sender.DJ.VoiceActivity;
import com.test.testh264sender.ui.LaifengLivingActivity;
import com.test.testh264sender.ui.LaifengScreenRecordActivity;
import com.test.testh264sender.ui.SecondActivity;

/**
 * Created by xu.wang
 * Date on  2018/5/28 09:41:00.
 *
 * @Desc
 */

public class StartActivity extends AppCompatActivity implements View.OnClickListener {

    private AppCompatButton btn_living, btn_record;
    private Uri fileUri,photoUri;
    private int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 1;
    private String ipname = Constant.ip;
    private Integer port = Constant.port;


    private Button video, chaifen, photo, view,dj;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        initialView();
        video = findViewById(R.id.video);
        chaifen = findViewById(R.id.chaifen);
        photo = findViewById(R.id.photo);
        dj = findViewById(R.id.dj);
        dj.setOnClickListener(this);
        video.setOnClickListener(this);
        chaifen.setOnClickListener(this);
        photo.setOnClickListener(this);
        view = findViewById(R.id.view);
        view.setOnClickListener(this);

    }

    private void initialView() {
        btn_living = findViewById(R.id.btn_test_living);
        btn_record = findViewById(R.id.btn_test_record);

        btn_living.setOnClickListener(this);
        btn_record.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_test_living:
                Intent livingIntent = new Intent(this, LaifengLivingActivity.class);
                startActivity(livingIntent);
                break;
            case R.id.btn_test_record:
                Intent intent = new Intent(this, LaifengScreenRecordActivity.class);
                startActivity(intent);
                break;

            case R.id.dj:
                Intent intent3 = new Intent(StartActivity.this,VoiceActivity.class);
                startActivity(intent3);
                break;
            case R.id.video:      //录像
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                } else {
                    Intent intent2 = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    File outputImage = new File(getExternalCacheDir(), "output_image.mp4");

                    if (outputImage.exists()) {
                        outputImage.delete();
                    }
                    try {
                        outputImage.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (Build.VERSION.SDK_INT >= 24) {
                        fileUri = FileProvider.getUriForFile(this, "com.example.root.cameraandalbum1.fileprovider", outputImage);
                    } else {
                        fileUri = Uri.fromFile(outputImage);
                    }
                    intent2.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);  // set the image file name
                    intent2.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high
                    // start the Video Capture Intent
                    startActivityForResult(intent2, 3);
                }
                break;
            case R.id.chaifen:    //拆分视频
                getBitmapsFromVideo();
                break;

            case R.id.view:     //图片直播

                Intent intent4 = new Intent(StartActivity.this, SecondActivity.class);
                intent4.putExtra("ipname", ipname);
                startActivity(intent4);
                break;
            case R.id.photo:    //拍照
                File outputImage = new File(getExternalCacheDir(), "output_image.jpg");

                if (outputImage.exists()) {
                    outputImage.delete();
                }
                try {
                    outputImage.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (Build.VERSION.SDK_INT >= 24) {
                    photoUri = FileProvider.getUriForFile(this, "com", outputImage);
                } else {
                    photoUri = Uri.fromFile(outputImage);
                }
                Intent intent5 = new Intent("android.media.action.IMAGE_CAPTURE");
                intent5.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(intent5, 2);
        }
    }

    public void getBitmapsFromVideo() {
        String dataPath = getExternalCacheDir() + "/output_image.mp4";
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(dataPath);
// 取得视频的长度(单位为毫秒)
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
// 取得视频的长度(单位为秒)
        int seconds = Integer.valueOf(time) / 1000;
// 得到每一秒时刻的bitmap比如第一秒,第二秒
        for (int i = 1; i <= seconds; i++) {
            Bitmap bitmap = retriever.getFrameAtTime(i * 1000 * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            String path = getExternalCacheDir() + File.separator + i + ".jpg";
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(path);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 2:
                if (resultCode == RESULT_OK) {
                    try {
                        final Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(photoUri));
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Socket socket = new Socket(ipname, port);
                                    File file = new File(getExternalCacheDir(), "output_image.jpg");
                                    FileInputStream fis = new FileInputStream(file);
                                    byte[] bytes = new byte[1024];
                                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                                    int length = 0;
                                    long progress = 0;
                                    while ((length = fis.read(bytes, 0, bytes.length)) != -1) {
                                        dos.write(bytes, 0, length);
                                        dos.flush();
                                    }
                                    socket.shutdownOutput();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                }
                break;

            case 3:
                if (resultCode == RESULT_OK) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Socket socket = new Socket(ipname, port);
                                File mp4 = new File(getExternalCacheDir(), "output_image.mp4");
                                FileInputStream fis = new FileInputStream(mp4);
                                byte[] bytes = new byte[1024];
                                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                                int length = 0;
                                long progress = 0;
                                while ((length = fis.read(bytes, 0, bytes.length)) != -1) {
                                    dos.write(bytes, 0, length);
                                    dos.flush();
                                }
                                socket.shutdownOutput();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
                break;

        }
    }


}
