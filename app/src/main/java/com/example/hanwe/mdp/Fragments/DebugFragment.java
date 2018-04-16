package com.example.hanwe.mdp.Fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.example.hanwe.mdp.Bluetooth.BluetoothService;
import com.example.hanwe.mdp.Configuration.Operation;
import com.example.hanwe.mdp.Configuration.Protocol;
import com.example.hanwe.mdp.MainActivity;
import com.example.hanwe.mdp.R;

import static android.content.Context.MODE_PRIVATE;

public class DebugFragment extends Fragment {
    private Button sendBtn, f1Btn, f2Btn, tiltBtn;
    private EditText receiveET, sendET;

    public DebugFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_debug, container, false);
        sendBtn = view.findViewById(R.id.sendTextBtn);
        f1Btn = view.findViewById(R.id.f1Btn);
        f2Btn = view.findViewById(R.id.f2Btn);
        tiltBtn = view.findViewById(R.id.tiltBtn);
        receiveET = view.findViewById(R.id.receiveET);
        sendET = view.findViewById(R.id.sendET);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothService.getInstance().sendText(sendET.getText().toString(), getActivity());
            }
        });

        f1Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sharedPrefs = getActivity().getSharedPreferences(Protocol.PROTOCOLPREF, MODE_PRIVATE);
                if(sharedPrefs.contains(getActivity().getResources().getString(R.string.f1))) {
                    BluetoothService.getInstance().sendText(sharedPrefs.getString(getActivity().getResources().getString(R.string.f1), ""), getActivity());
                    Operation.showToast(getActivity(), sharedPrefs.getString(getActivity().getResources().getString(R.string.f1), "") + " was sent.");
                } else {
                    BluetoothService.getInstance().sendText(Protocol.F1, getActivity());
                    Operation.showToast(getActivity(), Protocol.F1 + " was sent.");
                }
            }
        });

        f2Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sharedPrefs = getActivity().getSharedPreferences(Protocol.PROTOCOLPREF, MODE_PRIVATE);
                if(sharedPrefs.contains(getActivity().getResources().getString(R.string.f2))) {
                    BluetoothService.getInstance().sendText(sharedPrefs.getString(getActivity().getResources().getString(R.string.f2), ""), getActivity());
                    Operation.showToast(getActivity(), sharedPrefs.getString(getActivity().getResources().getString(R.string.f2), "") + " was sent.");
                } else {
                    BluetoothService.getInstance().sendText(Protocol.F2, getActivity());
                    Operation.showToast(getActivity(), Protocol.F2 + " was sent.");
                }
            }
        });

        tiltBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(tiltBtn.getText().equals("On Tilt")) {
                    tiltBtn.setText("Off Tilt");
                    ((MainActivity) getActivity()).activateTiltSensing();
                } else {
                    tiltBtn.setText("On Tilt");
                    ((MainActivity) getActivity()).deactivateTiltSensing();
                }
            }
        });

        return view;
    }

    public void setReceiveET(String receivedText) {
        receiveET.setText(receiveET.getText() + receivedText);
    }
}
