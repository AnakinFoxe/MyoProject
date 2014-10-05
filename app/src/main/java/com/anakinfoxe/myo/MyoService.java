package com.anakinfoxe.myo;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.XDirection;

public class MyoService extends Service {

    private static final String DEBUG = "debug";

    private Toast mToast;

    private int numIntent;

    // Classes that inherit from AbstractDeviceListener can be used to receive events from Myo devices.
    // If you do not override an event, the default behavior is to do nothing.
    private DeviceListener mListener = new AbstractDeviceListener() {

        private Arm mArm = Arm.UNKNOWN;
//        private XDirection mXDirection = XDirection.UNKNOWN;

        // onConnect() is called whenever a Myo has been connected.
        @Override
        public void onConnect(Myo myo, long timestamp) {
            showToast("Myo Connected");
        }

        // onDisconnect() is called whenever a Myo has been disconnected.
        @Override
        public void onDisconnect(Myo myo, long timestamp) {
            showToast("Myo Disconnected");
        }

        // onArmRecognized() is called whenever Myo has recognized a setup gesture after someone has put it on their
        // arm. This lets Myo know which arm it's on and which way it's facing.
        @Override
        public void onArmRecognized(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
            mArm = arm;
//            mXDirection = xDirection;

            String armSide;
            if (arm.equals(Arm.LEFT))
                armSide = "Left";
            else if (arm.equals(Arm.RIGHT))
                armSide = "Right";
            else
                armSide = "Unknown";
            showToast(armSide + " Arm Recognized");
        }

        // onArmLost() is called whenever Myo has detected that it was moved from a stable position on a person's arm after
        // it recognized the arm. Typically this happens when someone takes Myo off of their arm, but it can also happen
        // when Myo is moved around on the arm.
        @Override
        public void onArmLost(Myo myo, long timestamp) {
            mArm = Arm.UNKNOWN;
//            mXDirection = XDirection.UNKNOWN;
            showToast("Arm Recognition Lost");
        }

        // onOrientationData() is called whenever a Myo provides its current orientation,
        // represented as a quaternion.
        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {

        }

        // onPose() is called whenever a Myo provides a new pose.
        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            // Handle the cases of the Pose enumeration, and change the text of the text view
            // based on the pose we receive.
            switch (pose) {
                case UNKNOWN:
                    showToast("Pose: Unknown");
                    break;
                case REST:
                    switch (mArm) {
                        case LEFT:
                            showToast("Pose: Rest Left");
                            break;
                        case RIGHT:
                            showToast("Pose: Rest Right");
                            break;
                    }
                    break;
                case FIST:
                    break;
                case WAVE_IN:
                    showToast("Pose: Wave In");
                    sendData();
                    break;
                case WAVE_OUT:
                    showToast("Pose: Wave Out");
                    break;
                case FINGERS_SPREAD:
                    showToast("Pose: Fingers Spread");
                    break;
                case THUMB_TO_PINKY:
                    showToast("Pose: Thumb to Pinky");
                    break;
                default:
                    break;
            }
        }
    };



    private void sendData() {
        numIntent++;

        // sendIntent is the object that will be broadcast outside our app
        Intent sendIntent = new Intent();

        // We add flags for example to work from background
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION
                | Intent.FLAG_FROM_BACKGROUND
                | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        // SetAction uses a string which is an important name as it identifies the sender of the itent and that we will give to the receiver to know what to listen.
        // By convention, it's suggested to use the current package name
        sendIntent.setAction("com.anakinfoxe.sendintent.IntentToUnity");

        // Here we fill the Intent with our data, here just a string with an incremented number in it.
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Intent " + numIntent);

        // And here it goes ! our message is send to any other app that want to listen to it.
        sendBroadcast(sendIntent);

        showToast("intent sent " + numIntent);
    };


    @Override
    public IBinder onBind(Intent intent) {
        showToast("MyoService is bound");

        return null;
    }

    @Override
    public void onCreate() {
        Hub hub = Hub.getInstance();
        if (!hub.init(this)) {
            Log.e(DEBUG, "Could not initialize the Hub.");
            return;
        }

        // Next, register for DeviceListener callbacks.
        hub.addListener(mListener);

        // Finally, scan for Myo devices and connect to the first one found.
        hub.pairWithAnyMyo();

        showToast("Congrats! MyoService Created");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        showToast("MyoService Started");
        //Note: You can start a new thread and use it for long background processing from here.
    }

    @Override
    public void onDestroy() {
        showToast("MyoService Stopped");

        // We don't want any callbacks when the Activity is gone, so unregister the listener.
        Hub.getInstance().removeListener(mListener);

        // Shutdown the Hub. This will disconnect from the Myo.
        Hub.getInstance().shutdown();
    }

    private void showToast(String text) {
        Log.w(DEBUG, text);
        if (mToast == null) {
            mToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
        }
        mToast.show();
    }
}
