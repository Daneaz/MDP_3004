package com.example.hanwe.mdp.Configuration;

public interface Protocol {
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_DEVICE_NAME = 4;
    int MESSAGE_TOAST = 5;

    String UUID = "00001101-0000-1000-8000-00805F9B34FB";

    String LOG_FILE_DIR = "MDP_LOG";
    String LOG_FILE_NAME = "log.txt";

    String TOAST = "toast";
    String DEVICE_NAME = "device_name";
    String DEVICE_MAC = "device_address";

    String PROTOCOLPREF = "protocolPref";
    String F1 = "F1";
    String F2 = "F2";

    String MESSAGE_TYPE   = "MESSAGE_TYPE";
    String MESSAGE_BYTES  = "MESSAGE_BYTES";
    String MESSAGE_BUFFER = "MESSAGE_BUFFER";
    String MESSAGE_ARG1   = "MESSAGE_ARG1";

    String ARDUINO_MESSAGE_HEADER = "A";
    String PC_MESSAGE_HEADER = "P";

    String STATUS_FORWARD = "Moving Forward";
    String STATUS_TURN_LEFT = "Turning Left";
    String STATUS_TURN_RIGHT = "Turning Right";
    String STATUS_REVERSE = "Reversing";
    String STATUS_IDLE = "Idling";

    String MOVE_FORWARD = "pF";
    String TURN_LEFT    = "pL";
    String TURN_RIGHT   = "pD";
    String REVERSE = "pR";

    String START_EXPLORATION = "pe";
    String STOP_EXPLORATION  = "se";
    String START_FASTEST = "pf";
    String STOP_FASTEST  = "sf";
    String SEND_ARENA = "pse";

    //========== Protocol for communication with AMDTool  ==========
    //String AMD_DEVICE_NAME = "DESKTOP-UFDFUCR";
    //String AMD_DEVICE_MAC = "48:45:20:91:cd:95";
    String AMD_DEVICE_NAME = "DESKTOP-PN6SGT9";
    String AMD_DEVICE_MAC = "40:E2:30:68:D8:6A";

    String AMD_TURN_LEFT    = "tl";
    String AMD_TURN_RIGHT   = "tr";
    String AMD_MOVE_FORWARD = "f";
    String AMD_REVERSE = "r";

    String AMD_SEND_ARENA  = "sendArena";
    String AMD_START_EXPLORATION = "beginExplore";
    String AMD_START_FASTEST = "beginFastest";
    //==========   ==========   ==========   ==========   ==========
}
