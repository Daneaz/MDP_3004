package com.example.hanwe.mdp.Fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.hanwe.mdp.Arena.ArenaView;
import com.example.hanwe.mdp.Bluetooth.BluetoothService;
import com.example.hanwe.mdp.Configuration.Operation;
import com.example.hanwe.mdp.Configuration.Protocol;
import com.example.hanwe.mdp.MainActivity;
import com.example.hanwe.mdp.R;

import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;

public class ControlFragment extends Fragment {
    private TextView tvStatus, tvMdfstring1, tvMdfstring2;
    private Button autoBtn, updateBtn;
    private ArenaView arenaView;
    private SharedPreferences sharedPrefs;
    private boolean preferencesExist = false;
    private boolean autoMode = true;
    private Handler leftDelayHandler = null, rightDelayHandler = null, forwardDelayHandler = null, reverseDelayHandler = null;
    private ArrayList<String> leftMessageBuffer, rightMessageBuffer;

    ImageView ctrl_top;
    ImageView ctrl_left;
    ImageView ctrl_right;
    ImageView ctrl_reverse;
    ImageView ctrl_center;

    public ControlFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_control, container, false);

        tvMdfstring1 = view.findViewById(R.id.tvMdfstring1);
        tvMdfstring2 = view.findViewById(R.id.tvMdfstring2);
        tvStatus = view.findViewById(R.id.tvStatus);
        autoBtn = view.findViewById(R.id.btnAuto);
        updateBtn = view.findViewById(R.id.btnManual);
        updateBtn.setVisibility(View.GONE);
        ctrl_top = view.findViewById(R.id.ctrl_top);
        ctrl_left = view.findViewById(R.id.ctrl_left);
        ctrl_right = view.findViewById(R.id.ctrl_right);
        ctrl_reverse = view.findViewById(R.id.ctrl_bottom);
        ctrl_center = view.findViewById(R.id.center);
        arenaView = ((MainActivity)getActivity()).getArenaview();
        leftMessageBuffer = new ArrayList<>();
        rightMessageBuffer = new ArrayList<>();

        sharedPrefs = getActivity().getSharedPreferences(Protocol.PROTOCOLPREF, MODE_PRIVATE);

        //Left Rotate control button
        ctrl_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(leftDelayHandler == null && leftMessageBuffer.isEmpty()) {
                    setStatus(Protocol.STATUS_TURN_LEFT);
                    ((MainActivity)getActivity()).activateIdleCountDownTimer();
                    if(((MainActivity)getActivity()).getConnectedDeviceName() != null && ((MainActivity)getActivity()).getConnectedDeviceName().equalsIgnoreCase(Protocol.AMD_DEVICE_NAME) &&
                            ((MainActivity)getActivity()).getConnectedDeviceMac() != null && ((MainActivity)getActivity()).getConnectedDeviceMac().equalsIgnoreCase(Protocol.AMD_DEVICE_MAC)) {
                        BluetoothService.getInstance().sendText(Protocol.AMD_TURN_LEFT.toString(), getActivity());
                    } else if(preferencesExist) {
                        BluetoothService.getInstance().sendText("HA:" + sharedPrefs.getString(getActivity().getResources().getString(R.string.rotateLeft), ""), getActivity());
                    } else {
                        BluetoothService.getInstance().sendText("HA:" + Protocol.TURN_LEFT.toString(), getActivity());
                    }
                    leftDelayHandler = new Handler();
                    Runnable r=new Runnable() {
                        public void run() {
                            if(!leftMessageBuffer.isEmpty()) {
                                setStatus(Protocol.STATUS_TURN_LEFT);
                                ((MainActivity)getActivity()).activateIdleCountDownTimer();
                                BluetoothService.getInstance().sendText(leftMessageBuffer.get(0), getActivity());
                                leftMessageBuffer.remove(0);
                                leftDelayHandler.postDelayed(this, 330);
                                arenaView.getArena().getRobot().turnLeft();
                                arenaView.invalidate();
                            } else {
                                leftDelayHandler = null;
                            }
                        }
                    };
                    leftDelayHandler.postDelayed(r, 330);
                    arenaView.getArena().getRobot().turnLeft();
                    arenaView.invalidate();
                } else {
                    if(((MainActivity)getActivity()).getConnectedDeviceName() != null && ((MainActivity)getActivity()).getConnectedDeviceName().equalsIgnoreCase(Protocol.AMD_DEVICE_NAME) &&
                            ((MainActivity)getActivity()).getConnectedDeviceMac() != null && ((MainActivity)getActivity()).getConnectedDeviceMac().equalsIgnoreCase(Protocol.AMD_DEVICE_MAC)) {
                        leftMessageBuffer.add(Protocol.AMD_TURN_LEFT.toString());
                    } else if(preferencesExist) {
                        leftMessageBuffer.add("HA:" + sharedPrefs.getString(getActivity().getResources().getString(R.string.rotateLeft), ""));
                    } else {
                        leftMessageBuffer.add("HA:" + Protocol.TURN_LEFT.toString());
                    }
                }
            }
        });

        //Right Rotate control button
        ctrl_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rightDelayHandler == null && rightMessageBuffer.isEmpty()) {
                    setStatus(Protocol.STATUS_TURN_RIGHT);
                    ((MainActivity)getActivity()).activateIdleCountDownTimer();
                    if(((MainActivity)getActivity()).getConnectedDeviceName() != null && ((MainActivity)getActivity()).getConnectedDeviceName().equalsIgnoreCase(Protocol.AMD_DEVICE_NAME) &&
                            ((MainActivity)getActivity()).getConnectedDeviceMac() != null && ((MainActivity)getActivity()).getConnectedDeviceMac().equalsIgnoreCase(Protocol.AMD_DEVICE_MAC)) {
                        BluetoothService.getInstance().sendText(Protocol.AMD_TURN_RIGHT.toString(), getActivity());
                    } else if(preferencesExist) {
                        BluetoothService.getInstance().sendText("HA:" + sharedPrefs.getString(getActivity().getResources().getString(R.string.rotateRight), ""), getActivity());
                    } else {
                        BluetoothService.getInstance().sendText("HA:" + Protocol.TURN_RIGHT.toString(), getActivity());
                    }
                    rightDelayHandler = new Handler();
                    Runnable r=new Runnable() {
                        public void run() {
                            if(!rightMessageBuffer.isEmpty()) {
                                setStatus(Protocol.STATUS_TURN_RIGHT);
                                ((MainActivity)getActivity()).activateIdleCountDownTimer();
                                BluetoothService.getInstance().sendText(rightMessageBuffer.get(0), getActivity());
                                rightMessageBuffer.remove(0);
                                rightDelayHandler.postDelayed(this, 330);
                                arenaView.getArena().getRobot().turnRight();
                                arenaView.invalidate();
                            } else {
                                rightDelayHandler = null;
                            }
                        }
                    };
                    rightDelayHandler.postDelayed(r, 330);
                    arenaView.getArena().getRobot().turnRight();
                    arenaView.invalidate();
                } else {
                    if(((MainActivity)getActivity()).getConnectedDeviceName() != null && ((MainActivity)getActivity()).getConnectedDeviceName().equalsIgnoreCase(Protocol.AMD_DEVICE_NAME) &&
                            ((MainActivity)getActivity()).getConnectedDeviceMac() != null && ((MainActivity)getActivity()).getConnectedDeviceMac().equalsIgnoreCase(Protocol.AMD_DEVICE_MAC)) {
                        rightMessageBuffer.add(Protocol.AMD_TURN_RIGHT.toString());
                    } else if(preferencesExist) {
                        rightMessageBuffer.add("HA:" + sharedPrefs.getString(getActivity().getResources().getString(R.string.rotateRight), ""));
                    } else {
                        rightMessageBuffer.add("HA:" + Protocol.TURN_RIGHT.toString());
                    }
                }
            }
        });

        //Move Forward control button
        ctrl_top.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(forwardDelayHandler == null) {
                    if (((MainActivity) getActivity()).getConnectedDeviceName() != null && ((MainActivity) getActivity()).getConnectedDeviceName().equalsIgnoreCase(Protocol.AMD_DEVICE_NAME) &&
                            ((MainActivity) getActivity()).getConnectedDeviceMac() != null && ((MainActivity) getActivity()).getConnectedDeviceMac().equalsIgnoreCase(Protocol.AMD_DEVICE_MAC)) {
                        BluetoothService.getInstance().sendText(Protocol.AMD_MOVE_FORWARD.toString(), getActivity());
                        arenaView.getArena().getRobot().moveForward(true);
                        setStatus(Protocol.STATUS_FORWARD);
                        ((MainActivity) getActivity()).activateIdleCountDownTimer();
                    } else if (arenaView.getArena().getRobot().moveForward(false)) {
                        if (preferencesExist) {
                            BluetoothService.getInstance().sendText("HA:" + sharedPrefs.getString(getActivity().getResources().getString(R.string.forward), ""), getActivity());
                        } else {
                            BluetoothService.getInstance().sendText("HA:" + Protocol.MOVE_FORWARD.toString(), getActivity());
                        }
                        setStatus(Protocol.STATUS_FORWARD);
                        ((MainActivity) getActivity()).activateIdleCountDownTimer();
                    }
                    arenaView.invalidate();
                    forwardDelayHandler = new Handler();
                    Runnable r = new Runnable() {
                        public void run() {
                            forwardDelayHandler = null;
                        }
                    };
                    forwardDelayHandler.postDelayed(r, 300);
                }
            }
        });

        //Reverse control button
        ctrl_reverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(reverseDelayHandler == null) {
                    if (((MainActivity) getActivity()).getConnectedDeviceName() != null && ((MainActivity) getActivity()).getConnectedDeviceName().equalsIgnoreCase(Protocol.AMD_DEVICE_NAME) &&
                            ((MainActivity) getActivity()).getConnectedDeviceMac() != null && ((MainActivity) getActivity()).getConnectedDeviceMac().equalsIgnoreCase(Protocol.AMD_DEVICE_MAC)) {
                        BluetoothService.getInstance().sendText(Protocol.AMD_REVERSE.toString(), getActivity());
                        arenaView.getArena().getRobot().reverse(true);
                        setStatus(Protocol.STATUS_REVERSE);
                        ((MainActivity) getActivity()).activateIdleCountDownTimer();
                    } else if (arenaView.getArena().getRobot().reverse(false)) {
                        if (preferencesExist) {
                            BluetoothService.getInstance().sendText("HA:" + sharedPrefs.getString(getActivity().getResources().getString(R.string.reverse), ""), getActivity());
                        } else {
                            BluetoothService.getInstance().sendText("HA:" + Protocol.REVERSE.toString(), getActivity());
                        }
                        setStatus(Protocol.STATUS_REVERSE);
                        ((MainActivity) getActivity()).activateIdleCountDownTimer();
                    }
                    arenaView.invalidate();
                    reverseDelayHandler = new Handler();
                    Runnable r = new Runnable() {
                        public void run() {
                            reverseDelayHandler = null;
                        }
                    };
                    reverseDelayHandler.postDelayed(r, 300);
                }
            }
        });

        autoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(autoBtn.getText().equals("Auto")) {
                    autoBtn.setText("Manual");
                    updateBtn.setVisibility(View.VISIBLE);
                    autoMode = false;
                } else {
                    autoBtn.setText("Auto");
                    updateBtn.setVisibility(View.GONE);
                    autoMode = true;
                    arenaView.invalidate();
                }
            }
        });

        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                arenaView.invalidate();
            }
        });

        if(sharedPrefs.contains(getActivity().getResources().getString(R.string.forward))) {
            preferencesExist = true;
        } else {
            preferencesExist = false;
        }

        return view;
    }

    public void hideController(boolean en) {
        if(ctrl_reverse != null && ctrl_top != null && ctrl_left != null && ctrl_right != null) {
            if(en == false)
            {
                ctrl_left.setVisibility(ImageView.INVISIBLE);
                ctrl_reverse.setVisibility(ImageView.INVISIBLE);
                ctrl_right.setVisibility(ImageView.INVISIBLE);
                ctrl_top.setVisibility(ImageView.INVISIBLE);
                ctrl_center.setVisibility(ImageView.INVISIBLE);
            }
            else {
                ctrl_left.setVisibility(ImageView.VISIBLE);
                ctrl_reverse.setVisibility(ImageView.VISIBLE);
                ctrl_right.setVisibility(ImageView.VISIBLE);
                ctrl_top.setVisibility(ImageView.VISIBLE);
                ctrl_center.setVisibility(ImageView.VISIBLE);
            }
        }
    }

    public void setStatus(String status) { tvStatus.setText("Status: " + status); }

    public void setMDF1(String mdf1) { tvMdfstring1.setText("MDF1: " + mdf1); }

    public void setMDF2(String mdf2) { tvMdfstring2.setText("MDF2: " + mdf2); }

    public boolean getAutoStatus() { return autoMode; }
}
