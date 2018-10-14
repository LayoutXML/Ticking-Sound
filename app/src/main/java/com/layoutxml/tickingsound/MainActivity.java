package com.layoutxml.tickingsound;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.activity.WearableActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.layoutxml.tickingsound.activities.ActivityTextViewActivity;
import com.layoutxml.tickingsound.objects.ActivityOption;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends WearableActivity {

    private MediaPlayer mediaPlayer;
    private SharedPreferences sharedPreferences;
    private List<ActivityOption> values = new ArrayList<>();
    private SettingsListAdapter mAdapter;
    private Animation pulse;
    //Temporary values
    private Integer currentBattery;
    private Boolean isPlaying; //describes whether functionality has been started - whether player pressed play (not looking at restrictions)
    private Boolean isRestricted;
    private Boolean isPaused;
    private Boolean isCharging;
    //Preferences
    private Integer maxVolume = 11;
    private Integer currentVolume = 6;
    //Preferences - restrictions
    private Integer minimumBattery;
    private Integer maximumBattery;
    private Boolean whileCharging;
    private Boolean whileNotCharging;
    //Elements
    private Button button;
    private ImageView buttonIcon;
    private ConstraintLayout constraintLayout;
    private TextView volumeText;
    private Button volumeDown;
    private Button volumeUp;
    private RecyclerView recyclerView;

    private BroadcastReceiver batteryBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            currentBattery = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100);
            int pluggedIn = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            isCharging = pluggedIn == BatteryManager.BATTERY_PLUGGED_AC || pluggedIn == BatteryManager.BATTERY_PLUGGED_USB || pluggedIn == BatteryManager.BATTERY_PLUGGED_WIRELESS;
            checkRestrictions();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mediaPlayer  = new MediaPlayer();

        button = findViewById(R.id.button_background);
        buttonIcon = findViewById(R.id.button_icon);
        constraintLayout = findViewById(R.id.constraintLayout);
        volumeText = findViewById(R.id.volumeText);
        volumeDown = findViewById(R.id.volumeDown);
        volumeUp = findViewById(R.id.volumeUp);
        recyclerView = findViewById(R.id.settings_list);

        isPaused = false;
        isRestricted = false;
        currentBattery = 100;
        isCharging = false;

        sharedPreferences = getSharedPreferences(getString(R.string.sharedPreferences),Context.MODE_PRIVATE);
        loadPreferences();
        checkRestrictions();

        if (isPlaying || getIntent().getBooleanExtra("fromBoot",false))
            stopTicking(false,false);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new SettingsListAdapter();
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        if (isPlaying) {
            buttonIcon.setImageDrawable(getDrawable(R.drawable.ic_pause));
        } else {
            buttonIcon.setImageDrawable(getDrawable(R.drawable.ic_play));
        }

        //changeVolume();

        volumeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentVolume++;
                changeVolume();
            }
        });

        volumeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentVolume--;
                changeVolume();
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlaying) {
                    stopTicking(false,false);
                } else {
                    startTicking(false);
                }
            }
        });

        generateSettingsListValues();
        this.registerReceiver(this.batteryBroadcastReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        pulsate();
    }

    private void loadPreferences() {
        isPlaying = sharedPreferences.getBoolean(getString(R.string.isPlayingKey_preference),false);
        currentVolume = sharedPreferences.getInt(getString(R.string.volume_preference),6);
        minimumBattery = sharedPreferences.getInt(getString(R.string.minBattery_preference),0);
        maximumBattery = sharedPreferences.getInt(getString(R.string.maxBattery_preference),100);
        whileCharging = sharedPreferences.getBoolean(getString(R.string.charging_preference),true);
        whileNotCharging = sharedPreferences.getBoolean(getString(R.string.notcharging_preference),true);
    }

    private void checkRestrictions() {
        loadPreferences();
        isRestricted = !(currentBattery >= minimumBattery && currentBattery <= maximumBattery);
        if (!isRestricted) {
            if (isCharging) {
                if (!whileCharging)
                    isRestricted = true;
            } else {
                if (!whileNotCharging)
                    isRestricted = true;
            }
        }

        actOnRestrictions();
    }

    private void actOnRestrictions() {
        if (isPlaying && !isPaused && isRestricted) {
            //Pause
            isPaused = true;
            stopTicking(false,true);
        }
        else if (isPlaying && isPaused && !isRestricted) {
            //Continue
            isPaused = false;
            startTicking(true);
        }

        if (isRestricted) {
            button.setBackgroundResource(R.drawable.ic_circle_grayscale);
        } else {
            button.setBackgroundResource(R.drawable.ic_circle);
        }
    }

    private void generateSettingsListValues() {
        ActivityOption activityOption = new ActivityOption();
        activityOption.setName("Restrictions");
        activityOption.setExtra("restrictions");
        activityOption.setActivity(ActivityTextViewActivity.class);
        values.add(activityOption);

        activityOption = new ActivityOption();
        activityOption.setName("Integrations");
        activityOption.setExtra("integrations");
        activityOption.setActivity(ActivityTextViewActivity.class);
        values.add(activityOption);

        activityOption = new ActivityOption();
        activityOption.setName("About");
        activityOption.setExtra("about");
        activityOption.setActivity(ActivityTextViewActivity.class);
        values.add(activityOption);

        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkRestrictions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTicking(true,false);
        unregisterReceiver(batteryBroadcastReceiver);
    }

    private void stopTicking(Boolean forced, Boolean paused) {
        if (isPlaying && forced) {
            Toast.makeText(this,"To resume sound, minimize the app instead of closing it",Toast.LENGTH_LONG).show();
        }
        if (mediaPlayer!=null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        if (!paused) {
            isPlaying = false;
            buttonIcon.setImageDrawable(getDrawable(R.drawable.ic_play));
            sharedPreferences.edit().putBoolean(getString(R.string.isPlayingKey_preference),isPlaying).apply();
        }
        pulsate();
    }

    private void startTicking(Boolean fromPaused) {
        mediaPlayer = MediaPlayer.create(this,R.raw.ticking_sound);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
        changeVolume();
        if (!fromPaused) {
            isPlaying = true;
            buttonIcon.setImageDrawable(getDrawable(R.drawable.ic_pause));
            sharedPreferences.edit().putBoolean(getString(R.string.isPlayingKey_preference), isPlaying).apply();
        }
        pulsate();
    }

    private void changeVolume() {
        if (currentVolume>=1 && currentVolume<=11) {
            float newVolume = (float) (Math.log(maxVolume - currentVolume) / Math.log(maxVolume));
            mediaPlayer.setVolume(1 - newVolume, 1 - newVolume);
            updateVolumeText();
        } else {
            if (currentVolume<=1)
                currentVolume=1;
            else
                currentVolume=11;
        }
        sharedPreferences.edit().putInt(getString(R.string.volume_preference),currentVolume).apply();
    }

    private void updateVolumeText() {
        String current = (currentVolume-1<10) ? "0"+(currentVolume-1) : (currentVolume-1)+"";
        volumeText.setText("Volume: "+current+"/"+(maxVolume-1));
        if (currentVolume.equals(maxVolume)) {
            volumeUp.setBackgroundResource(R.drawable.ic_circle_grayscale);
            volumeDown.setBackgroundResource(R.drawable.ic_circle);
        } else if (currentVolume.equals(1)) {
            volumeUp.setBackgroundResource(R.drawable.ic_circle);
            volumeDown.setBackgroundResource(R.drawable.ic_circle_grayscale);
        } else {
            volumeUp.setBackgroundResource(R.drawable.ic_circle);
            volumeDown.setBackgroundResource(R.drawable.ic_circle);
        }
    }

    public void pulsate() {
        if (!isPlaying) {
            pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
            button.startAnimation(pulse);
        } else {
            pulse.setRepeatCount(0);
        }
    }

    public class SettingsListAdapter extends RecyclerView.Adapter<SettingsListAdapter.MyViewHolder>{

        class MyViewHolder extends RecyclerView.ViewHolder {

            TextView name;

            MyViewHolder(View view) {
                super(view);
                name = view.findViewById(R.id.textView);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getAdapterPosition(); // gets item position
                        Intent intent = new Intent(MainActivity.this, values.get(position).getActivity());
                        intent.putExtra("Activity",values.get(position).getExtra());
                        MainActivity.this.startActivity(intent);
                    }
                });
            }
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.textview_item,parent,false);
            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            ActivityOption activityOption = values.get(position);
            holder.name.setText(activityOption.getName());
        }

        @Override
        public int getItemCount() {
            return values.size();
        }

    }
}
