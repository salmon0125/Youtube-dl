package com.example.youtube_dl;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class YoutubeDLActivity extends AppCompatActivity {
    public static final String TAG = "byUserDebug";
    private AtomicBoolean downloading = new AtomicBoolean(false);

    private EditText urlText;
    private ProgressBar progressBar;
    private TextView tvDownloadStatus;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_d_l);

        progressBar = findViewById(R.id.progressBar);
        urlText = findViewById(R.id.editText);
        tvDownloadStatus = findViewById(R.id.textView);

        try {
            YoutubeDL.getInstance().init(getApplication());
        } catch (YoutubeDLException e) {
            Log.e(TAG, "failed to initialize youtubedl-android", e);
        }
    }

    public void onClickDownload(View view){
        startDownload();
    }

    private void startDownload(){
        if(downloading.get()){
            Toast.makeText(YoutubeDLActivity.this,"already downloading",Toast.LENGTH_LONG).show();
            return;
        }

        if (!isStoragePermissionGranted()) {
            Toast.makeText(YoutubeDLActivity.this, "grant storage permission and retry", Toast.LENGTH_LONG).show();
            return;
        }


        String url = urlText.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            urlText.setError(getString(R.string.url_error));
            return;
        }

        YoutubeDLRequest request = new YoutubeDLRequest(url);
        File youtubeDLDir = getDownloadLocation();

        request.addOption("-o", youtubeDLDir.getAbsolutePath() + "/%(title)s.%(ext)s");
        request.addOption("-x");
        request.addOption("--audio-format","mp3");

        progressBar.setProgress(0);
        tvDownloadStatus.setText(getString(R.string.download_start));

        downloading.set(true);
        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, callback))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(youtubeDLResponse -> {
                    progressBar.setProgress(100);
                    Toast.makeText(YoutubeDLActivity.this, "download successful", Toast.LENGTH_LONG).show();
                    downloading.set(false);
                    tvDownloadStatus.setText(R.string.download_complete);
                }, e -> {
                    if(BuildConfig.DEBUG) Log.e(TAG,  "failed to download", e);
                    Toast.makeText(YoutubeDLActivity.this, "download failed", Toast.LENGTH_LONG).show();
                    progressBar.setProgress(0);
                    downloading.set(false);
                    tvDownloadStatus.setText(R.string.download_failed);
                });
        compositeDisposable.add(disposable);
    }

    private final DownloadProgressCallback callback = new DownloadProgressCallback() {
        @Override
        public void onProgressUpdate(float progress, long etaInSeconds, String line) {
            runOnUiThread(() -> {
                        progressBar.setProgress((int) progress);
                        tvDownloadStatus.setText(getText(R.string.download_doing));
                        Log.d(TAG,"progress is "+(int)progress);
                    }
            );
        }
    };

    private File getDownloadLocation() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File youtubeDLDir = new File(downloadsDir, "youtubedl");
        if (!youtubeDLDir.exists()) youtubeDLDir.mkdir();
        return youtubeDLDir;
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
                return false;
            }
        } else {
            return true;
        }
    }
}