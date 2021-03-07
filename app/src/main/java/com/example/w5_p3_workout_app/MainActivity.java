package com.example.w5_p3_workout_app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ListView lvWorkout;

    private final int EASY_SIGNIFICANT_SHAKE = 600;
    private final int MEDIUM_SIGNIFICANT_SHAKE = 1000;
    private final int HARD_SIGNIFICANT_SHAKE = 1500;
    private final int MAX_SHAKE_COUNT = 100;

    private float lastX, lastY, lastZ;
    private float acceleration;
    private float currentAcceleration;
    private float lastAcceleration;

    private CameraManager CamManager;
    private String CamID;

    private int SIGNIFICANT_SHAKE;
    private int shakeCounter;


    //keep track of which level is activated, levels[0]:easy, levels[1]:medium, levels[2]:hard
    private ArrayList<Boolean> levels;

    //keep track of which sound track is playing, 0: superman, 1: starwars, 2: rocky
    private int indexSong = -1;

    private long start, finish, timeElapsed;

    private Boolean hasStarted, hasLevel, isBlinkEnabled;

    private Camera camera;
    Camera.Parameters params;

    Button btnStart;
    Button btnStop;
    TextView txtNumSteps, txtCurrentLvl;

    MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        mp = MediaPlayer.create(MainActivity.this, R.row)

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        txtNumSteps = (TextView) findViewById(R.id.txtNumSteps);
        txtCurrentLvl = (TextView) findViewById(R.id.txtCurrentLvl);
        //add items to listview
        lvWorkout = (ListView) findViewById(R.id.lvWorkout);
        final String[] Workouts = {"easy", "medium", "hard"};
        ArrayAdapter WorkoutListAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, Workouts);
        lvWorkout.setAdapter(WorkoutListAdapter);

        hasLevel = false;
        hasStarted = false;
        isBlinkEnabled = false;


        //initiate all levels to false before user makes an selection
        levels = new ArrayList<Boolean>();
        levels.add(false);
        levels.add(false);
        levels.add(false);

        acceleration = 0.0f;
        currentAcceleration = SensorManager.GRAVITY_EARTH;
        lastAcceleration = SensorManager.GRAVITY_EARTH;



        CamManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CamID = CamManager.getCameraIdList()[0];  //rear camera is at index 0
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        shakeCounter = 0;

        lvWorkout.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String menuItem = String.valueOf(parent.getItemAtPosition(position));
                Toast.makeText(MainActivity.this, menuItem, Toast.LENGTH_SHORT).show();

                switch (menuItem) {
                    case "easy":
                        SIGNIFICANT_SHAKE = EASY_SIGNIFICANT_SHAKE;
                        levels.set(0, true);
                        levels.set(1, false);
                        levels.set(2, false);
                        Log.i("shakeValue", SIGNIFICANT_SHAKE + "");


                        break;

                    case "medium":
                        SIGNIFICANT_SHAKE = MEDIUM_SIGNIFICANT_SHAKE;
                        levels.set(0, false);
                        levels.set(1, true);
                        levels.set(2, false);
                        break;
                    case "hard":
                        SIGNIFICANT_SHAKE = HARD_SIGNIFICANT_SHAKE;
                        levels.set(0, false);
                        levels.set(1, false);
                        levels.set(2, true);
                        break;
                    default:
                        //default should not be reached
                        Log.i("lvlOnClick","reached default, sth is wrong");
                        break;
                }
                txtCurrentLvl.setText("current: "+menuItem);
                hasLevel = true;
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(hasLevel) {
                    hasStarted = true;
                    if (shakeCounter >= 100) {
                        shakeCounter = 0;
                    }
                    start = System.currentTimeMillis();
                    enableAccelerometerListening();
                }
                else{
                    Toast.makeText(MainActivity.this, "please select a level", Toast.LENGTH_SHORT).show();
                }

            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopWorkout();
                // disableAccelerometerListening();
            }
        });


    }

    private void enableAccelerometerListening() {
        SensorManager sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);

        sensorManager.registerListener(sensorEventListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }


    private void disableAccelerometerListening() {
        SensorManager sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);

        sensorManager.unregisterListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    }

    //
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            //Ref: http://developer.android.com/reference/android/hardware/SensorEvent.html
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            // save previous acceleration value
            lastAcceleration = currentAcceleration;
            // calculate the current acceleration
            currentAcceleration = x * x + y * y + z * z;
            // calculate the change in acceleration
            acceleration = currentAcceleration * (currentAcceleration - lastAcceleration);
            // if the acceleration is above a certain threshold
            if (acceleration > SIGNIFICANT_SHAKE) {
                Log.i("acceleration", "delta x = " + (x - lastX));
                Log.i("acceleration", "delta y = " + (y - lastY));
                Log.i("acceleration", "delta z = " + (z - lastZ));

                shakeCounter++;
                txtNumSteps.setText(shakeCounter + "");

                shakeCounterHelper(shakeCounter);


            }
            else {
                //if shake is insignificant
                lastX = x;
                lastY = y;
                lastZ = z;
                LightOff();
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }








    };

    public void stopWorkout() {
        if(hasStarted) {
            levels.set(0, false);
            levels.set(1, false);
            levels.set(2, false);
            finish = System.currentTimeMillis();
            timeElapsed = finish - start;
            Toast.makeText(MainActivity.this, "duration: "+timeElapsed +" ms", Toast.LENGTH_LONG).show();
            disableAccelerometerListening();

            isBlinkEnabled = false;

            LightOff();
            stopPlaying();


        }
        hasStarted = false;
    }
    private void BlinkFlash(){

        if(isBlinkEnabled) {
            String myString = "010101010101";
            long blinkDelay =50; //Delay in ms
            for (int i = 0; i < myString.length(); i++) {
                if (myString.charAt(i) == '0') {

                    Lighton();



                } else {

                    LightOff();

                }
                // try {
                //     Thread.sleep(blinkDelay);
                // } catch (InterruptedException e) {
                //     e.printStackTrace();
                // }
            }
        }

    }

    public void shakeCounterHelper(int numSteps) {
        //All workouts end automatically after 100 significant shakes.
        if (numSteps >= 100) {
            stopWorkout();
        } // hard workout and flashlight blink
        else if (numSteps >= 60 && levels.get(2)) {
            if(indexSong != 2) {
                stopPlaying();
                indexSong = 2;
            }
            mp = MediaPlayer.create(MainActivity.this, R.raw.rocky_theme);
            mp.start();

        }
        else if (numSteps >= 20 && levels.get(2)) {
            isBlinkEnabled = true;
            BlinkFlash();

        } // easy workout and play Superman
        else if (numSteps >= 30 && levels.get(0)) {

            if(indexSong != 0) {
                stopPlaying();
                indexSong = 0;
            }
            mp = MediaPlayer.create(MainActivity.this, R.raw.superman_theme);
            mp.start();
        }// medium  workout and play Starwars
        else if (numSteps >= 50 && levels.get(1)) {
            if(indexSong != 1) {
                stopPlaying();
                indexSong = 2;
            }
            mp = MediaPlayer.create(MainActivity.this, R.raw.starwar_theme);
            mp.start();
        }// hard workout and play Rpcky

        else{
            //no level selected and not complete the workout
            // Log.i("music", shakeCounter+"");
        }
    }

    public void stopPlaying(){
        //error for music not stopping: E/MediaPlayer: Error (1,-2147483646)
        if(mp!=null){
            Log.i("music", "mp is not null");
            if(mp.isPlaying()) {
                mp.stop();
            }
            mp.reset();
            mp.release();
            mp=null;
        }
    }

    public void Lighton() {
        try {
            CamManager.setTorchMode(CamID, true);
        } catch (CameraAccessException e) {
//            e.PrintStackTrace();
        }
    }

    public void LightOff() {
        try {
            CamManager.setTorchMode(CamID, false);

        } catch (CameraAccessException e) {
//            e.PrintStackTrace();
        }
    }
}
