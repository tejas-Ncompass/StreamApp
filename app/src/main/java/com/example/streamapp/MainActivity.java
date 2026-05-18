package com.example.streamapp;

import static android.content.ContentValues.TAG;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;

import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.pedro.encoder.input.gl.render.filters.AndroidViewFilterRender;
import com.pedro.encoder.input.gl.render.filters.object.ImageObjectFilterRender;
import com.pedro.library.util.BitrateAdapter;
import com.pedro.library.view.OpenGlView;

import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.streamapp.databinding.ActivityMainBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pedro.common.ConnectChecker;
import com.pedro.encoder.input.gl.render.filters.object.TextObjectFilterRender;
import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.encoder.utils.gl.TranslateTo;
import com.pedro.library.rtmp.RtmpCamera1;

public class MainActivity extends AppCompatActivity implements ConnectChecker, AdapterView.OnItemSelectedListener, View.OnClickListener {
    private ActivityMainBinding binding;
    private RtmpCamera1 camera;
    private OpenGlView preView;
    private AlertDialog networkDialog;
    private static final double smoothingFactor = 0.2;
    private boolean isPaused = false, wasStreaming = false, isRear = true, isMute = false, isOpen;
    private int selectedWidth = 1280, selectedHeight = 720, bitrateps = 2500 * 1000, minBitrate = 500 * 1000, maxBitrate = 5000 * 1000, targetBitrate;
    private long bitrateRecieved;
    private double averageBitrate = 0;
    private Handler customBitrateHandler = new Handler(Looper.getMainLooper());
    private Runnable customBitrateAdjuster;
    private TennisScoreLogic tennisScoreLogic;
    private final String streamUrl = "rtmp://44.198.94.207:1935/jdp/bpzDEvWmJnFYD1MFWOah-ABRmbntwrkcheck2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        View statusBarBg = binding.statusBarBackground;
        ViewCompat.setOnApplyWindowInsetsListener(statusBarBg, (v, insets) -> {
            Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.getLayoutParams().height = statusBarInsets.top;
            v.requestLayout();

            return insets;
        });

        checkPermissions(); // Camera and audio permissions check

        preView = binding.captureView;
        camera = new RtmpCamera1(preView, this);

        // Setting limit to the retry
        camera.getStreamClient().setReTries(10);

        // onClickListeners
        binding.btnStartStop.setOnClickListener(this::handleControlBtn);
        binding.stopButton.setOnClickListener(this::handleStopBtn);
        binding.camCard.setOnClickListener(this::handleCameraswitch);
        binding.qualityButton.setOnClickListener(this::handleSettingsBtn);
        binding.micCard.setOnClickListener(this::handleMicBtn);
        binding.focusCard.setOnClickListener(this::handleAutofocusBtn);
        binding.openScoreboard.setOnClickListener(this::handleOpenScoreboard);
        binding.previewScreen.setOnClickListener(this::handleCloseScoreboard);

        tennisScoreLogic = new TennisScoreLogic(this,binding);

        binding.captureView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (!camera.isOnPreview()) {
                    camera.startPreview();
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                if (camera.isStreaming()) camera.stopStream();
                camera.stopPreview();
            }
        });

        // setting adapter for quality dropdown (spinner)
        binding.qualitySpinner.setOnItemSelectedListener(this);
        ArrayAdapter<String> qualityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,getResources().getStringArray(R.array.quality));
        qualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.qualitySpinner.setAdapter(qualityAdapter);
        binding.qualitySpinner.setSelection(2);

        // setting adapter for sports dropdown (spinner)
        binding.sportsSpinner.setOnItemSelectedListener(this);
        ArrayAdapter<String> sportsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,getResources().getStringArray(R.array.sports));
        sportsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.sportsSpinner.setAdapter(sportsAdapter);
    }

    private Point getScreenResolution() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        return new Point(metrics.widthPixels, metrics.heightPixels);
    }


    // Button that handles Go Live, Pause and Resume
    private void handleControlBtn(View view) {
        if (!camera.isStreaming()) {
            startStreamingProcess();
        } else {
            pauseResumeCondition();
        }
    }

    // Button handling Stop stream
    private void handleStopBtn(View view) {
        if (camera.isStreaming() || wasStreaming) {
            camera.stopStream();
            resetToStart();
        }
    }

    // Button that switches the camera to front/back
    private void handleCameraswitch(View view) {
        try {
            camera.switchCamera();
            isRear = !isRear;
        } catch (CameraOpenException e) {
            Toast.makeText(this, "Unable to switch camera", Toast.LENGTH_SHORT).show();
        }
        if (isRear) {
            binding.camStatus.setText(R.string.rear);
            binding.camLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.card_color));
        } else {
            binding.camStatus.setText(R.string.front);
            binding.camLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.selected));
        }
    }

    // settings button that opens the sliding drawer
    private void handleSettingsBtn(View view) {
        if(!binding.main.isDrawerOpen(GravityCompat.END)) {
            binding.main.openDrawer(GravityCompat.END);
        }
        else
        {
            binding.main.closeDrawer(GravityCompat.END);
        }
    }

    // handles mute and unmute
    private void handleMicBtn(View view) {
        if(isMute){
            camera.enableAudio();
            binding.micStatus.setText(R.string.unmuted);
            binding.micLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.card_color));
            binding.muteImage.setVisibility(View.GONE);
        }
        else{
            camera.disableAudio();
            binding.micStatus.setText(R.string.muted);
            binding.micLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.selected));
            binding.muteImage.setVisibility(View.VISIBLE);
        }
        isMute = !isMute;
    }

    // handles autofocus on and off
    private void handleAutofocusBtn(View view) {
        if (camera.isAutoFocusEnabled()) {
            camera.disableAutoFocus();
            binding.focusStatus.setText(R.string.off);
            binding.focusLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.card_color));

        } else {
            camera.enableAutoFocus();
            binding.focusStatus.setText(R.string.on);
            binding.focusLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.selected));
        }
    }

    // opens scoreboard by shrinking the preview screen
    private void handleOpenScoreboard(View view) {
        float centerX = (binding.captureView.getWidth() / 2f) - (binding.previewScreen.getWidth() / 2f);
        float centerY = (binding.captureView.getHeight() / 2f) - (binding.previewScreen.getHeight() / 1.65f);

        isOpen =true;
        binding.previewScreen.animate().x(centerX).y(centerY).scaleX(0.5f).scaleY(0.6f).setDuration(300).start();

        binding.btnStartStop.setVisibility(View.INVISIBLE);
        binding.bakedView.setVisibility(View.INVISIBLE);
        binding.onlineStatus.setVisibility(View.INVISIBLE);
        binding.offlineStatus.setVisibility(View.INVISIBLE);
    }

    // closes scoreboard by enlarging the preview screen
    private void handleCloseScoreboard(View view) {
        if(isOpen){
            binding.previewScreen.animate().x(0f).y(0f).scaleX(1f).scaleY(1f).setDuration(300).start();
            binding.btnStartStop.setVisibility(View.VISIBLE);
            binding.bakedView.setVisibility(View.VISIBLE);
            binding.onlineStatus.setVisibility(View.VISIBLE);
            binding.offlineStatus.setVisibility(View.VISIBLE);
            isOpen = false;
        }
    }

    // Start stream logic
    private void startStreamingProcess() {

        if (!isNetworkAvailable()) {
            showNoInternetDialog();
            return;
        }

        if (camera.prepareAudio() && camera.prepareVideo(selectedWidth, selectedHeight, 30, bitrateps, 2, 0)) {
            camera.startStream(streamUrl);
            if (camera.getGlInterface() != null){

                camera.getGlInterface().clearFilters();

                // Baking the view into stream
            AndroidViewFilterRender viewFilter = new AndroidViewFilterRender();
            viewFilter.setView(binding.bakedView);
            viewFilter.setPosition(8f,8f);
            viewFilter.setScale(15f,15f);

            // setting watermark
            ImageObjectFilterRender watermarkFilter = new ImageObjectFilterRender();
            Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.app_icon);
            watermarkFilter.setImage(logo);
            watermarkFilter.setAlpha(0.08f);
            watermarkFilter.setPosition(12.5f,0f);
            watermarkFilter.setScale(50f,100f);

            camera.getGlInterface().addFilter(watermarkFilter);
            camera.getGlInterface().addFilter(viewFilter);
            }
        }
    }

    // Pause or Resume logic
    private void pauseResumeCondition() {
        if (!isPaused) {
            camera.pauseRecord();
            isPaused = true;
            binding.btnStartStop.setText(R.string.resume);
            binding.btnStartStop.setBackgroundColor(ContextCompat.getColor(this, R.color.green));
        }
        else {
            camera.resumeRecord();
            isPaused = false;
            updateLiveUI();
        }
    }

    // Sets the UI to show streaming status
    private void updateLiveUI() {
        binding.btnStartStop.setText(R.string.pause);
        binding.btnStartStop.setBackgroundColor(ContextCompat.getColor(this, R.color.amber));
        binding.liveStatus.setVisibility(View.VISIBLE);
        binding.stopButton.setVisibility(View.VISIBLE);
    }

    // Sets the UI when not streaming
    private void resetToStart() {
        binding.btnStartStop.setText(R.string.go_live);
        binding.btnStartStop.setBackgroundColor(ContextCompat.getColor(this, R.color.green));
        binding.liveStatus.setVisibility(View.INVISIBLE);
        binding.stopButton.setVisibility(View.GONE);
        isPaused = false;
        wasStreaming = false;
    }

    // Method to set resolution to the stream
    private void setResolution(int w, int h, int bit) {
        selectedWidth = w;
        selectedHeight = h;
        targetBitrate = bit * 1000;
        bitrateps = targetBitrate;

        averageBitrate = 0;

        if(camera.isStreaming()) {
            camera.stopStream();
            startStreamingProcess();
        }
    }

    // Network monitor/Connectivity manager that keeps a watch on the internet access
    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {

        // logic that runs when internet is available
        @Override
        public void onAvailable(@NonNull Network network) {
            runOnUiThread(() -> {
                if (networkDialog != null && networkDialog.isShowing()) {
                    networkDialog.dismiss();
                }
                binding.onlineStatus.setVisibility(View.VISIBLE);
                binding.offlineStatus.setVisibility(View.INVISIBLE);
            });
        }

        // logic that runs when connection is lost
        @Override
        public void onLost(@NonNull Network network) {
            boolean currentlyStreaming = camera.isStreaming();

            runOnUiThread(() -> {
                showNoInternetDialog();
                binding.onlineStatus.setVisibility(View.INVISIBLE);
                binding.offlineStatus.setVisibility(View.VISIBLE);

                if (currentlyStreaming) {
                    camera.stopStream();
                }
            });
        }
    };

    // Setting up network monitor
    private void setupNetworkMonitoring() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        NetworkRequest request = new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();

        cm.registerNetworkCallback(request, networkCallback);
    }

    // Checks the network availability in between the stream
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    // Start method for the Internet connection monitor
    @Override
    protected void onStart() {
        super.onStart();
        setupNetworkMonitoring();
    }

    // Stop method for the Internet connection monitor
    @Override
    protected void onStop() {
        super.onStop();
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            cm.unregisterNetworkCallback(networkCallback);
        }
    }

    // Dialog that appears when network connectivity is lost
    private void showNoInternetDialog() {
        if (networkDialog == null) {
            View view = LayoutInflater.from(this).inflate(R.layout.connection_lost_dialog, null);
            Button settings = view.findViewById(R.id.settings_button);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(view);

            networkDialog = builder.create();
            networkDialog.setCancelable(false);
            if (networkDialog.getWindow() != null) {
                networkDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            }
            settings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        }
        if(!networkDialog.isShowing()){
            networkDialog.show();
        }
    }

    // Method to check Camera and audio permissions
    private void checkPermissions() {
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, 100);
        }
    }

    // ConnectChecker Interface Callbacks
    @Override
    public void onConnectionSuccess() {
        runOnUiThread(() -> {
            binding.loadingSpinner.setVisibility(View.GONE);
            updateLiveUI();
            isPaused = false;
            wasStreaming = false;

            binding.adaptiveSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked){
                    Toast.makeText(this, "Adaptive Bitrate turned on", Toast.LENGTH_SHORT).show();
                    startBitrateController();
                }
                if (!isChecked){
                    Toast.makeText(this, "Adaptive Bitrate turned off", Toast.LENGTH_SHORT).show();
                    camera.setVideoBitrateOnFly(bitrateps);
                }
            });

            Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show();
        });
        }

    @Override
    public void onConnectionFailed(@NonNull String reason) {
        runOnUiThread(() -> {
            if (camera.getStreamClient().reTry(2000, reason)) {
                binding.loadingSpinner.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Connection failed :( Retrying...", Toast.LENGTH_SHORT).show();
            } else {
                binding.loadingSpinner.setVisibility(View.GONE);
                camera.stopStream();
            }

            stopBitrateController();
        });
    }

    // ABR logic
    private void startBitrateController() {
        customBitrateAdjuster = new Runnable() {
            @Override
            public void run() {
                double inputBitrate = bitrateRecieved; // bitrate received from onNewBitrate()

                if (averageBitrate == 0){              // initial average value
                    averageBitrate = inputBitrate;
                }
                else{                // updating the average base on : 20% of new bitrate detected and 80% of old average bitrate
                    averageBitrate = (smoothingFactor * inputBitrate) + ((1 - smoothingFactor) * averageBitrate);
                }

                int newBitrate = bitrateps;       // bitrate after quality change or acts as reference to current quality

                if(averageBitrate < bitrateps * 0.7){          // checks if it is less than 70% of set bitrate
                    newBitrate = (int)(averageBitrate * 0.8);     // then reduces 20% from the average
                }
                else if (averageBitrate > bitrateps * 1.2 && bitrateps <= targetBitrate){  // checks if it exceeds the set bitrate value
                    newBitrate = (int) Math.min(targetBitrate, bitrateps * 1.1);          // increases the bitrateps by 10%.
                }

                newBitrate = Math.max(minBitrate, Math.min(newBitrate, maxBitrate)); // ensures it stays within the min and max bounds

                if (newBitrate != bitrateps){
                    bitrateps = newBitrate;
                    camera.setVideoBitrateOnFly(newBitrate);    // applies bitrate on the fly
                }

                customBitrateHandler.postDelayed(this, 5000);   // this happens every 5 seconds
            }
        };
        customBitrateHandler.post(customBitrateAdjuster);
    }

    // Stops the bitrate controller
    private void stopBitrateController() {
        if (customBitrateHandler != null && customBitrateAdjuster != null) {
            customBitrateHandler.removeCallbacks(customBitrateAdjuster);
        }
    }

    // Displays the live bitrate(mbps) in UI
    @Override
    public void onNewBitrate(long bitrate) {
      bitrateRecieved = bitrate;
        runOnUiThread(() -> {
            double mbpsValue = bitrate / 1000000.0;
            String mbpsText = String.format("%.2f Mbps", mbpsValue);
            binding.mbps1.setText(mbpsText);


            if (mbpsValue < 1.2) // sets the icon according to the bitrate
            {
                binding.signalStrength1.setImageResource(R.drawable.low);
            }
            else if ((mbpsValue >= 1.2) && (4.0 >= mbpsValue)) {
                binding.signalStrength1.setImageResource(R.drawable.mid);
            }
            else if (mbpsValue > 4.0) {
                binding.signalStrength1.setImageResource(R.drawable.high);
            }
        });
    }
    @Override
    public void onDisconnect() {}
    @Override
    public void onAuthError() {}
    @Override
    public void onAuthSuccess() {}
    @Override
    public void onConnectionStarted(@NonNull String s) {}
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        String qualityAtPosition = parent.getItemAtPosition(position).toString();

        switch (qualityAtPosition)
        {
            case "360p":
                Toast.makeText(MainActivity.this, "Quality set to 360p", Toast.LENGTH_SHORT).show();
                setResolution(640, 360, 1000);
                binding.qualityImage.setImageResource(R.drawable.q1);
                break;

            case "480p":
                Toast.makeText(MainActivity.this, "Quality set to 480p", Toast.LENGTH_SHORT).show();
                setResolution(720, 480, 1500);
                binding.qualityImage.setImageResource(R.drawable.q2);
                break;

            case "720p":
                Toast.makeText(MainActivity.this, "Quality set to 720p", Toast.LENGTH_SHORT).show();
                setResolution(1280, 720, 2500);
                binding.qualityImage.setImageResource(R.drawable.q3);
                break;

            case "1080p":
                Toast.makeText(MainActivity.this, "Quality set to 1080p", Toast.LENGTH_SHORT).show();
                setResolution(1920, 1080, 5000);
                binding.qualityImage.setImageResource(R.drawable.q4);
                break;

            default:
                setResolution(1280, 720, 2500);
                binding.qualityImage.setImageResource(R.drawable.q3);
                break;
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {}
    @Override
    public void onClick(View v) {}
}














