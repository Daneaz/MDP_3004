package com.example.hanwe.mdp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ScrollView;

import com.example.hanwe.mdp.Configuration.Protocol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class LogActivity extends AppCompatActivity {
    EditText logET;
    ScrollView scroller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        setTitle(R.string.messageLogTitle);

        logET = findViewById(R.id.logET);
        scroller = findViewById(R.id.scroller);
        logET.setText(readLog());
        scroller.post(new Runnable() {
            public void run() {
                scroller.smoothScrollTo(0, logET.getBottom());
            }
        });
    }

    private String readLog() {
        StringBuffer stringBuffer = new StringBuffer();
        File fileDir = new File(getFilesDir(),Protocol.LOG_FILE_DIR);
        if(!fileDir.exists()){
            stringBuffer.append("There is no message at the moment.");
        } else {
            try {
                String line;
                File logFile = new File(fileDir, Protocol.LOG_FILE_NAME);
                BufferedReader bufferedReader = new BufferedReader(new FileReader(logFile));
                while((line = bufferedReader.readLine())!=null){
                    stringBuffer.append(line).append("\n");
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        return stringBuffer.toString();
    }
}
