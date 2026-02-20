package net.chezxinqi.carrybot3;

import android.os.Bundle;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "CarryBot";
    private static final String EXTRA_CONNECTED = "connected";
    private static final String EXTRA_BASE_URL = "base_url";
    private static final String EXTRA_PREVIEW = "preview";
    private static final String EXTRA_DEVICE_NAME = "device_name";
    private static final String DEFAULT_BASE_URL = "http://192.168.4.1";
    private static final int CONTROL_PORT = 8090;

    private TextView txtTitle;
    private TextView txtStatus;
    private TextView txtBackLabel;
    private Button btnStop;
    private ImageButton btnUp;
    private ImageButton btnDown;
    private ImageButton btnLeft;
    private ImageButton btnRight;
    private Button btnCenterStop;
    private ImageButton btnLiftUp;
    private ImageButton btnLiftDown;
    private View viewStatusIndicator;

    private static final int COLOR_RED = Color.parseColor("#C94A4A");
    private static final int COLOR_GREEN = Color.parseColor("#43A047");
    private static final int COLOR_YELLOW = Color.parseColor("#F2B705");
    private static final int COLOR_GRAY = Color.parseColor("#BDBDBD");
    private static final float DISABLED_ALPHA = 0.4f;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isConnected = false;
    private boolean isPreview = false;
    private boolean isDisconnectedState = false;
    private String baseUrl = DEFAULT_BASE_URL;
    private String currentStatusKey = UiStrings.KEY_STATUS_CLOSED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        TtsManager.warmUp(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtStatus = findViewById(R.id.txtStatus);
        txtTitle = findViewById(R.id.txtTitle);
        btnStop = findViewById(R.id.btnStop);
        txtBackLabel = findViewById(R.id.txtBackLabel);
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            TtsManager.speak(this, UiStrings.get(this, UiStrings.KEY_BACK));
            finish();
        });
        btnUp = findViewById(R.id.btnUp);
        btnDown = findViewById(R.id.btnDown);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);
        btnCenterStop = findViewById(R.id.btnCenterStop);
        btnLiftUp = findViewById(R.id.btnLiftUp);
        btnLiftDown = findViewById(R.id.btnLiftDown);
        viewStatusIndicator = findViewById(R.id.viewStatusIndicator);

        isPreview = getIntent().getBooleanExtra(EXTRA_PREVIEW, false);
        String deviceName = getIntent().getStringExtra(EXTRA_DEVICE_NAME);
        if (deviceName != null && !deviceName.trim().isEmpty()) {
            txtTitle.setText(deviceName.trim());
        }
        String incomingUrl = getIntent().getStringExtra(EXTRA_BASE_URL);
        if (incomingUrl != null && !incomingUrl.trim().isEmpty()) {
            baseUrl = incomingUrl.trim();
        }
        applyLanguage();
        applyContrast();

        if (isPreview) {
            setPreviewMode();
        } else {
            checkRobotOnline();
        }

        bindAction(btnUp, "AVANT");
        bindAction(btnDown, "ARRIÈRE");
        bindAction(btnLeft, "GAUCHE");
        bindAction(btnRight, "DROITE");
        bindAction(btnLiftUp, "MONTER");
        bindAction(btnLiftDown, "DESCENDRE");

        btnStop.setOnClickListener(v -> {
            String label = UiStrings.get(this,
                    isDisconnectedState ? UiStrings.KEY_RECONNECT : UiStrings.KEY_DISCONNECT);
            TtsManager.speak(this, label);
            if (isDisconnectedState) {
                startActivity(new android.content.Intent(this, DeviceSelectActivity.class));
                finish();
                return;
            }
            logAction("DÉCONNECTER");
            setDisconnectedState();
            Toast.makeText(this, UiStrings.get(this, UiStrings.KEY_STATUS_DISCONNECTED), Toast.LENGTH_SHORT).show();
        });

        btnCenterStop.setOnClickListener(v -> {
            sendCommand("STOP");
            setStatusKey(UiStrings.KEY_STATUS_PAUSED, COLOR_YELLOW);
        });
    }

    private void bindAction(View view, String action) {
        view.setOnClickListener(v -> sendCommand(action));
    }

    private void logAction(String action) {
        Log.d(TAG, action);
    }

    private void setStatusKey(String key, int color) {
        currentStatusKey = key;
        txtStatus.setText(UiStrings.get(this, key));
        viewStatusIndicator.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    private void setConnected(boolean connected) {
        if (!connected) {
            setRobotOffline();
            return;
        }
        isConnected = connected;
        isDisconnectedState = false;
        setControlsEnabled(connected);
        btnStop.setEnabled(true);
        btnStop.setText(UiStrings.get(this, UiStrings.KEY_DISCONNECT));
        btnStop.setBackgroundTintList(ColorStateList.valueOf(COLOR_GREEN));
        btnCenterStop.setBackgroundTintList(ColorStateList.valueOf(COLOR_RED));
        if (connected) {
            setStatusKey(UiStrings.KEY_STATUS_RUNNING, COLOR_GREEN);
        } else {
            setStatusKey(UiStrings.KEY_STATUS_CLOSED, COLOR_RED);
        }
    }

    private void setRobotOffline() {
        isConnected = false;
        isDisconnectedState = false;
        setControlsEnabled(false);
        btnStop.setEnabled(false);
        btnStop.setText(UiStrings.get(this, UiStrings.KEY_DISCONNECT));
        btnStop.setBackgroundTintList(ColorStateList.valueOf(COLOR_GRAY));
        btnCenterStop.setBackgroundTintList(ColorStateList.valueOf(COLOR_GRAY));
        setStatusKey(UiStrings.KEY_STATUS_CLOSED, COLOR_RED);
    }

    private void setPreviewMode() {
        isConnected = false;
        isDisconnectedState = false;
        setControlsEnabled(true);
        btnStop.setEnabled(true);
        btnStop.setText(UiStrings.get(this, UiStrings.KEY_DISCONNECT));
        btnStop.setBackgroundTintList(ColorStateList.valueOf(COLOR_GREEN));
        btnCenterStop.setBackgroundTintList(ColorStateList.valueOf(COLOR_RED));
        setStatusKey(UiStrings.KEY_STATUS_RUNNING, COLOR_GREEN);
    }

    private void setDisconnectedState() {
        isConnected = false;
        isDisconnectedState = true;
        setControlsEnabled(false);
        btnStop.setEnabled(true);
        btnStop.setText(UiStrings.get(this, UiStrings.KEY_RECONNECT));
        btnStop.setBackgroundTintList(ColorStateList.valueOf(COLOR_GREEN));
        btnCenterStop.setBackgroundTintList(ColorStateList.valueOf(COLOR_GRAY));
        setStatusKey(UiStrings.KEY_STATUS_DISCONNECTED, COLOR_RED);
    }

    private void checkRobotOnline() {
        setRobotOffline();
        executor.execute(() -> {
            boolean ok = checkConnection(baseUrl);
            runOnUiThread(() -> {
                if (ok) {
                    setConnected(true);
                } else {
                    Toast.makeText(this, UiStrings.get(this, UiStrings.KEY_TOAST_TURN_ON), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private boolean checkConnection(String base) {
        if (base == null || base.trim().isEmpty()) {
            return false;
        }
        return httpGetHealth(buildControlUrl("/health"));
    }

    private String buildControlUrl(String path) {
        Uri uri = Uri.parse(baseUrl);
        String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) {
            String raw = baseUrl.replaceFirst("^https?://", "");
            int slashIndex = raw.indexOf('/');
            if (slashIndex >= 0) {
                raw = raw.substring(0, slashIndex);
            }
            int colonIndex = raw.indexOf(':');
            host = colonIndex >= 0 ? raw.substring(0, colonIndex) : raw;
        }
        return scheme + "://" + host + ":" + CONTROL_PORT + path;
    }

    private void setControlsEnabled(boolean enabled) {
        btnUp.setEnabled(enabled);
        btnDown.setEnabled(enabled);
        btnLeft.setEnabled(enabled);
        btnRight.setEnabled(enabled);
        btnCenterStop.setEnabled(enabled);
        btnLiftUp.setEnabled(enabled);
        btnLiftDown.setEnabled(enabled);
        float alpha = enabled ? 1f : DISABLED_ALPHA;
        btnUp.setAlpha(alpha);
        btnDown.setAlpha(alpha);
        btnLeft.setAlpha(alpha);
        btnRight.setAlpha(alpha);
        btnCenterStop.setAlpha(alpha);
        btnLiftUp.setAlpha(alpha);
        btnLiftDown.setAlpha(alpha);
    }

    private void sendCommand(String command) {
        String speakLabel = commandLabel(command);
        if (speakLabel != null) {
            TtsManager.speak(this, speakLabel);
        }
        if (isPreview) {
            logAction(command);
            return;
        }
        if (!isConnected) {
            Toast.makeText(this, UiStrings.get(this, UiStrings.KEY_TOAST_NOT_CONNECTED), Toast.LENGTH_SHORT).show();
            return;
        }
        logAction(command);
        String driveAction = mapDriveAction(command);
        executor.execute(() -> {
            boolean ok;
            if ("STOP".equals(command)) {
                ok = httpPostJson(buildControlUrl("/stop"), null);
            } else if (driveAction != null) {
                ok = httpPostJson(buildControlUrl("/drive"), buildDrivePayload(driveAction));
            } else {
                ok = false;
            }
            if (!ok) {
                runOnUiThread(() -> Toast.makeText(this,
                        UiStrings.get(this, UiStrings.KEY_TOAST_SEND_ERROR), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private String mapDriveAction(String command) {
        if ("AVANT".equals(command)) {
            return "forward";
        }
        if ("ARRIÈRE".equals(command)) {
            return "backward";
        }
        if ("GAUCHE".equals(command)) {
            return "left";
        }
        if ("DROITE".equals(command)) {
            return "right";
        }
        if ("MONTER".equals(command)) {
            return "up";
        }
        if ("DESCENDRE".equals(command)) {
            return "down";
        }
        return null;
    }

    private String buildDrivePayload(String action) {
        try {
            JSONObject body = new JSONObject();
            body.put("action", action);
            return body.toString();
        } catch (JSONException e) {
            return "{\"action\":\"" + action + "\"}";
        }
    }

    private boolean httpGetHealth(String urlString) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(1500);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                return false;
            }
            String body = readBody(conn.getInputStream());
            if (body == null || body.trim().isEmpty()) {
                return false;
            }
            JSONObject json = new JSONObject(body);
            return json.optBoolean("ok", false);
        } catch (IOException e) {
            return false;
        } catch (JSONException e) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private boolean httpPostJson(String urlString, String jsonBody) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(1500);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            if (jsonBody != null) {
                byte[] data = jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(data.length);
                OutputStream os = conn.getOutputStream();
                os.write(data);
                os.flush();
                os.close();
            }
            int code = conn.getResponseCode();
            return code >= 200 && code < 300;
        } catch (IOException e) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String readBody(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyLanguage();
        applyContrast();
    }

    private void applyLanguage() {
        txtBackLabel.setText(UiStrings.get(this, UiStrings.KEY_BACK));
        btnCenterStop.setText(UiStrings.get(this, UiStrings.KEY_STOP));
        if (isDisconnectedState) {
            btnStop.setText(UiStrings.get(this, UiStrings.KEY_RECONNECT));
        } else {
            btnStop.setText(UiStrings.get(this, UiStrings.KEY_DISCONNECT));
        }
        txtStatus.setText(UiStrings.get(this, currentStatusKey));
        applyLiftIcons();
    }

    private void applyContrast() {
        ContrastHelper.apply(findViewById(R.id.main), UiPrefs.isContrastEnabled(this));
    }

    private void applyLiftIcons() {
        String lang = UiPrefs.getLang(this);
        if (UiStrings.LANG_EN.equals(lang)) {
            btnLiftUp.setImageResource(R.drawable.monter_anglais);
            btnLiftDown.setImageResource(R.drawable.descendre_anglais);
        } else if (UiStrings.LANG_ZH.equals(lang)) {
            btnLiftUp.setImageResource(R.drawable.monter_chinoise);
            btnLiftDown.setImageResource(R.drawable.descendre_chinoise);
        } else {
            btnLiftUp.setImageResource(R.drawable.monter);
            btnLiftDown.setImageResource(R.drawable.descendre);
        }
    }

    private String commandLabel(String command) {
        if (command == null) {
            return null;
        }
        switch (command) {
            case "AVANT":
                return UiStrings.get(this, UiStrings.KEY_CMD_FORWARD);
            case "ARRIÈRE":
                return UiStrings.get(this, UiStrings.KEY_CMD_BACKWARD);
            case "GAUCHE":
                return UiStrings.get(this, UiStrings.KEY_CMD_LEFT);
            case "DROITE":
                return UiStrings.get(this, UiStrings.KEY_CMD_RIGHT);
            case "MONTER":
                return UiStrings.get(this, UiStrings.KEY_CMD_LIFT_UP);
            case "DESCENDRE":
                return UiStrings.get(this, UiStrings.KEY_CMD_LIFT_DOWN);
            case "STOP":
                return UiStrings.get(this, UiStrings.KEY_STOP);
            default:
                return null;
        }
    }
}
