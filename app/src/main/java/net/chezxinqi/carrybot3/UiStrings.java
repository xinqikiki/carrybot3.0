package net.chezxinqi.carrybot3;

import android.content.Context;

import java.util.Locale;

public final class UiStrings {

    public static final String LANG_FR = "fr";
    public static final String LANG_EN = "en";
    public static final String LANG_ZH = "zh";

    public static final String KEY_SELECT_TITLE = "select_title";
    public static final String KEY_CONNECT_DESC = "connect_desc";
    public static final String KEY_ADD = "add";
    public static final String KEY_BACK = "back";
    public static final String KEY_SUBTITLE = "subtitle";
    public static final String KEY_DISCONNECT = "disconnect";
    public static final String KEY_RECONNECT = "reconnect";
    public static final String KEY_STOP = "stop";
    public static final String KEY_STATUS_LABEL = "status_label";
    public static final String KEY_STATUS_RUNNING = "status_running";
    public static final String KEY_STATUS_CLOSED = "status_closed";
    public static final String KEY_STATUS_PAUSED = "status_paused";
    public static final String KEY_STATUS_DISCONNECTED = "status_disconnected";
    public static final String KEY_CMD_FORWARD = "cmd_forward";
    public static final String KEY_CMD_BACKWARD = "cmd_backward";
    public static final String KEY_CMD_LEFT = "cmd_left";
    public static final String KEY_CMD_RIGHT = "cmd_right";
    public static final String KEY_CMD_LIFT_UP = "cmd_lift_up";
    public static final String KEY_CMD_LIFT_DOWN = "cmd_lift_down";
    public static final String KEY_TOAST_TURN_ON = "toast_turn_on";
    public static final String KEY_TOAST_SEND_ERROR = "toast_send_error";
    public static final String KEY_TOAST_NOT_CONNECTED = "toast_not_connected";
    public static final String KEY_TOAST_ALREADY_ADDED = "toast_already_added";
    public static final String KEY_TOAST_ENTER_IP = "toast_enter_ip";
    public static final String KEY_TOAST_INVALID_IP = "toast_invalid_ip";
    public static final String KEY_TTS_ON = "tts_on";
    public static final String KEY_TTS_OFF = "tts_off";
    public static final String KEY_LANG_BUTTON = "lang_button";
    public static final String KEY_TTS_BUTTON = "tts_button";
    public static final String KEY_CONTRAST_BUTTON = "contrast_button";
    public static final String KEY_VIDEO_BUTTON = "video_button";
    public static final String KEY_CONTRAST_ON = "contrast_on";
    public static final String KEY_CONTRAST_OFF = "contrast_off";
    public static final String KEY_DELETE = "delete";
    public static final String KEY_DELETE_CONFIRM = "delete_confirm";
    public static final String KEY_YES = "yes";
    public static final String KEY_NO = "no";

    private UiStrings() {
    }

    public static String get(Context context, String key) {
        String lang = UiPrefs.getLang(context);
        switch (lang) {
            case LANG_EN:
                return getEn(key);
            case LANG_ZH:
                return getZh(key);
            default:
                return getFr(key);
        }
    }

    public static String getLangLabel(Context context) {
        String lang = UiPrefs.getLang(context);
        switch (lang) {
            case LANG_EN:
                return "English";
            case LANG_ZH:
                return "中文";
            default:
                return "Français";
        }
    }

    public static Locale getLocale(Context context) {
        String lang = UiPrefs.getLang(context);
        switch (lang) {
            case LANG_EN:
                return Locale.ENGLISH;
            case LANG_ZH:
                return Locale.SIMPLIFIED_CHINESE;
            default:
                return Locale.FRENCH;
        }
    }

    private static String getFr(String key) {
        switch (key) {
            case KEY_SELECT_TITLE:
                return "Choisissez votre appareil";
            case KEY_CONNECT_DESC:
                return "Entrez l'adresse IP du robot";
            case KEY_ADD:
                return "AJOUTER";
            case KEY_BACK:
                return "Retour";
            case KEY_SUBTITLE:
                return "PANNEAU DE COMMANDE";
            case KEY_DISCONNECT:
                return "DÉCONNECTER";
            case KEY_RECONNECT:
                return "RECONNECTER";
            case KEY_STOP:
                return "ARRÊTER";
            case KEY_STATUS_LABEL:
                return "État du Robot";
            case KEY_STATUS_RUNNING:
                return "En cours";
            case KEY_STATUS_CLOSED:
                return "Fermé";
            case KEY_STATUS_PAUSED:
                return "Pause";
            case KEY_STATUS_DISCONNECTED:
                return "Déconnecté";
            case KEY_CMD_FORWARD:
                return "Avant";
            case KEY_CMD_BACKWARD:
                return "Arrière";
            case KEY_CMD_LEFT:
                return "Gauche";
            case KEY_CMD_RIGHT:
                return "Droite";
            case KEY_CMD_LIFT_UP:
                return "Monter";
            case KEY_CMD_LIFT_DOWN:
                return "Descendre";
            case KEY_TOAST_TURN_ON:
                return "Veuillez allumer le robot";
            case KEY_TOAST_SEND_ERROR:
                return "Erreur d'envoi";
            case KEY_TOAST_NOT_CONNECTED:
                return "Non connecté";
            case KEY_TOAST_ALREADY_ADDED:
                return "Déjà ajouté";
            case KEY_TOAST_ENTER_IP:
                return "Entrez l'adresse IP";
            case KEY_TOAST_INVALID_IP:
                return "Adresse IP invalide";
            case KEY_TTS_ON:
                return "TTS activé";
            case KEY_TTS_OFF:
                return "TTS désactivé";
            case KEY_LANG_BUTTON:
                return "Langage";
            case KEY_TTS_BUTTON:
                return "TTS";
            case KEY_CONTRAST_BUTTON:
                return "Contraste";
            case KEY_VIDEO_BUTTON:
                return "Vidéo";
            case KEY_CONTRAST_ON:
                return "Contraste activé";
            case KEY_CONTRAST_OFF:
                return "Contraste désactivé";
            case KEY_DELETE:
                return "Supprimer";
            case KEY_DELETE_CONFIRM:
                return "Supprimer cet appareil ?";
            case KEY_YES:
                return "Oui";
            case KEY_NO:
                return "Non";
            default:
                return "";
        }
    }

    private static String getEn(String key) {
        switch (key) {
            case KEY_SELECT_TITLE:
                return "Choose your device";
            case KEY_CONNECT_DESC:
                return "Enter the robot IP address";
            case KEY_ADD:
                return "ADD";
            case KEY_BACK:
                return "Back";
            case KEY_SUBTITLE:
                return "CONTROL PANEL";
            case KEY_DISCONNECT:
                return "DISCONNECT";
            case KEY_RECONNECT:
                return "RECONNECT";
            case KEY_STOP:
                return "STOP";
            case KEY_STATUS_LABEL:
                return "Robot Status";
            case KEY_STATUS_RUNNING:
                return "Running";
            case KEY_STATUS_CLOSED:
                return "Closed";
            case KEY_STATUS_PAUSED:
                return "Paused";
            case KEY_STATUS_DISCONNECTED:
                return "Disconnected";
            case KEY_CMD_FORWARD:
                return "Forward";
            case KEY_CMD_BACKWARD:
                return "Backward";
            case KEY_CMD_LEFT:
                return "Left";
            case KEY_CMD_RIGHT:
                return "Right";
            case KEY_CMD_LIFT_UP:
                return "Climb";
            case KEY_CMD_LIFT_DOWN:
                return "Descend";
            case KEY_TOAST_TURN_ON:
                return "Please power on the robot";
            case KEY_TOAST_SEND_ERROR:
                return "Send error";
            case KEY_TOAST_NOT_CONNECTED:
                return "Not connected";
            case KEY_TOAST_ALREADY_ADDED:
                return "Already added";
            case KEY_TOAST_ENTER_IP:
                return "Enter IP address";
            case KEY_TOAST_INVALID_IP:
                return "Invalid IP address";
            case KEY_TTS_ON:
                return "TTS on";
            case KEY_TTS_OFF:
                return "TTS off";
            case KEY_LANG_BUTTON:
                return "Language";
            case KEY_TTS_BUTTON:
                return "TTS";
            case KEY_CONTRAST_BUTTON:
                return "Contrast";
            case KEY_VIDEO_BUTTON:
                return "Video";
            case KEY_CONTRAST_ON:
                return "Contrast on";
            case KEY_CONTRAST_OFF:
                return "Contrast off";
            case KEY_DELETE:
                return "Delete";
            case KEY_DELETE_CONFIRM:
                return "Delete this device?";
            case KEY_YES:
                return "Yes";
            case KEY_NO:
                return "No";
            default:
                return "";
        }
    }

    private static String getZh(String key) {
        switch (key) {
            case KEY_SELECT_TITLE:
                return "请选择设备";
            case KEY_CONNECT_DESC:
                return "请输入小车的IP地址";
            case KEY_ADD:
                return "添加";
            case KEY_BACK:
                return "返回";
            case KEY_SUBTITLE:
                return "控制面板";
            case KEY_DISCONNECT:
                return "断开";
            case KEY_RECONNECT:
                return "重新连接";
            case KEY_STOP:
                return "停止";
            case KEY_STATUS_LABEL:
                return "机器人状态";
            case KEY_STATUS_RUNNING:
                return "运行中";
            case KEY_STATUS_CLOSED:
                return "关闭";
            case KEY_STATUS_PAUSED:
                return "暂停";
            case KEY_STATUS_DISCONNECTED:
                return "已断开";
            case KEY_CMD_FORWARD:
                return "前进";
            case KEY_CMD_BACKWARD:
                return "后退";
            case KEY_CMD_LEFT:
                return "左转";
            case KEY_CMD_RIGHT:
                return "右转";
            case KEY_CMD_LIFT_UP:
                return "上升";
            case KEY_CMD_LIFT_DOWN:
                return "下降";
            case KEY_TOAST_TURN_ON:
                return "请先开机";
            case KEY_TOAST_SEND_ERROR:
                return "发送失败";
            case KEY_TOAST_NOT_CONNECTED:
                return "未连接";
            case KEY_TOAST_ALREADY_ADDED:
                return "已添加";
            case KEY_TOAST_ENTER_IP:
                return "请输入IP地址";
            case KEY_TOAST_INVALID_IP:
                return "IP地址无效";
            case KEY_TTS_ON:
                return "语音已开启";
            case KEY_TTS_OFF:
                return "语音已关闭";
            case KEY_LANG_BUTTON:
                return "语言";
            case KEY_TTS_BUTTON:
                return "语音";
            case KEY_CONTRAST_BUTTON:
                return "对比度";
            case KEY_VIDEO_BUTTON:
                return "视频";
            case KEY_CONTRAST_ON:
                return "对比度已开启";
            case KEY_CONTRAST_OFF:
                return "对比度已关闭";
            case KEY_DELETE:
                return "删除";
            case KEY_DELETE_CONFIRM:
                return "确定删除这个设备？";
            case KEY_YES:
                return "确定";
            case KEY_NO:
                return "取消";
            default:
                return "";
        }
    }
}
