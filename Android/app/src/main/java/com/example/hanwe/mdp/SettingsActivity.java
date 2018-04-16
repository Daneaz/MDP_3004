package com.example.hanwe.mdp;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.hanwe.mdp.Fragments.F1F2Fragment;
import com.example.hanwe.mdp.Fragments.ProtocolFragment;

public class SettingsActivity extends AppCompatActivity {
    private ProtocolFragment protocolFragment = null;
    private F1F2Fragment f1F2Fragment = null;
    private ListView settingsLV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle(R.string.action_settings);

        settingsLV = findViewById(R.id.settingsLV);
        String[] settingsList= getResources().getStringArray(R.array.settingsList);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1, settingsList);

        settingsLV.setAdapter(adapter);

        settingsLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                switch(position) {
                    // Communication Protocol
                    case 0:
                        settingsLV.setVisibility(View.GONE);
                        if(protocolFragment == null) {
                            protocolFragment = new ProtocolFragment();
                        }

                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.settings_container, protocolFragment);
                        transaction.addToBackStack(null);
                        transaction.commit();
                        break;
                    // Configure F1 / F2
                    case 1:
                        settingsLV.setVisibility(View.GONE);
                        if(f1F2Fragment == null) {
                            f1F2Fragment = new F1F2Fragment();
                        }

                        transaction = getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.settings_container, f1F2Fragment);
                        transaction.addToBackStack(null);
                        transaction.commit();
                        break;

                        default:
                            Toast.makeText(getBaseContext(), "Position pressed: " + String.valueOf(position), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if(protocolFragment != null) {
            FragmentManager fm = this.getSupportFragmentManager();
            if(fm.getBackStackEntryCount()>0) {
                fm.popBackStack();
            }
            protocolFragment = null;
            settingsLV.setVisibility(View.VISIBLE);
        } else if(f1F2Fragment != null) {
            FragmentManager fm = this.getSupportFragmentManager();
            if(fm.getBackStackEntryCount()>0) {
                fm.popBackStack();
            }
            f1F2Fragment = null;
            settingsLV.setVisibility(View.VISIBLE);
        } else {
            super.onBackPressed();
        }
    }
}
