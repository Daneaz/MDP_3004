package com.example.hanwe.mdp.Bluetooth;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.hanwe.mdp.MainActivity;
import com.example.hanwe.mdp.R;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothDialog {
    private Context context;
    private BluetoothAdapter mBluetoothAdapter;
    private ListView pairedBTDevicesLV, availableDevicesLV;
    private Button scanBtn;
    private ArrayList<String> availableList, pairedList;
    private ArrayAdapter<String> availableBtAdapter, pairedBtAdapter;
    private ArrayList<BluetoothDevice> availableBtDevice, pairedBtDevice;
    private int pairingPosition, unpairingPosition;
    private BluetoothService btService;
    private MainActivity mainActivity;

    public BluetoothDialog(Context context, BluetoothService btService) {
        this.context = context;
        this.mainActivity = (MainActivity) context;
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.availableList = new ArrayList<>();
        this.pairedList = new ArrayList<>();
        availableList.add(context.getString(R.string.no_available_device));
        this.availableBtDevice = new ArrayList<>();
        this.pairedBtDevice = new ArrayList<>();
        this.pairingPosition = -1;
        this.unpairingPosition = -1;

        if(btService == null) {
            btService = new BluetoothService();
            btService.initialiseService();
            mainActivity.setBtService(btService);
        }
    }

    public final void showDialog(String title, final BluetoothService btService){
        this.btService = btService;
        final Dialog dialog = new Dialog(this.context);

        dialog.setTitle(title);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.bluetooth_main);

        pairedBTDevicesLV = dialog.findViewById(R.id.pairedBTDevicesLV);
        availableDevicesLV = dialog.findViewById(R.id.availableDevicesLV);
        scanBtn = dialog.findViewById(R.id.scanBtn);
        displayPairedBTDevices();

        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(scanBtn.getText().equals(context.getString(R.string.scan_bt))) {
                    availableList.clear();
                    availableBtDevice.clear();
                    availableBtAdapter.notifyDataSetChanged();
                    scanBtn.setText(context.getString(R.string.stop_scan_bt));
                    mBluetoothAdapter.startDiscovery();
                } else {
                    scanBtn.setText(context.getString(R.string.scan_bt));
                    mBluetoothAdapter.cancelDiscovery();
                    if(availableList.size() == 0) {
                        availableList.add(context.getString(R.string.no_available_device));
                    }
                }
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                scanBtn.setText(context.getString(R.string.scan_bt));
                mBluetoothAdapter.cancelDiscovery();
            }
        });

        pairedBTDevicesLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3) {
                if(mBluetoothAdapter.isDiscovering()) {
                    scanBtn.setText(context.getString(R.string.scan_bt));
                    mBluetoothAdapter.cancelDiscovery();
                }
                if(!pairedList.get(position).contains("(Connected)")) {
                    // Attempt to connect to the device
                    btService.connect(pairedBtDevice.get(position));
                } else {
                    btService.disconnect();
                }

            }
        });

        pairedBTDevicesLV.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long arg3) {
                if(mBluetoothAdapter.isDiscovering()) {
                    scanBtn.setText(context.getString(R.string.scan_bt));
                    mBluetoothAdapter.cancelDiscovery();
                }
                setUnpairingPosition(position);
                BluetoothPairing btPairing = new BluetoothPairing();
                // initiate unpairing process
                btPairing.unpairDevice(pairedBtDevice.get(position));
                return true;
            }
        });

        availableDevicesLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3) {
                setPairingPosition(position);
                BluetoothPairing btPairing = new BluetoothPairing();
                if(mBluetoothAdapter.isDiscovering()) {
                    scanBtn.setText(context.getString(R.string.scan_bt));
                    mBluetoothAdapter.cancelDiscovery();
                }
                // initiate pairing process
                btPairing.pairDevice(availableBtDevice.get(position));
            }
        });

        availableBtAdapter = new ArrayAdapter<String>(
                context,
                android.R.layout.simple_list_item_1,
                availableList );

        availableDevicesLV.setAdapter(availableBtAdapter);

        dialog.show();
    }

    private void displayPairedBTDevices() {
        pairedList.clear();
        pairedBtDevice.clear();

        Set<BluetoothDevice> pairedDevices = this.mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if(btService.getConnectionState() == BluetoothService.STATE_CONNECTED) {
                    if(mainActivity.getConnectedDeviceName().equals(deviceName) &&
                            mainActivity.getConnectedDeviceMac().equals(deviceHardwareAddress)) {
                        pairedList.add(deviceName + "   (Connected)" + "\n" + deviceHardwareAddress);
                    } else {
                        pairedList.add(deviceName + "\n" + deviceHardwareAddress);
                    }
                } else {
                    pairedList.add(deviceName + "\n" + deviceHardwareAddress);
                }
                pairedBtDevice.add(device);
            }
        } else {
            pairedList.add(context.getString(R.string.no_paired_device));
        }

        pairedBtAdapter = new ArrayAdapter<String>(
                context,
                android.R.layout.simple_list_item_1,
                pairedList);

        pairedBTDevicesLV.setAdapter(pairedBtAdapter);
    }

    public void updateAvailableListView(String data, BluetoothDevice btDevice){
        if(!existInPairedList(data)) {
            if(availableList.size() == 1 && availableList.get(0).toString().equals(context.getString(R.string.no_available_device))) {
                availableList.clear();
            }
            if(!checkDuplicateDevice(availableList, data)) {
                availableList.add(data);
                availableBtDevice.add(btDevice);
                availableBtAdapter.notifyDataSetChanged();
            }
        }
    }

    // return true means there is duplicate
    private boolean checkDuplicateDevice(ArrayList<String> list, String data) {
        if (list.size() > 0 && !list.get(0).toString().equals(context.getString(R.string.no_available_device))) {
            // There are paired devices. Get the name and address of each paired device.
            for (String listData : list) {
                if(listData.equals(data))
                    return true;
            }
        }
        return false;
    }

    // return true if it exists in Paired List
    private boolean existInPairedList(String data) {
        if (pairedList.size() > 0) {
            for (String device : pairedList) {
                if(device.equals(data))
                    return true;
            }
        }
        return false;
    }

    public void setPairingPosition(int position) {
        this.pairingPosition = position;
    }

    public void setUnpairingPosition(int position) {
        this.unpairingPosition = position;
    }

    public void updatePairedSuccess() {
        if(this.pairingPosition != -1) {
            if(pairedList.size() == 1 && pairedList.get(0).equals(context.getString(R.string.no_paired_device))) {
                pairedList.clear();
            }
            pairedList.add(availableList.get(this.pairingPosition));
            pairedBtDevice.add(availableBtDevice.get(this.pairingPosition));
            availableBtDevice.remove(this.pairingPosition);
            availableList.remove(this.pairingPosition);
            pairedBtAdapter.notifyDataSetChanged();
            availableBtAdapter.notifyDataSetChanged();
            this.pairingPosition = -1;
            Toast.makeText(context, "Successfully Paired with " + pairedBtDevice.get(pairedBtDevice.size()-1).getName(), Toast.LENGTH_SHORT).show();
        }
    }

    public void updateUnpairedSuccess() {
        if(this.unpairingPosition != -1) {
            String deviceName = pairedBtDevice.get(unpairingPosition).getName();
            pairedList.remove(unpairingPosition);
            pairedBtDevice.remove(unpairingPosition);
            pairedBtAdapter.notifyDataSetChanged();
            this.unpairingPosition = -1;
            if(pairedList.size() == 0) {
                pairedList.add(context.getString(R.string.no_paired_device));
            }
            Toast.makeText(context, "Successfully Unpaired with " + deviceName, Toast.LENGTH_SHORT).show();
        }
    }

    public void updateList(String mConnectedDeviceName, String mConnectedDeviceMac, boolean connected) {
        if(connected) {
            for(int i=0; i<pairedBtDevice.size(); i++) {
                if(pairedBtDevice.get(i).getName().equals(mConnectedDeviceName) &&
                        pairedBtDevice.get(i).getAddress().equals(mConnectedDeviceMac)) {
                    String newInfo = pairedBtDevice.get(i).getName().toString() + "   (Connected)" + "\n" + pairedBtDevice.get(i).getAddress().toString();
                    pairedList.add(i, newInfo);
                    pairedList.remove(i+1);
                    pairedBtAdapter.notifyDataSetChanged();
                    break;
                }
            }
        } else {
            for(int i=0; i<pairedList.size(); i++) {
                if(pairedBtDevice.get(i).getName().equals(mConnectedDeviceName) &&
                        pairedBtDevice.get(i).getAddress().equals(mConnectedDeviceMac)) {
                    String newInfo = pairedList.get(i).toString().replace("(Connected)", "");
                    pairedList.add(i, newInfo);
                    pairedList.remove(i + 1);
                    pairedBtAdapter.notifyDataSetChanged();
                    break;
                }
            }
        }
    }
}