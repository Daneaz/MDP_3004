package com.example.hanwe.mdp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.hardware.SensorEventListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import com.example.hanwe.mdp.Arena.Arena;
import com.example.hanwe.mdp.Arena.ArenaView;
import com.example.hanwe.mdp.Arena.Robot;
import com.example.hanwe.mdp.Bluetooth.BluetoothDialog;
import com.example.hanwe.mdp.Bluetooth.BluetoothService;
import com.example.hanwe.mdp.Configuration.Config;
import com.example.hanwe.mdp.Configuration.Operation;
import com.example.hanwe.mdp.Configuration.Protocol;
import com.example.hanwe.mdp.Fragments.ControlFragment;
import com.example.hanwe.mdp.Fragments.DebugFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.hardware.SensorManager.SENSOR_DELAY_UI;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SensorEventListener {

    private RelativeLayout btnExplore, btnFastestPath;
    private NavigationView navigationView;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDialog btDialog = null;
    private BluetoothService btService = null;
    private ViewDialog vdDialog = null;
    private final static int REQUEST_ENABLE_BT = 777;
    private final static int REQUEST_ALLOW_COARSE_LOCATION = 888;
    private ImageView btIcon;
    private TextView btStatus, exploreTV, fastestTV, exploreTimeTV, fastestTimeTV;
    private String mConnectedDeviceName = null;
    private String mConnectedDeviceMac = null;
    Handler handlerAutoUpdate = new Handler();
    private DebugFragment debugFragment = null;
    private ControlFragment ctrlFragment = null;
    private boolean setRobotPosition; // Used to determine if I am setting Robot or Waypoint position
    private int oldX, oldY;
    private CountDownTimer countDownTimer = null;

    private long exploreMillisecondTime, exploreStartTime, exploreTimeBuff, exploreUpdateTime = 0L;
    private Handler exploreTimeHandler, sendStartCommandToPcHandler;
    private int exploreSeconds, exploreMinutes, exploreMilliSeconds ;
    private long fastestPathMillisecondTime, fastestPathStartTime, fastestPathTimeBuff, fastestPathUpdateTime = 0L;
    private Handler fastestPathTimeHandler;
    private int fastestPathSeconds, fastestPathMinutes, fastestPathMilliSeconds ;

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;

    private long delayTime;

    Menu menu;
    ArenaView arenaview;

    private Runnable sendStartExploreToPC = new Runnable() {
        @Override
        public void run() {
            BluetoothService.getInstance().sendText(Protocol.PC_MESSAGE_HEADER + "exs", MainActivity.this);
        }
    };

    private Runnable sendStartFastestPathToPC = new Runnable() {
        @Override
        public void run() {
            BluetoothService.getInstance().sendText(Protocol.PC_MESSAGE_HEADER + "fps", MainActivity.this);
        }
    };

    private Runnable exploreRunnable = new Runnable() {
        @Override
        public void run() {
            exploreMillisecondTime = SystemClock.uptimeMillis() - exploreStartTime;
            exploreUpdateTime = exploreTimeBuff + exploreMillisecondTime;
            exploreSeconds = (int) (exploreUpdateTime / 1000);
            exploreMinutes = exploreSeconds / 60;

            String strExploreMinutes = "";
            if(exploreMinutes < 10) {
                strExploreMinutes = "0" + String.valueOf(exploreMinutes);
            } else {
                strExploreMinutes = String.valueOf(exploreMinutes);
            }

            exploreSeconds = exploreSeconds % 60;
            exploreMilliSeconds = (int) (exploreUpdateTime % 1000);

            exploreTimeTV.setText("" + strExploreMinutes + ":"
                    + String.format("%02d", exploreSeconds) + ":"
                    + String.format("%03d", exploreMilliSeconds));

            exploreTimeHandler.postDelayed(this, 0);
        }
    };

    private Runnable fastestPathRunnable = new Runnable() {
        @Override
        public void run() {
            fastestPathMillisecondTime = SystemClock.uptimeMillis() - fastestPathStartTime;
            fastestPathUpdateTime = fastestPathTimeBuff + fastestPathMillisecondTime;
            fastestPathSeconds = (int) (fastestPathUpdateTime / 1000);
            fastestPathMinutes = fastestPathSeconds / 60;

            String strFastestPathMinutes = "";
            if(fastestPathMinutes < 10) {
                strFastestPathMinutes = "0" + String.valueOf(fastestPathMinutes);
            } else {
                strFastestPathMinutes = String.valueOf(fastestPathMinutes);
            }

            fastestPathSeconds = fastestPathSeconds % 60;

            fastestPathMilliSeconds = (int) (fastestPathUpdateTime % 1000);

            fastestTimeTV.setText("" + strFastestPathMinutes + ":"
                    + String.format("%02d", fastestPathSeconds) + ":"
                    + String.format("%03d", fastestPathMilliSeconds));

            fastestPathTimeHandler.postDelayed(this, 0);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the ArenaView
        arenaview = findViewById(R.id.arenaView);
        arenaview.setupArena(new Arena(this));

        // Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        btIcon = findViewById(R.id.icon_bt);
        btStatus = findViewById(R.id.status_bt);

        // Sensor
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // set Touch Listener for the toolbar
        toolbar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // Open Bluetooth Scanning Dialog
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    if (mBluetoothAdapter == null) {
                        // Device doesn't support Bluetooth
                        Toast.makeText(getBaseContext(), "Sorry, your device does not support Bluetooth", Toast.LENGTH_SHORT).show();
                    } else {
                        if (!mBluetoothAdapter.isEnabled()) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                // Check if ACCESS_COARSE_LOCATION is granted
                                if (ContextCompat.checkSelfPermission(MainActivity.this,
                                        Manifest.permission.ACCESS_COARSE_LOCATION)
                                        != PackageManager.PERMISSION_GRANTED) {
                                    // request for ACCESS_COARSE_LOCATION since it is NOT granted
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                            REQUEST_ALLOW_COARSE_LOCATION);
                                } else {
                                    showBTDialog();
                                }
                            } else {
                                showBTDialog();
                            }
                        }
                    }
                    return true;
                }
                return false;
            }
        });

        // Initialize Navigation Drawer
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Initialize Navigation Drawer Items
        navigationView = findViewById(R.id.nav_view);
        // Set click listener to Navigation Drawer Items
        navigationView.setNavigationItemSelectedListener(this);

        // Get the default bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothService btc = (BluetoothService) getLastCustomNonConfigurationInstance();

        if (btc != null)
            btService = btc;

        // Register for broadcasts when a bluetooth device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        // Register for broadcasts when a bluetooth device is pairing.
        IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mPairReceiver, intent);

        // Initialize Setup Tab Button
        RelativeLayout btnSetup = findViewById(R.id.btnSetup);
        // Set click listener to Setup Tab Button
        btnSetup.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(vdDialog == null) {
                    vdDialog = new ViewDialog(MainActivity.this);
                }
                vdDialog.showDialog("Default Setup Configuration", "");
            }
        });

        // Initialize Explore Tab TextView
        exploreTV = findViewById(R.id.txtExplore);
        exploreTimeTV = findViewById(R.id.tvExploreTimeValue);

        // Initialize Explore Tab Button
        btnExplore = findViewById(R.id.btnExplore);
        // Set click listener to Explore Tab Button
        btnExplore.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(exploreTV.getText().equals("Start Explore")) {
                    ctrlFragment.hideController(false);
                    btnFastestPath.setEnabled(false);
                    // Format of message: robot x-coord,robot y-coord,robot dir,waypt x-coord,waypt y-coord
                    StringBuilder stringBuilder = new StringBuilder();
                    // Add Communication Protocol Header
                    stringBuilder.append(Protocol.PC_MESSAGE_HEADER);
                    // Add Robot x-coord
                    stringBuilder.append(String.valueOf(arenaview.getArena().getRobot().getYPos()-1));
                    // Add separator ,
                    stringBuilder.append(",");
                    // Add Robot y-coord
                    stringBuilder.append(String.valueOf(arenaview.getArena().getRobot().getXPos()-1));
                    // Add separator ,
                    stringBuilder.append(",");
                    // Add Robot Direction
                    if(arenaview.getArena().getRobot().getDirection().equals(Robot.Direction.NORTH)) {
                        stringBuilder.append("0");
                    } else if(arenaview.getArena().getRobot().getDirection().equals(Robot.Direction.EAST)) {
                        stringBuilder.append("90");
                    } else if(arenaview.getArena().getRobot().getDirection().equals(Robot.Direction.SOUTH)) {
                        stringBuilder.append("180");
                    } else if(arenaview.getArena().getRobot().getDirection().equals(Robot.Direction.WEST)) {
                        stringBuilder.append("270");
                    }
                    // Add separator ,
                    stringBuilder.append(",");
                    // Add waypoint x-coord
                    stringBuilder.append(String.valueOf(arenaview.getArena().getWayPoint().getYPos()));
                    // Add separator ,
                    stringBuilder.append(",");
                    // Add waypoint y-coord
                    stringBuilder.append(String.valueOf(arenaview.getArena().getWayPoint().getXPos()));
                    BluetoothService.getInstance().sendText(stringBuilder.toString(), MainActivity.this);
                    exploreTV.setText("Stop Explore");
                    // Start Explore Timer
                    exploreStartTime = SystemClock.uptimeMillis();
                    if(exploreTimeHandler == null) {
                        exploreTimeHandler = new Handler();
                    }
                    if(sendStartCommandToPcHandler == null) {
                        sendStartCommandToPcHandler = new Handler();
                    }
                    exploreTimeHandler.postDelayed(exploreRunnable, 0);
                } else {
                    ctrlFragment.hideController(true);
                    btnFastestPath.setEnabled(true);
                    exploreTV.setText("Start Explore");

                    // Pause Explore Timer
                    exploreTimeBuff += exploreMillisecondTime;

                    exploreTimeHandler.removeCallbacks(exploreRunnable);
                    exploreMillisecondTime = 0L;
                    exploreStartTime = 0L;
                    exploreTimeBuff= 0L;
                    exploreUpdateTime = 0L;
                    //BluetoothService.getInstance().sendText(Protocol.PC_MESSAGE_HEADER + "exStop", MainActivity.this);
                }

                // Hide the controller


            }
        });

        // Initialize FastestPath Tab TextView
        fastestTV = findViewById(R.id.txtFastestPath);
        fastestTimeTV = findViewById(R.id.tvFastestTimeValue);

        // Initialize FastestPath Tab Button
        btnFastestPath = findViewById(R.id.btnFasterPath);
        // Set click listener to FastestPath Tab Button
        btnFastestPath.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(fastestTV.getText().equals("Start Fastest Path")) {
                    ctrlFragment.hideController(false);
                    btnExplore.setEnabled(false);
                    fastestTV.setText("Stop Fastest Path");
                    // Start FastestPath Timer
                    fastestPathStartTime = SystemClock.uptimeMillis();
                    if(fastestPathTimeHandler == null) {
                        fastestPathTimeHandler = new Handler();
                    }
                    fastestPathTimeHandler.postDelayed(fastestPathRunnable, 0);
                    if(sendStartCommandToPcHandler == null) {
                        sendStartCommandToPcHandler = new Handler();
                    }
                    sendStartCommandToPcHandler.postDelayed(sendStartFastestPathToPC, 0);
                } else {
                    ctrlFragment.hideController(true);
                    btnExplore.setEnabled(true);
                    fastestTV.setText("Start Fastest Path");
                    // Pause FastestPath Timer
                    fastestPathTimeBuff += fastestPathMillisecondTime;

                    fastestPathTimeHandler.removeCallbacks(fastestPathRunnable);
                    fastestPathMillisecondTime = 0L;
                    fastestPathStartTime = 0L;
                    fastestPathTimeBuff = 0L;
                    fastestPathUpdateTime = 0L;
                    //BluetoothService.getInstance().sendText(Protocol.PC_MESSAGE_HEADER + "fpStop", MainActivity.this);
                }
            }
        });

        if(ctrlFragment== null) {
            ctrlFragment = new ControlFragment();
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, ctrlFragment);
        transaction.commit();

        activateIdleCountDownTimer();
    }

    // Shortcut to set the Robot or Waypoint follow by it's X and Y position
    public void setRobotPosition (boolean s) {
        this.setRobotPosition = s;
        oldX = arenaview.getArena().getRobot().getXPos();
        oldY = arenaview.getArena().getRobot().getYPos();
    }

    // Set inputWayPointPosition
    public void inputWayPointPosition(int type) {
        if (arenaview != null && arenaview.getArena() != null) {

            if(arenaview.getArena().isReset()) {
                enableInputPosition(true, type);
            }
            else {
                Operation.showToast(this, "Please reset your arena first");
            }
        }
    }

    // Enable input position for way point
    private void enableInputPosition(boolean b, int t) {
        if (arenaview != null && arenaview.getArena() != null) {
            arenaview.setInputPositionEnabled(b, t);
        }

        if(t == 0 || t == 1) {
            for (int i = 0; i < menu.size(); i ++)
                menu.getItem(i).setVisible(b);
        }
        else if(t == 2) {
            menu.getItem(1).setVisible(b);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_done:
                enableInputPosition(false, 0);

                // setting robot position done
                if(setRobotPosition) {
                    if(oldX != arenaview.getArena().getRobot().getXPos() ||
                            oldY != arenaview.getArena().getRobot().getYPos()) {
                        /*String newX = String.valueOf(mod((arenaview.getArena().getRobot().getYPos()-15), 15));
                        String newY = String.valueOf(mod((19-arenaview.getArena().getRobot().getXPos()), 20));
                        BluetoothService.getInstance().sendText("Robot position changed from X: " +
                                String.valueOf(mod((oldY-15), 15)) + ", Y: " + String.valueOf(mod((19-oldX), 20)) + " to X: " +
                                newX + ", Y: " + newY + "\n", this);*/
                        BluetoothService.getInstance().sendText("coordinate (" +
                                String.valueOf(arenaview.getArena().getRobot().getYPos()-1) + "," +
                                String.valueOf(arenaview.getArena().getRobot().getXPos()-1) + ")", this);
                    } else {
                        Operation.showToast(this, "No change to the position of the Robot.");
                    }
                }
                // setting waypoint position done
                else {
                    if(oldX != arenaview.getArena().getWayPoint().getXPos() ||
                            oldY != arenaview.getArena().getWayPoint().getYPos()) {
                        String newX = String.valueOf(mod((arenaview.getArena().getWayPoint().getYPos()-15), 15));
                        String newY = String.valueOf(mod((19-arenaview.getArena().getWayPoint().getXPos()), 20));
                        BluetoothService.getInstance().sendText("Waypoint position changed from X: " +
                                String.valueOf(mod((oldY-15), 15)) + ", Y: " + String.valueOf(mod((19-oldX), 20)) + " to X: " +
                                newX + ", Y: " + newY + "\n", this);
                    } else {
                        Operation.showToast(this, "No change to the position of the Robot.");
                    }
                }
                break;

            case R.id.menu_rotate:
                arenaview.getArena().getRobot().turnRight();
                arenaview.invalidate();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.input_waypoint_position, menu);

        for (int i = 0; i < menu.size(); i++){
            menu.getItem(i).setVisible(false);
            Drawable drawable = menu.getItem(i).getIcon();

            if (drawable != null) {
                drawable.mutate();
                drawable.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                btDialog.updateAvailableListView(deviceName + "\n" + deviceHardwareAddress, device);
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //Done searching
                Toast.makeText(context, "Done Searching", Toast.LENGTH_SHORT).show();
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                //Device is about to disconnect
                Toast.makeText(context, "About to disconnect", Toast.LENGTH_SHORT).show();
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Device has disconnected
                Toast.makeText(context, "Disconnected", Toast.LENGTH_SHORT).show();
            }
        }
    };

    // BroadcastReceiver for pairing Bluetooth devices
    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    // Device Paired Successfully
                    btDialog.updatePairedSuccess();
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    // Device Unpaired Successfully
                    btDialog.updateUnpairedSuccess();
                }
            }
        }
    };

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            Log.d(Config.log_id, "Broadcast Message received");
            Log.d(Config.log_id, "Message type: " + intent.getExtras().getInt(Protocol.MESSAGE_TYPE));
            Message msg = Message.obtain();
            msg.what = intent.getExtras().getInt(Protocol.MESSAGE_TYPE);
            msg.setData(intent.getExtras());

            if (intent.getExtras().getInt(Protocol.MESSAGE_ARG1, -99) != -99) {
                msg.arg1 = intent.getExtras().getInt(Protocol.MESSAGE_ARG1, -99);
            }

            handleMessage(msg);
        }
    };

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (debugFragment != null) {
            FragmentManager fm = this.getSupportFragmentManager();
            if(fm.getBackStackEntryCount()>0) {
                fm.popBackStack();
            }
            debugFragment = null;
            navigationView.getMenu().getItem(2).setChecked(false);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_connect) {
            showBTDialog();
        } else if (id == R.id.reset_arena) {
            arenaview.getArena().resetArena();
            ctrlFragment.setMDF1("");
            ctrlFragment.setMDF2("");
            arenaview.invalidate();
            exploreTimeTV.setText("00:00:00");
            fastestTimeTV.setText("00:00:00");
            item.setChecked(false);
            if (exploreTV.getText().equals("Stop Explore")) {
                btnExplore.performClick();
            }
            if(fastestTV.getText().equals("Stop Fastest Path")) {
                btnFastestPath.performClick();
            }
        } else if (id == R.id.nav_log) {
            Intent intent = new Intent(this, LogActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_debug) {
            if(debugFragment == null) {
                debugFragment = new DebugFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, debugFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            } else {
                FragmentManager fm = this.getSupportFragmentManager();
                if(fm.getBackStackEntryCount()>0) {
                    fm.popBackStack();
                }
                debugFragment = null;
                item.setChecked(false);
            }
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Enable Bluetooth Request
        if (requestCode == REQUEST_ENABLE_BT) {
            // Bluetooth Enabled
            if (resultCode == RESULT_OK) {
                // Only ask for these permissions on runtime when running Android 6.0 or higher
                // Bluetooth Discovery requires Coarse Location for device running Android 6.0 or higher
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Check if ACCESS_COARSE_LOCATION is granted
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        // request for ACCESS_COARSE_LOCATION since it is NOT granted
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                REQUEST_ALLOW_COARSE_LOCATION);
                    } else {
                        showBTDialog();
                    }
                }
            } else {
                Toast.makeText(getBaseContext(), "You need to turn on your Bluetooth", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_ALLOW_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    showBTDialog();
                } else {
                    // permission denied
                    Toast.makeText(getBaseContext(), "Bluetooth Discovery requires Coarse Location Permission\nfor device running Android 6.0 or higher", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
        unregisterReceiver(mPairReceiver);
        removeSchedulerCallBack();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        senSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Restore the connected bluetooth device name and address
        mConnectedDeviceName = getSharedPreferences("protocolPref", MODE_PRIVATE).getString(Protocol.DEVICE_NAME, "");
        mConnectedDeviceMac = getSharedPreferences("protocolPref", MODE_PRIVATE).getString(Protocol.DEVICE_MAC, "");
        IntentFilter intentFilter = new IntentFilter("BTLocalBroadcast");
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, intentFilter);
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.

        if (btService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            // Start the Bluetooth chat services

            if (btService.getConnectionState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                btService.initialiseService();
            }

            if (btService.getConnectionState() == BluetoothService.STATE_LISTEN) {
                if(btStatus == null || btIcon == null) {
                    btIcon = (ImageView) findViewById(R.id.icon_bt);
                    btStatus = (TextView) findViewById(R.id.status_bt);
                }
                btIcon.setImageResource(R.drawable.disconnected_bluetooth);
                btStatus.setText(this.getString(R.string.disconnected_bt));
                if(btDialog == null) {
                    btDialog = new BluetoothDialog(MainActivity.this, btService);
                }
                btDialog.updateList(mConnectedDeviceName, mConnectedDeviceMac, false);

            } else if (btService.getConnectionState() == BluetoothService.STATE_CONNECTED) {
                if(btStatus == null || btIcon == null) {
                    btIcon = (ImageView) findViewById(R.id.icon_bt);
                    btStatus = (TextView) findViewById(R.id.status_bt);
                }
                btIcon.setImageResource(R.drawable.connected_bluetooth);
                btStatus.setText("Connected to " + mConnectedDeviceName);
                if(btDialog == null) {
                    btDialog = new BluetoothDialog(MainActivity.this, btService);
                }
                btDialog.updateList(mConnectedDeviceName, mConnectedDeviceMac, true);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.

        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

            } else if (btService == null) {
                Log.e(Config.log_id, "on start bluetooth service restart");
                // Initialize the BluetoothChatService to perform bluetooth connections
                btService = BluetoothService.getInstance();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        removeSchedulerCallBack();

        // Save the connected bluetooth device name and address
        SharedPreferences.Editor editor = getSharedPreferences(Protocol.PROTOCOLPREF, MODE_PRIVATE).edit();
        editor.putString(Protocol.DEVICE_NAME, mConnectedDeviceName);
        editor.putString(Protocol.DEVICE_MAC, mConnectedDeviceMac);
        editor.commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Protocol.DEVICE_NAME, mConnectedDeviceName);
        outState.putString(Protocol.DEVICE_MAC, mConnectedDeviceMac);
        //savedInstanceState.putBoolean("isAccelerometerEnabled", isAccelerometerEnabled);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mConnectedDeviceName = savedInstanceState.getString(Protocol.DEVICE_NAME);
        mConnectedDeviceMac = savedInstanceState.getString(Protocol.DEVICE_MAC);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return  btService;
    }

    private void showBTDialog() {
        if(btDialog == null) {
            if(btService == null) {
                // Initialize the BluetoothChatService to perform bluetooth connections
                btService = BluetoothService.getInstance();
            }
            btDialog = new BluetoothDialog(MainActivity.this, btService);
        }

        btDialog.showDialog("Scan for Bluetooth Devices", btService);
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case Protocol.MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                    case BluetoothService.STATE_CONNECTED:
                        btIcon.setImageResource(R.drawable.connected_bluetooth);
                        btStatus.setText("Connected to " + mConnectedDeviceName);
                        if(btDialog == null) {
                            if (btService == null) {
                                // Initialize the BluetoothChatService to perform bluetooth connections
                                btService = BluetoothService.getInstance();
                            }
                            btDialog = new BluetoothDialog(MainActivity.this, btService);
                        }
                        btDialog.updateList(mConnectedDeviceName, mConnectedDeviceMac, true);
                        break;

                    case BluetoothService.STATE_CONNECTING:
                        btIcon.setImageResource(R.drawable.disconnected_bluetooth);
                        if(mConnectedDeviceName != null) {
                            writeToLog("Connecting to\n" + mConnectedDeviceName + ", " + mConnectedDeviceMac);
                        }
                        btStatus.setText("Connecting to " + mConnectedDeviceName);
                        break;

                    case BluetoothService.STATE_LISTEN:
                    case BluetoothService.STATE_NONE:
                        btIcon.setImageResource(R.drawable.disconnected_bluetooth);
                        btStatus.setText(this.getString(R.string.disconnected_bt));
                        if(btDialog == null) {
                            if(btService == null) {
                                // Initialize the BluetoothChatService to perform bluetooth connections
                                btService = BluetoothService.getInstance();
                            }
                            btDialog = new BluetoothDialog(MainActivity.this, btService);
                        }

                        if (msg.arg1 == BluetoothService.STATE_NONE) {
                            writeToLog("Disconnected from\n" + mConnectedDeviceName + ", " + mConnectedDeviceMac);
                        }
                        btDialog.updateList(mConnectedDeviceName, mConnectedDeviceMac, false);
                        break;
                }

                break;

            case Protocol.MESSAGE_WRITE:
                byte[] writeBuf = msg.getData().getByteArray(Protocol.MESSAGE_BUFFER);
                String writeMessage = new String(writeBuf);
                writeToLog("Message sent to\n" + mConnectedDeviceName + ", " + mConnectedDeviceMac + ": \n" + writeMessage);
                break;

            case Protocol.MESSAGE_READ:
                int bytes = msg.getData().getInt(Protocol.MESSAGE_BYTES);
                byte[] buffer = msg.getData().getByteArray(Protocol.MESSAGE_BUFFER);
                String readMessage = new String(buffer, 0, bytes).trim();
                writeToLog("Message from\n" + mConnectedDeviceName + ", " + mConnectedDeviceMac + ":\n" + readMessage);
                if(debugFragment != null) {
                    debugFragment.setReceiveET(readMessage);
                }

                // GridMap Update String from PC
                if(readMessage.equalsIgnoreCase("exr")) {
                    sendStartCommandToPcHandler.postDelayed(sendStartExploreToPC, 0);
                } else if(readMessage.startsWith("{\"robot\":")) {
                    readMessage = readMessage.replace("}{", "}, {");
                    readMessage = readMessage.replace("} {", "}, {");
                    readMessage = "[ " + readMessage + " ]";
                    String robot = "";
                    try {
                        JSONArray json = new JSONArray(readMessage);
                        JSONObject obj = json.getJSONObject(json.length()-1);
                        robot = obj.getString("robot").trim();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    /*message[0]:part1, message[1]:part2, message[2]:robot y-coord count from 1 from below,
                      message[3]:robot x-coord count from 1 from left, message[4]: robot head direction */
                    String[] message = robot.split(",");

                    arenaview.getArena().getRobot().setPosition(Integer.valueOf(message[2]), Integer.valueOf(message[3]));

                    switch((Integer.parseInt(message[4].trim()))) {
                        case 0: arenaview.getArena().getRobot().setDirection(Robot.Direction.NORTH);
                            break;
                        case 90: arenaview.getArena().getRobot().setDirection(Robot.Direction.EAST);
                            break;
                        case 180: arenaview.getArena().getRobot().setDirection(Robot.Direction.SOUTH);
                            break;
                        case 270: arenaview.getArena().getRobot().setDirection(Robot.Direction.WEST);
                            break;
                        default:
                    }
                    handlePCGridUpdate(message[0], message[1]);

                    if(ctrlFragment.getAutoStatus()) {
                        arenaview.invalidate();
                    }
                } else if(readMessage.equalsIgnoreCase("exploreComplete")) {
                    if(exploreTV.getText().equals("Stop Explore")) {
                        btnExplore.performClick();
                    }
                } else if(readMessage.equalsIgnoreCase("S")) {
                    if(fastestTV.getText().equals("Stop Fastest Path")) {
                        btnFastestPath.performClick();
                    }
                }
                //Message may come from AMD Tool
                else {
                    // AMD sent a move forward
                    if(readMessage.equals(Protocol.AMD_MOVE_FORWARD)) {
                        arenaview.getArena().getRobot().moveForward(true);
                        BluetoothService.getInstance().sendText(Protocol.AMD_MOVE_FORWARD.toString(), this);
                        if(ctrlFragment != null) {
                            ctrlFragment.setStatus(Protocol.STATUS_FORWARD);
                        }
                        arenaview.invalidate();
                    }
                    // AMD sent a turn left
                    else if(readMessage.equals(Protocol.AMD_TURN_LEFT)) {
                        arenaview.getArena().getRobot().turnLeft();
                        BluetoothService.getInstance().sendText(Protocol.AMD_TURN_LEFT.toString(), this);
                        if(ctrlFragment != null) {
                            ctrlFragment.setStatus(Protocol.STATUS_TURN_LEFT);
                        }
                        arenaview.invalidate();
                    }
                    // AMD sent a turn right
                    else if(readMessage.equals(Protocol.AMD_TURN_RIGHT)) {
                        arenaview.getArena().getRobot().turnRight();
                        BluetoothService.getInstance().sendText(Protocol.AMD_TURN_RIGHT.toString(), this);
                        if(ctrlFragment != null) {
                            ctrlFragment.setStatus(Protocol.STATUS_TURN_RIGHT);
                        }
                        arenaview.invalidate();
                    }
                    // AMD sent a reverse
                    else if(readMessage.equals(Protocol.AMD_REVERSE)) {
                        arenaview.getArena().getRobot().reverse(true);
                        BluetoothService.getInstance().sendText(Protocol.AMD_REVERSE.toString(), this);
                        if(ctrlFragment != null) {
                            ctrlFragment.setStatus(Protocol.STATUS_REVERSE);
                        }
                        arenaview.invalidate();
                    }
                    // AMD sent a grid string
                    else if(readMessage.startsWith("{\"grid")) {
                        String gridData = readMessage.substring(11, readMessage.length()-2);
                        String binaryGridData = Operation.hexToBinary(gridData);
                        //writeToLog("Read :\n" + gridData + "\nConverted to:\n" + binaryGridData);
                        handleGridUpdate(binaryGridData);
                    }
                    // For Testing AMD Status Checkpoint
                    else if(readMessage.startsWith("{\"status\":")){
                        ctrlFragment.setStatus(readMessage.substring(11, readMessage.length()-2));
                    }
                    // For Updating of AMD Robot position
                    else if(readMessage.startsWith("{\"robotPosition\" :")) {
                        readMessage = readMessage.substring(20, readMessage.length()-2);
                        String[] message = readMessage.split(",");

                        arenaview.getArena().getRobot().setPosition(Integer.parseInt(message[1].trim()) + 1, Integer.parseInt(message[0].trim()) + 1);
                        switch((Integer.parseInt(message[2].trim()))) {
                            case 0: arenaview.getArena().getRobot().setDirection(Robot.Direction.NORTH);
                                break;
                            case 90: arenaview.getArena().getRobot().setDirection(Robot.Direction.EAST);
                                break;
                            case 180: arenaview.getArena().getRobot().setDirection(Robot.Direction.SOUTH);
                                break;
                            case 270: arenaview.getArena().getRobot().setDirection(Robot.Direction.WEST);
                                break;
                            default:
                        }
                        if(ctrlFragment.getAutoStatus()) {
                            arenaview.invalidate();
                        }
                    }
                }
                break;

            case Protocol.MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(Protocol.DEVICE_NAME);
                mConnectedDeviceMac = msg.getData().getString(Protocol.DEVICE_MAC);
                if(btService.getConnectionState() == BluetoothService.STATE_CONNECTED) {
                    writeToLog("Connected to\n" + mConnectedDeviceName + ", " + mConnectedDeviceMac);
                    Operation.showToast(getApplicationContext(), "Connected to " + mConnectedDeviceName);
                }
                break;

            case Protocol.MESSAGE_TOAST:
                Operation.showToast(getApplicationContext(), msg.getData().getString(Protocol.TOAST));
                break;
        }
    }
    public void removeSchedulerCallBack() {
        try {
            handlerAutoUpdate.removeCallbacksAndMessages(null);
        } catch (Exception e) {
            Log.e(Config.log_id, e.getMessage());
        }
    }

    public void setBtService(BluetoothService btService) {
        this.btService = btService;
    }

    public String getConnectedDeviceName() {
        return this.mConnectedDeviceName;
    }

    public String getConnectedDeviceMac() { return this.mConnectedDeviceMac; }

    public ArenaView getArenaview() {
        return arenaview;
    }

    private void writeToLog(String content){
        File fileDir = new File(getFilesDir(),Protocol.LOG_FILE_DIR);
        if(!fileDir.exists()){
            fileDir.mkdir();
        }

        try{
            File logFile = new File(fileDir, Protocol.LOG_FILE_NAME);
            FileWriter writer = new FileWriter(logFile, true);
            SimpleDateFormat simpledateformat = new SimpleDateFormat("dd MMM yyyy, HH:mm");
            String dateTime = simpledateformat.format(Calendar.getInstance().getTime());
            writer.append(dateTime + ": " + content + "\n\n");
            writer.flush();
            writer.close();
        }catch (Exception e){
            e.printStackTrace();

        }
    }

    public int mod(int value, int modValue)
    {
        int result = value % modValue;
        return result < 0? result + modValue : result;
    }

    // Set the status to Idle every 5 sec
    public void activateIdleCountDownTimer() {
        if(countDownTimer != null)
            countDownTimer.cancel();

        countDownTimer = new CountDownTimer(5000, 1000) {

            public void onTick(long millisUntilFinished) {
                // do nothing every sec until 5sec reach
            }

            public void onFinish() {
                if(ctrlFragment != null) {
                    ctrlFragment.setStatus(Protocol.STATUS_IDLE);
                }
            }
        }.start();
    }

    private void handleGridUpdate(String gridData) {
        arenaview.getArena().updateGridMap(gridData);
        ctrlFragment.setMDF1(arenaview.getArena().getMDF1());
        ctrlFragment.setMDF2(arenaview.getArena().getMDF2());
        if(ctrlFragment.getAutoStatus()) {
            arenaview.invalidate();
        }
    }

    private void handlePCGridUpdate(String part1, String part2) {
        arenaview.getArena().updateGridMapFromPc(Operation.hexToBinary(part1), Operation.hexToBinary(part2));
        ctrlFragment.setMDF1(arenaview.getArena().getMDF1());
        ctrlFragment.setMDF2(arenaview.getArena().getMDF2());
        if(ctrlFragment.getAutoStatus()) {
            arenaview.invalidate();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long systemTime = System.currentTimeMillis();
        long time = systemTime - delayTime;
        if(time > 500) {
            delayTime = systemTime;
            float x = event.values[0];
            float y = event.values[1];
            if (Math.abs(x) > Math.abs(y)) {
                if (x < 0) {
                    arenaview.getArena().getRobot().turnRight();
                    ctrlFragment.setStatus(Protocol.STATUS_TURN_RIGHT);
                    activateIdleCountDownTimer();
                    if(getConnectedDeviceName() != null && getConnectedDeviceName().equalsIgnoreCase(Protocol.AMD_DEVICE_NAME) &&
                            getConnectedDeviceMac() != null && getConnectedDeviceMac().equalsIgnoreCase(Protocol.AMD_DEVICE_MAC)) {
                        BluetoothService.getInstance().sendText(Protocol.AMD_TURN_RIGHT.toString(), this);
                    }
                }
                if (x > 0) {
                    arenaview.getArena().getRobot().turnLeft();
                    ctrlFragment.setStatus(Protocol.STATUS_TURN_LEFT);
                    activateIdleCountDownTimer();
                    if(getConnectedDeviceName() != null && getConnectedDeviceName().equalsIgnoreCase(Protocol.AMD_DEVICE_NAME) &&
                            getConnectedDeviceMac() != null && getConnectedDeviceMac().equalsIgnoreCase(Protocol.AMD_DEVICE_MAC)) {
                        BluetoothService.getInstance().sendText(Protocol.AMD_TURN_LEFT.toString(), this);
                    }
                }
            } else {
                if (y < 0) {
                    arenaview.getArena().getRobot().moveForward(true);
                    ctrlFragment.setStatus(Protocol.STATUS_FORWARD);
                    activateIdleCountDownTimer();
                    if(getConnectedDeviceName() != null && getConnectedDeviceName().equalsIgnoreCase(Protocol.AMD_DEVICE_NAME) &&
                            getConnectedDeviceMac() != null && getConnectedDeviceMac().equalsIgnoreCase(Protocol.AMD_DEVICE_MAC)) {
                        BluetoothService.getInstance().sendText(Protocol.AMD_MOVE_FORWARD.toString(), this);
                    }
                }
                if (y > 0) {
                    arenaview.getArena().getRobot().reverse(true);
                    ctrlFragment.setStatus(Protocol.STATUS_REVERSE);
                    activateIdleCountDownTimer();
                    if(getConnectedDeviceName() != null && getConnectedDeviceName().equalsIgnoreCase(Protocol.AMD_DEVICE_NAME) &&
                            getConnectedDeviceMac() != null && getConnectedDeviceMac().equalsIgnoreCase(Protocol.AMD_DEVICE_MAC)) {
                        BluetoothService.getInstance().sendText(Protocol.AMD_REVERSE.toString(), this);
                    }
                }
            }
            if (x > (-2) && x < (2) && y > (-2) && y < (2)) {
                ctrlFragment.setStatus(Protocol.STATUS_IDLE);
            }
            arenaview.invalidate();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void activateTiltSensing(){
        senSensorManager.registerListener(this, senAccelerometer , SENSOR_DELAY_UI);
        delayTime = System.currentTimeMillis();
    }

    public void deactivateTiltSensing() {
        senSensorManager.unregisterListener(this);
    }
}