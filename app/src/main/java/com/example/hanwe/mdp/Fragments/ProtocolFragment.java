package com.example.hanwe.mdp.Fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.hanwe.mdp.Bluetooth.BluetoothService;
import com.example.hanwe.mdp.Configuration.Operation;
import com.example.hanwe.mdp.Configuration.Protocol;
import com.example.hanwe.mdp.R;

import static android.content.Context.MODE_PRIVATE;

public class ProtocolFragment extends Fragment {
    private EditText forwardET, rotateLeftET, rotateRightET, reverseET, beginExploreET,
            stopExploreET, beginFastestET, stopFastestET, sendArenaET;
    private Button saveProtocolBtn;
    private SharedPreferences sharedPrefs;
    private boolean preferencesExist = false;

    public ProtocolFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_protocol, container, false);
        forwardET = view.findViewById(R.id.forwardET);
        rotateLeftET = view.findViewById(R.id.rotateLeftET);
        rotateRightET = view.findViewById(R.id.rotateRightET);
        reverseET = view.findViewById(R.id.reverseET);
        beginExploreET = view.findViewById(R.id.beginExploreET);
        stopExploreET = view.findViewById(R.id.stopExploreET);
        beginFastestET = view.findViewById(R.id.beginFastestET);
        stopFastestET = view.findViewById(R.id.stopFastestET);
        sendArenaET = view.findViewById(R.id.sendArenaET);
        saveProtocolBtn = view.findViewById(R.id.saveProtocolBtn);
        sharedPrefs = getActivity().getSharedPreferences(Protocol.PROTOCOLPREF, MODE_PRIVATE);

        saveProtocolBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!hasBlank()) {
                    if(preferencesExist) {
                        if(checkPreferencesForChanges()) {
                            // Save persistence Protocols
                            savePreferences();
                            Toast.makeText(getActivity(), "Protocols Saved Successfully!", Toast.LENGTH_SHORT).show();
                            getActivity().onBackPressed();
                        } else {
                            Toast.makeText(getActivity(), "You have not changed anything. Please check it out.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if(checkProtocolsForChanges()) {
                            // Save persistence Protocols
                            savePreferences();
                            preferencesExist = true;
                            Toast.makeText(getActivity(), "Protocols Saved Successfully!", Toast.LENGTH_SHORT).show();
                            getActivity().onBackPressed();
                        } else {
                            Toast.makeText(getActivity(), "You have not changed anything. Please check it out.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        if(sharedPrefs.contains(getActivity().getResources().getString(R.string.forward))) {
            preferencesExist = true;
            forwardET.setText(sharedPrefs.getString(getActivity().getResources().getString(R.string.forward), ""));
            rotateLeftET.setText(sharedPrefs.getString(getActivity().getResources().getString(R.string.rotateLeft), ""));
            rotateRightET.setText(sharedPrefs.getString(getActivity().getResources().getString(R.string.rotateRight), ""));
            reverseET.setText(sharedPrefs.getString(getActivity().getResources().getString(R.string.reverse), ""));
            beginExploreET.setText(sharedPrefs.getString(getActivity().getResources().getString(R.string.beginExplore), ""));
            stopExploreET.setText(sharedPrefs.getString(getActivity().getResources().getString(R.string.stopExplore), ""));
            beginFastestET.setText(sharedPrefs.getString(getActivity().getResources().getString(R.string.beginFastest), ""));
            stopFastestET.setText(sharedPrefs.getString(getActivity().getResources().getString(R.string.stopFastest), ""));
            sendArenaET.setText(sharedPrefs.getString(getActivity().getResources().getString(R.string.sendArena), ""));
        } else {
            // get the default Protocols
            forwardET.setText(Protocol.MOVE_FORWARD);
            rotateLeftET.setText(Protocol.TURN_LEFT);
            rotateRightET.setText(Protocol.TURN_RIGHT);
            reverseET.setText(Protocol.REVERSE);
            beginExploreET.setText(Protocol.START_EXPLORATION);
            stopExploreET.setText(Protocol.STOP_EXPLORATION);
            beginFastestET.setText(Protocol.START_FASTEST);
            stopFastestET.setText(Protocol.STOP_FASTEST);
            sendArenaET.setText(Protocol.SEND_ARENA);
        }

        return view;
    }

    // Check if any of the Protocols are blank. Return true if any of the Protocols is blank.
    private boolean hasBlank() {
        boolean hasBlank = false;
        if(forwardET.getText().toString().isEmpty())
            hasBlank = true;
        else if(rotateLeftET.getText().toString().isEmpty())
            hasBlank = true;
        else if(rotateRightET.getText().toString().isEmpty())
            hasBlank = true;
        else if(reverseET.getText().toString().isEmpty())
            hasBlank = true;
        else if(beginExploreET.getText().toString().isEmpty())
            hasBlank = true;
        else if(stopExploreET.getText().toString().isEmpty())
            hasBlank = true;
        else if(beginFastestET.getText().toString().isEmpty())
            hasBlank = true;
        else if(stopFastestET.getText().toString().isEmpty())
            hasBlank = true;
        else if(sendArenaET.getText().toString().isEmpty())
            hasBlank = true;
        if(hasBlank)
            Operation.showToast(getActivity(), "Protocols cannot be empty. Please check your inputs.");

        return hasBlank;
    }

    // Check if any Protocols has been changed, return true if it has been changed
    private boolean checkProtocolsForChanges() {
        boolean hasChanges = false;
        if(!Protocol.MOVE_FORWARD.equals(forwardET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("Forward Protocol: Changed from " + Protocol.MOVE_FORWARD.toString() + " to " + forwardET.getText().toString() + "\n", getActivity());
        }
        if(!Protocol.TURN_LEFT.equals(rotateLeftET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("Rotate Left Protocol: Changed from " + Protocol.TURN_LEFT.toString() + " to " + rotateLeftET.getText().toString() + "\n", getActivity());
        }
        if(!Protocol.TURN_RIGHT.equals(rotateRightET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("Rotate Right Protocol: Changed from " + Protocol.TURN_RIGHT.toString() + " to " + rotateRightET.getText().toString() + "\n", getActivity());
        }
        if(!Protocol.REVERSE.equals(reverseET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("Reverse Protocol: Changed from " + Protocol.REVERSE.toString() + " to " + reverseET.getText().toString() + "\n", getActivity());
        }
        if(!Protocol.START_EXPLORATION.equals(beginExploreET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("Begin Exploration Protocol: Changed from " + Protocol.START_EXPLORATION.toString() + " to " + beginExploreET.getText().toString() + "\n", getActivity());
        }
        if(!Protocol.STOP_EXPLORATION.equals(stopExploreET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("Stop Exploration Protocol: Changed from " + Protocol.STOP_EXPLORATION.toString() + " to " + stopExploreET.getText().toString() + "\n", getActivity());
        }
        if(!Protocol.START_FASTEST.equals(beginFastestET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("Begin Fastest Path Protocol: Changed from " + Protocol.START_FASTEST.toString() + " to " + beginFastestET.getText().toString() + "\n", getActivity());
        }
        if(!Protocol.STOP_FASTEST.equals(stopFastestET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("Stop Fastest Path Protocol: Changed from " + Protocol.STOP_FASTEST.toString() + " to " + stopFastestET.getText().toString() + "\n", getActivity());
        }
        if(!Protocol.SEND_ARENA.equals(sendArenaET.getText().toString())){
            hasChanges = true;
            BluetoothService.getInstance().sendText("Send Arena Information Protocol: Changed from " + Protocol.SEND_ARENA.toString() + " to " + sendArenaET.getText().toString() + "\n", getActivity());
        }
        return hasChanges;
    }

    // Check if any Preferences has been changed, return true if it has been changed
    private boolean checkPreferencesForChanges() {
        boolean hasChanges = false;
        if(!sharedPrefs.getString(getActivity().getResources().getString(R.string.forward), "").equals(forwardET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("Forward Protocol: Changed from " + sharedPrefs.getString(getActivity().getResources().getString(R.string.forward), "").toString() + " to " + forwardET.getText().toString() + "\n", getActivity());
        }
        if(!sharedPrefs.getString(getActivity().getResources().getString(R.string.rotateLeft), "").equals(rotateLeftET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("Rotate Left Protocol: Changed from " + sharedPrefs.getString(getActivity().getResources().getString(R.string.rotateLeft), "").toString() + " to " + rotateLeftET.getText().toString() + "\n", getActivity());
        }
        if(!sharedPrefs.getString(getActivity().getResources().getString(R.string.rotateRight), "").equals(rotateRightET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("Rotate Right Protocol: Changed from " + sharedPrefs.getString(getActivity().getResources().getString(R.string.rotateRight), "").toString() + " to " + rotateRightET.getText().toString() + "\n", getActivity());
        }
        if(!sharedPrefs.getString(getActivity().getResources().getString(R.string.reverse), "").equals(reverseET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("Reverse Protocol: Changed from " + sharedPrefs.getString(getActivity().getResources().getString(R.string.reverse), "").toString() + " to " + reverseET.getText().toString() + "\n", getActivity());
        }
        if(!sharedPrefs.getString(getActivity().getResources().getString(R.string.beginExplore), "").equals(beginExploreET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("Begin Exploration Protocol: Changed from " + sharedPrefs.getString(getActivity().getResources().getString(R.string.beginExplore), "").toString() + " to " + beginExploreET.getText().toString() + "\n", getActivity());
        }
        if(!sharedPrefs.getString(getActivity().getResources().getString(R.string.stopExplore), "").equals(stopExploreET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("Stop Exploration Protocol: Changed from " + sharedPrefs.getString(getActivity().getResources().getString(R.string.stopExplore), "").toString() + " to " + stopExploreET.getText().toString() + "\n", getActivity());
        }
        if(!sharedPrefs.getString(getActivity().getResources().getString(R.string.beginFastest), "").equals(beginFastestET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("Begin Fastest Path Protocol: Changed from " + sharedPrefs.getString(getActivity().getResources().getString(R.string.beginFastest), "").toString() + " to " + beginFastestET.getText().toString() + "\n", getActivity());
        }
        if(!sharedPrefs.getString(getActivity().getResources().getString(R.string.stopFastest), "").equals(stopFastestET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("Stop Fastest Path Protocol: Changed from " + sharedPrefs.getString(getActivity().getResources().getString(R.string.stopFastest), "").toString() + " to " + stopFastestET.getText().toString() + "\n", getActivity());
        }
        if(!sharedPrefs.getString(getActivity().getResources().getString(R.string.sendArena), "").equals(sendArenaET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("Send Arena Information Protocol: Changed from " + sharedPrefs.getString(getActivity().getResources().getString(R.string.sendArena), "").toString() + " to " + sendArenaET.getText().toString() + "\n", getActivity());
        }

        return hasChanges;
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(getActivity().getResources().getString(R.string.forward), forwardET.getText().toString());
        editor.putString(getActivity().getResources().getString(R.string.rotateLeft), rotateLeftET.getText().toString());
        editor.putString(getActivity().getResources().getString(R.string.rotateRight), rotateRightET.getText().toString());
        editor.putString(getActivity().getResources().getString(R.string.reverse), reverseET.getText().toString());
        editor.putString(getActivity().getResources().getString(R.string.beginExplore), beginExploreET.getText().toString());
        editor.putString(getActivity().getResources().getString(R.string.stopExplore), stopExploreET.getText().toString());
        editor.putString(getActivity().getResources().getString(R.string.beginFastest), beginFastestET.getText().toString());
        editor.putString(getActivity().getResources().getString(R.string.stopFastest), stopFastestET.getText().toString());
        editor.putString(getActivity().getResources().getString(R.string.sendArena), sendArenaET.getText().toString());
        editor.commit();
    }
}
