package net.chezxinqi.carrybot3;

import android.os.Bundle;
import android.os.Build;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "CarryBot";
    private static final String EXTRA_CONNECTED = "connected";
    private static final String EXTRA_BASE_URL = "base_url";
    private static final String EXTRA_PREVIEW = "preview";
    private static final String EXTRA_DEVICE_NAME = "device_name";
    private static final String EXTRA_CONTROL_PORT = "control_port";
    private static final String DEFAULT_BASE_URL = "";
    private static final int CONTROL_PORT = 8080;
    private static final int[] CONTROL_PORT_CANDIDATES = new int[]{8080};

    private TextView txtTitle;
    private TextView txtStatus;
    private TextView txtStatusLabel;
    private TextView txtBackLabel;
    private TextView txtVideoLabel;
    private Button btnStop;
    private ImageButton btnUp;
    private ImageButton btnDown;
    private ImageButton btnLeft;
    private ImageButton btnRight;
    private Button btnCenterStop;
    private ImageButton btnLiftUp;
    private ImageButton btnLiftDown;
    private View viewStatusIndicator;
    private LinearLayout container;
    private MaterialCardView cardVideo;
    private MaterialCardView cardStatus;
    private MaterialCardView cardDirection;
    private MaterialCardView cardStop;
    private MaterialCardView cardLift;
    private ConstraintLayout dpad;
    private SwitchCompat swVideo;
    private WebView webVideo;

    private static final int COLOR_RED = Color.parseColor("#C94A4A");
    private static final int COLOR_GREEN = Color.parseColor("#43A047");
    private static final int COLOR_YELLOW = Color.parseColor("#F2B705");
    private static final int COLOR_GRAY = Color.parseColor("#BDBDBD");
    private static final float DISABLED_ALPHA = 0.4f;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isConnected = false;
    private boolean isPreview = false;
    private boolean isDisconnectedState = false;
    private boolean isUpdatingVideoSwitch = false;
    private String baseUrl = DEFAULT_BASE_URL;
    private String currentStatusKey = UiStrings.KEY_STATUS_CLOSED;
    private int activeControlPort = CONTROL_PORT;

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
        txtStatusLabel = findViewById(R.id.txtStatusLabel);
        txtTitle = findViewById(R.id.txtTitle);
        btnStop = findViewById(R.id.btnStop);
        txtBackLabel = findViewById(R.id.txtBackLabel);
        txtVideoLabel = findViewById(R.id.txtVideoLabel);
        container = findViewById(R.id.container);
        swVideo = findViewById(R.id.swVideo);
        cardVideo = findViewById(R.id.cardVideo);
        cardStatus = findViewById(R.id.cardStatus);
        cardDirection = findViewById(R.id.cardDirection);
        cardStop = findViewById(R.id.cardStop);
        cardLift = findViewById(R.id.cardLift);
        dpad = findViewById(R.id.dpad);
        webVideo = findViewById(R.id.webVideo);
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
        styleSwitch(swVideo);
        setupVideoWebView();
        swVideo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingVideoSwitch) {
                return;
            }
            TtsManager.speak(this, UiStrings.get(this, UiStrings.KEY_VIDEO_BUTTON));
            toggleVideo(isChecked);
        });
        applyControlLayout(false);

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
            ensureWifiRouting();
            boolean initialConnected = getIntent().getBooleanExtra(EXTRA_CONNECTED, false);
            int incomingPort = getIntent().getIntExtra(EXTRA_CONTROL_PORT, CONTROL_PORT);
            if (incomingPort > 0) {
                activeControlPort = incomingPort;
            }
            if (initialConnected) {
                setConnected(true);
            } else {
                setRobotOffline();
            }
            checkRobotOnline(false);
        }

        bindAction(btnUp, "AVANT");
        bindAction(btnDown, "ARRIÈRE");
        bindAction(btnLeft, "GAUCHE");
        bindAction(btnRight, "DROITE");
        bindAction(btnLiftUp, "MONTER");
        bindAction(btnLiftDown, "DESCENDRE");

        btnStop.setOnClickListener(v -> {
            String label;
            if (isDisconnectedState || (!isConnected && !isPreview)) {
                label = UiStrings.get(this, UiStrings.KEY_RECONNECT);
            } else {
                label = UiStrings.get(this, UiStrings.KEY_DISCONNECT);
            }
            TtsManager.speak(this, label);
            if (isDisconnectedState) {
                startActivity(new android.content.Intent(this, DeviceSelectActivity.class));
                finish();
                return;
            }
            if (!isConnected && !isPreview) {
                checkRobotOnline(true);
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

    private float dp(int value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private void styleSwitch(SwitchCompat sw) {
        if (sw == null) {
            return;
        }
        sw.setTrackResource(R.drawable.switch_track);
        sw.setThumbResource(R.drawable.switch_thumb);
        sw.setTrackTintList(null);
        sw.setThumbTintList(null);
        sw.setSplitTrack(false);
        int w = (int) dp(60);
        int h = (int) dp(26);
        sw.setSwitchMinWidth(w);
        sw.setMinWidth(w);
        sw.setMinimumWidth(w);
        sw.setMinHeight(h);
        sw.setMinimumHeight(h);
        sw.setPadding(0, 0, 0, 0);
    }

    private void setupVideoWebView() {
        if (webVideo == null) {
            return;
        }
        WebSettings settings = webVideo.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        webVideo.setWebViewClient(new WebViewClient());
    }

    private void toggleVideo(boolean enabled) {
        cardVideo.setVisibility(enabled ? View.VISIBLE : View.GONE);
        applyControlLayout(enabled);
        if (enabled) {
            startVideoStream();
        } else {
            stopVideoStream();
        }
    }

    private void startVideoStream() {
        if (webVideo == null || baseUrl == null || baseUrl.trim().isEmpty()) {
            return;
        }
        ensureWifiRouting();
        webVideo.loadUrl(buildControlUrl(baseUrl, CONTROL_PORT, "/video_feed"));
    }

    private void stopVideoStream() {
        if (webVideo == null) {
            return;
        }
        webVideo.stopLoading();
        webVideo.loadUrl("about:blank");
    }

    private void forceDisableVideo() {
        if (swVideo != null && swVideo.isChecked()) {
            isUpdatingVideoSwitch = true;
            swVideo.setChecked(false);
            isUpdatingVideoSwitch = false;
        }
        toggleVideo(false);
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
        forceDisableVideo();
        setControlsEnabled(false);
        btnStop.setEnabled(true);
        btnStop.setText(UiStrings.get(this, UiStrings.KEY_RECONNECT));
        btnStop.setBackgroundTintList(ColorStateList.valueOf(COLOR_GREEN));
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
        forceDisableVideo();
        setControlsEnabled(false);
        btnStop.setEnabled(true);
        btnStop.setText(UiStrings.get(this, UiStrings.KEY_RECONNECT));
        btnStop.setBackgroundTintList(ColorStateList.valueOf(COLOR_GREEN));
        btnCenterStop.setBackgroundTintList(ColorStateList.valueOf(COLOR_GRAY));
        setStatusKey(UiStrings.KEY_STATUS_DISCONNECTED, COLOR_RED);
    }

    private void checkRobotOnline(boolean showFailureToast) {
        ensureWifiRouting();
        if (showFailureToast) {
            setRobotOffline();
        }
        executor.execute(() -> {
            boolean ok = checkConnection(baseUrl);
            runOnUiThread(() -> {
                if (ok) {
                    setConnected(true);
                } else {
                    setRobotOffline();
                    if (showFailureToast) {
                        Toast.makeText(this, UiStrings.get(this, UiStrings.KEY_TOAST_TURN_ON), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    private boolean checkConnection(String base) {
        if (base == null || base.trim().isEmpty()) {
            return false;
        }
        String host = extractHost(base);
        if (host == null || host.trim().isEmpty()) {
            Log.d(TAG, "Invalid base url: " + base);
            return false;
        }
        for (int port : CONTROL_PORT_CANDIDATES) {
            if (httpGetHealth(buildControlUrl(base, port, "/health"))) {
                activeControlPort = port;
                return true;
            }
            if (httpGetHealth(buildControlUrl(base, port, "/status"))) {
                activeControlPort = port;
                return true;
            }
            if (httpGetHealth(buildControlUrl(base, port, "/ping"))) {
                activeControlPort = port;
                return true;
            }
            if (isTcpReachable(host, port, 2000)) {
                activeControlPort = port;
                return true;
            }
        }
        return false;
    }

    private String buildControlUrl(String path) {
        return buildControlUrl(baseUrl, activeControlPort, path);
    }

    private String buildControlUrl(String base, int port, String path) {
        Uri uri = Uri.parse(base);
        String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
        String host = extractHost(base);
        return scheme + "://" + host + ":" + port + path;
    }

    private String extractHost(String base) {
        Uri uri = Uri.parse(base);
        String host = uri.getHost();
        if (host != null && !host.trim().isEmpty()) {
            return host;
        }
        String raw = base.replaceFirst("^https?://", "");
        int slashIndex = raw.indexOf('/');
        if (slashIndex >= 0) {
            raw = raw.substring(0, slashIndex);
        }
        int colonIndex = raw.indexOf(':');
        return colonIndex >= 0 ? raw.substring(0, colonIndex) : raw;
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
        executor.execute(() -> {
            boolean ok = executeCommandRequest(command);
            if (!ok) {
                ok = tryFallbackPorts(command);
            }
            final boolean commandOk = ok;
            runOnUiThread(() -> {
                if (commandOk) {
                    if ("STOP".equals(command)) {
                        setStatusKey(UiStrings.KEY_STATUS_PAUSED, COLOR_YELLOW);
                    }
                } else {
                    Toast.makeText(this, UiStrings.get(this, UiStrings.KEY_TOAST_SEND_ERROR), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private boolean executeCommandRequest(String command) {
        String driveAction = mapDriveAction(command);
        if ("STOP".equals(command)) {
            return httpPostJson(buildControlUrl("/stop"), null);
        }
        if (driveAction != null) {
            return httpPostJson(buildControlUrl("/drive"), buildDrivePayload(driveAction));
        }
        return false;
    }

    private boolean tryFallbackPorts(String command) {
        int originalPort = activeControlPort;
        for (int port : CONTROL_PORT_CANDIDATES) {
            activeControlPort = port;
            if (executeCommandRequest(command)) {
                return true;
            }
        }
        activeControlPort = originalPort;
        return false;
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
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                if (code == 403 || code == 404 || code == 405) {
                    Log.d(TAG, "Endpoint not found/blocked but server reachable: " + urlString + " code=" + code);
                    return true;
                }
                Log.d(TAG, "Health check non-2xx: " + urlString + " code=" + code);
                return false;
            }
            String body = readBody(conn.getInputStream());
            if (body == null || body.trim().isEmpty()) {
                Log.d(TAG, "Health check ok with empty body: " + urlString);
                return true;
            }
            try {
                JSONObject json = new JSONObject(body);
                if (json.has("ok") && !json.optBoolean("ok", false)) {
                    Log.d(TAG, "Health endpoint reachable but ok=false: " + urlString);
                }
                return true;
            } catch (JSONException ignored) {
                return true;
            }
        } catch (IOException e) {
            Log.d(TAG, "Health check failed: " + urlString + " error=" + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private boolean isTcpReachable(String host, int port, int timeoutMs) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException e) {
            Log.d(TAG, "TCP check failed: " + host + ":" + port + " error=" + e.getMessage());
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void ensureWifiRouting() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (cm == null) {
                return;
            }
            Network[] networks = cm.getAllNetworks();
            for (Network network : networks) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    cm.bindProcessToNetwork(network);
                    Log.d(TAG, "Bound to Wi-Fi network for robot communication");
                    return;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to bind Wi-Fi route: " + e.getMessage());
        }
    }

    private boolean httpPostJson(String urlString, String jsonBody) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
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
            Log.d(TAG, "POST failed: " + urlString + " error=" + e.getMessage());
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
        stopVideoStream();
        if (webVideo != null) {
            webVideo.destroy();
        }
        executor.shutdownNow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyLanguage();
        applyContrast();
        if (swVideo != null && swVideo.isChecked()) {
            applyControlLayout(true);
            startVideoStream();
        } else {
            applyControlLayout(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (swVideo != null && swVideo.isChecked()) {
            stopVideoStream();
        }
    }

    private void applyLanguage() {
        txtBackLabel.setText(UiStrings.get(this, UiStrings.KEY_BACK));
        txtStatusLabel.setText(UiStrings.get(this, UiStrings.KEY_STATUS_LABEL));
        txtVideoLabel.setText(UiStrings.get(this, UiStrings.KEY_VIDEO_BUTTON));
        btnCenterStop.setText(UiStrings.get(this, UiStrings.KEY_STOP));
        if (isDisconnectedState) {
            btnStop.setText(UiStrings.get(this, UiStrings.KEY_RECONNECT));
        } else if (!isConnected && !isPreview) {
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

    private void applyControlLayout(boolean compact) {
        if (container == null) {
            return;
        }
        int outerPadding = compact ? 9 : 12;
        container.setPadding(toPx(outerPadding), 0, toPx(outerPadding), toPx(outerPadding));

        applyCardStyle(cardVideo, compact, compact ? 3 : 0);
        applyCardStyle(cardStatus, compact, compact ? 3 : 0);
        applyCardStyle(cardDirection, compact, compact ? 3 : 0);
        applyCardStyle(cardStop, compact, compact ? 3 : 0);
        applyCardStyle(cardLift, compact, 0);

        txtStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, compact ? 19f : 22f);
        ViewGroup.LayoutParams dotLp = viewStatusIndicator.getLayoutParams();
        int dotSize = toPx(compact ? 12 : 14);
        dotLp.width = dotSize;
        dotLp.height = dotSize;
        viewStatusIndicator.setLayoutParams(dotLp);

        ViewGroup.LayoutParams dpadLp = dpad.getLayoutParams();
        dpadLp.height = toPx(compact ? 210 : 260);
        dpad.setLayoutParams(dpadLp);

        updateDpadButtonLayout(btnUp, compact ? 82 : 108, compact ? 12 : 16, "bottom");
        updateDpadButtonLayout(btnDown, compact ? 82 : 108, compact ? 12 : 16, "top");
        updateDpadButtonLayout(btnLeft, compact ? 82 : 108, compact ? 62 : 86, "end");
        updateDpadButtonLayout(btnRight, compact ? 82 : 108, compact ? 62 : 86, "start");

        ViewGroup.LayoutParams centerStopLp = btnCenterStop.getLayoutParams();
        centerStopLp.height = toPx(compact ? 58 : 70);
        btnCenterStop.setLayoutParams(centerStopLp);
        btnCenterStop.setTextSize(TypedValue.COMPLEX_UNIT_SP, compact ? 18f : 20f);

        ViewGroup.LayoutParams upLp = btnLiftUp.getLayoutParams();
        upLp.width = toPx(compact ? 114 : 140);
        upLp.height = toPx(compact ? 114 : 140);
        btnLiftUp.setLayoutParams(upLp);

        ViewGroup.LayoutParams downLp = btnLiftDown.getLayoutParams();
        downLp.width = toPx(compact ? 114 : 140);
        downLp.height = toPx(compact ? 114 : 140);
        btnLiftDown.setLayoutParams(downLp);
    }

    private void applyCardStyle(MaterialCardView card, boolean compact, int marginBottomDp) {
        if (card == null) {
            return;
        }
        card.setUseCompatPadding(!compact);
        card.setCardElevation(dp(compact ? 3 : 4));
        ViewGroup.LayoutParams lp = card.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) lp).bottomMargin = toPx(marginBottomDp);
            card.setLayoutParams(lp);
        }
    }

    private void updateDpadButtonLayout(ImageButton button, int sizeDp, int marginDp, String marginSide) {
        if (button == null) {
            return;
        }
        ViewGroup.LayoutParams lp = button.getLayoutParams();
        if (!(lp instanceof ConstraintLayout.LayoutParams)) {
            return;
        }
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) lp;
        int px = toPx(sizeDp);
        params.width = px;
        params.height = px;
        int marginPx = toPx(marginDp);
        if ("top".equals(marginSide)) {
            params.topMargin = marginPx;
        } else if ("bottom".equals(marginSide)) {
            params.bottomMargin = marginPx;
        } else if ("start".equals(marginSide)) {
            params.setMarginStart(marginPx);
        } else if ("end".equals(marginSide)) {
            params.setMarginEnd(marginPx);
        }
        button.setLayoutParams(params);
    }

    private int toPx(int valueDp) {
        return (int) dp(valueDp);
    }
}
