package com.example.hanwe.mdp.Configuration;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class Operation {
    public static String binaryToHex(String binaryString) {
        String hexString = "";

        if (binaryString.length() % 4 != 0) {
            int numOf0sToPad = 4 - ((binaryString.length() % 4));

            for (int i = 0; i < numOf0sToPad; i ++)
                binaryString += "0";
        }

        for (int i = 0; i < binaryString.length(); i += 4) {
            try {
                int hexValue = Integer.parseInt(binaryString.substring(i, i + 4), 2);
                hexString += Integer.toHexString(hexValue).toUpperCase();

            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                Log.e(Config.log_id, e.getMessage());
            }
        }

        return hexString;
    }

    public static String hexToBinary(String Hex) {
        String binary = "";
        Hex = Hex.toUpperCase();
        for(int index=0; index < Hex.length(); index++) {
            switch (Hex.charAt(index)) {
                case '0':
                    binary += "0000";
                    break;
                case '1':
                    binary += "0001";
                    break;
                case '2':
                    binary += "0010";
                    break;
                case '3':
                    binary += "0011";
                    break;
                case '4':
                    binary += "0100";
                    break;
                case '5':
                    binary += "0101";
                    break;
                case '6':
                    binary += "0110";
                    break;
                case '7':
                    binary += "0111";
                    break;
                case '8':
                    binary += "1000";
                    break;
                case '9':
                    binary += "1001";
                    break;
                case 'A':
                    binary += "1010";
                    break;
                case 'B':
                    binary += "1011";
                    break;
                case 'C':
                    binary += "1100";
                    break;
                case 'D':
                    binary += "1101";
                    break;
                case 'E':
                    binary += "1110";
                    break;
                case 'F':
                    binary += "1111";
                    break;
                default:
            }
        }
        return binary;
    }

    public static void showToast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}