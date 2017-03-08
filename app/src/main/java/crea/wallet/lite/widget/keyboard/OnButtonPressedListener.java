package crea.wallet.lite.widget.keyboard;

/**
 * Created by ander on 15/06/15.
 */
public interface OnButtonPressedListener {

    int DELETE_KEY = -1;
    int NEXT_KEY = -2;
    int OK_KEY = -3;
    int KEY_UNKNOWN = -4;

    int KEY_0 = 0;
    int KEY_1 = 1;
    int KEY_2 = 2;
    int KEY_3 = 3;
    int KEY_4 = 4;
    int KEY_5 = 5;
    int KEY_6 = 6;
    int KEY_7 = 7;
    int KEY_8 = 8;
    int KEY_9 = 9;

    int KEY_A = 10;
    int KEY_B = 11;
    int KEY_C = 12;
    int KEY_D = 13;
    int KEY_E = 14;
    int KEY_F = 15;
    int KEY_G = 16;
    int KEY_H = 17;
    int KEY_I = 18;
    int KEY_J = 19;
    int KEY_K = 20;
    int KEY_L = 21;
    int KEY_M = 22;
    int KEY_N = 23;
    int KEY_O = 24;
    int KEY_P = 25;
    int KEY_Q = 26;
    int KEY_R = 27;
    int KEY_S = 28;
    int KEY_T = 29;
    int KEY_U = 30;
    int KEY_V = 31;
    int KEY_W = 32;
    int KEY_X = 33;
    int KEY_Y = 34;
    int KEY_Z = 35;

    void onButtonPressed(int key);
}
