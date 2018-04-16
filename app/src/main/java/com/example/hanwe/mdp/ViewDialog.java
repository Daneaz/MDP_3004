package com.example.hanwe.mdp;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;


public class ViewDialog {

    private MainActivity mainActivity;
    private Context context;

    public ViewDialog(Context context) {
        this.context = context;
        this.mainActivity = (MainActivity) context;
    }

    public final void showDialog(String title, String msg)
    {
        final Dialog dialog = new Dialog(context);

        dialog.setTitle(title);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_main);

        ImageView imgRobot = (ImageView) dialog.findViewById(R.id.imgRobot);
        imgRobot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.setRobotPosition(true);
                mainActivity.inputWayPointPosition(1);
                dialog.dismiss();
            }
        });

        ImageView imgWaypoint = (ImageView) dialog.findViewById(R.id.imgWaypoint);
        imgWaypoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.setRobotPosition(false);
                mainActivity.inputWayPointPosition(2);
                dialog.dismiss();
            }
        });

        // Submit Button
        Button btnSubmit = (Button) dialog.findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int r_x, r_y, w_x, w_y;

                CheckBox chkWayPointEnable = (CheckBox) dialog.findViewById(R.id.chkIniWayPointPos);

                // Robot initial value for X and Y
                Spinner spInitialX = (Spinner) dialog.findViewById(R.id.spInitialX);
                Spinner spInitialY = (Spinner) dialog.findViewById(R.id.spInitialY);

                // Waypoint initial value for X and Y
                Spinner spwpInitialX = (Spinner) dialog.findViewById(R.id.spwpInitialX);
                Spinner spwpInitialY = (Spinner) dialog.findViewById(R.id.spwpInitialY);

                // Get robot x and y pos
                r_x = mainActivity.mod(19 - Integer.parseInt(spInitialX.getSelectedItem().toString()), 20);
                r_y = mainActivity.mod(Integer.parseInt(spInitialY.getSelectedItem().toString()) - 15, 15);

                // Get waypoint x and y pos
                w_x = mainActivity.mod(19 - Integer.parseInt(spwpInitialX.getSelectedItem().toString()), 20);
                w_y = mainActivity.mod(Integer.parseInt(spwpInitialY.getSelectedItem().toString()) - 15, 15);Integer.parseInt(spwpInitialY.getSelectedItem().toString());

                // Checked if waypoint is set
                if (chkWayPointEnable.isChecked()) {
                    mainActivity.getArenaview().getArena().getWayPoint().setPosition(w_x,w_y);
                }

                mainActivity.getArenaview().getArena().getRobot().setPosition(r_x,r_y);
                mainActivity.getArenaview().invalidate();

                //Close dialog
                dialog.dismiss();
            }
        });

        // Close Button
        Button btnClose = (Button) dialog.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
