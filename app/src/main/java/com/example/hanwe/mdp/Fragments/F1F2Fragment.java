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

public class F1F2Fragment extends Fragment {
    private EditText f1ET, f2ET;
    private Button f1SaveBtn, f2SaveBtn;
    private SharedPreferences sharedPrefs;
    private boolean preferencesExist = false;

    public F1F2Fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_f1_f2, container, false);
        f1ET = view.findViewById(R.id.f1ET);
        f2ET = view.findViewById(R.id.f2ET);
        f1SaveBtn = view.findViewById(R.id.saveF1Btn);
        f2SaveBtn = view.findViewById(R.id.saveF2Btn);
        sharedPrefs = getActivity().getSharedPreferences(Protocol.PROTOCOLPREF, MODE_PRIVATE);

        if(sharedPrefs.contains(getActivity().getResources().getString(R.string.f1))) {
            preferencesExist = true;
            f1ET.setText(sharedPrefs.getString(getActivity().getResources().getString(R.string.f1), ""));
            f2ET.setText(sharedPrefs.getString(getActivity().getResources().getString(R.string.f2), ""));
        } else {
            // get the default Protocols
            f1ET.setText(Protocol.F1);
            f2ET.setText(Protocol.F2);
        }

        f1SaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!hasBlank()) {
                    if(preferencesExist) {
                        if(checkPreferencesForChanges()) {
                            // Save persistence Protocols
                            savePreferences();
                            Toast.makeText(getActivity(), "F1 Saved Successfully!", Toast.LENGTH_SHORT).show();
                            getActivity().onBackPressed();
                        } else {
                            Toast.makeText(getActivity(), "You have not changed anything. Please check it out.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if(checkProtocolsForChanges()) {
                            // Save persistence Protocols
                            savePreferences();
                            preferencesExist = true;
                            Toast.makeText(getActivity(), "F1 Saved Successfully!", Toast.LENGTH_SHORT).show();
                            getActivity().onBackPressed();
                        } else {
                            Toast.makeText(getActivity(), "You have not changed anything. Please check it out.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        f2SaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!hasBlank()) {
                    if(preferencesExist) {
                        if(checkPreferencesForChanges()) {
                            // Save persistence Protocols
                            savePreferences();
                            Toast.makeText(getActivity(), "F2 Saved Successfully!", Toast.LENGTH_SHORT).show();
                            getActivity().onBackPressed();
                        } else {
                            Toast.makeText(getActivity(), "You have not changed anything. Please check it out.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if(checkProtocolsForChanges()) {
                            // Save persistence Protocols
                            savePreferences();
                            preferencesExist = true;
                            Toast.makeText(getActivity(), "F2 Saved Successfully!", Toast.LENGTH_SHORT).show();
                            getActivity().onBackPressed();
                        } else {
                            Toast.makeText(getActivity(), "You have not changed anything. Please check it out.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        return view;
    }

    // Check if any of the Protocols are blank. Return true if any of the Protocols is blank.
    private boolean hasBlank() {
        boolean hasBlank = false;
        if(f1ET.getText().toString().isEmpty())
            hasBlank = true;
        else if(f2ET.getText().toString().isEmpty())
            hasBlank = true;

        if(hasBlank)
            Operation.showToast(getActivity(), "Protocols cannot be empty. Please check your inputs.");

        return hasBlank;
    }

    // Check if anything has been changed, return true if it has been changed
    private boolean checkProtocolsForChanges() {
        boolean hasChanges = false;
        if(!Protocol.F1.equals(f1ET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("F1 Changed from " + Protocol.F1.toString() + " to " + f1ET.getText().toString() + "\n", getActivity());
        }
        if(!Protocol.F2.equals(f2ET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("F2 Changed from " + Protocol.F2.toString() + " to " + f2ET.getText().toString() + "\n", getActivity());
        }
        return hasChanges;
    }

    // Check if any Preferences has been changed, return true if it has been changed
    private boolean checkPreferencesForChanges() {
        boolean hasChanges = false;
        if(!sharedPrefs.getString(getActivity().getResources().getString(R.string.f1), "").equals(f1ET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("F1 Changed from " + sharedPrefs.getString(getActivity().getResources().getString(R.string.f1), "").toString() + " to " + f1ET.getText().toString() + "\n", getActivity());
        }
        if(!sharedPrefs.getString(getActivity().getResources().getString(R.string.f2), "").equals(f2ET.getText().toString())) {
            hasChanges = true;
            BluetoothService.getInstance().sendText("F2 Changed from " + sharedPrefs.getString(getActivity().getResources().getString(R.string.f2), "").toString() + " to " + f2ET.getText().toString() + "\n", getActivity());
        }
        return hasChanges;
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(getActivity().getResources().getString(R.string.f1), f1ET.getText().toString());
        editor.putString(getActivity().getResources().getString(R.string.f2), f2ET.getText().toString());
        editor.commit();
    }

}
