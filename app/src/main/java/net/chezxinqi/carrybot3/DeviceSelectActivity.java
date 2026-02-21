package net.chezxinqi.carrybot3;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeviceSelectActivity extends AppCompatActivity {

    private static final int[] CONTROL_PORT_CANDIDATES = new int[]{8090, 8080, 5000, 80};
    private TextView txtSelectTitle;
    private TextView txtTtsLabel;
    private TextView txtContrastLabel;
    private MaterialButton btnLang;
    private SwitchCompat swTts;
    private SwitchCompat swContrast;
    private boolean isUpdatingToggles = false;
    private RecyclerView recyclerView;
    private DeviceAdapter adapter;
    private final ExecutorService connectionExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_select);

        findViewById(R.id.imgSelectLogo).setOnClickListener(v -> openPreview());
        findViewById(R.id.btnAddDevice).setOnClickListener(v -> openAdd());
        txtSelectTitle = findViewById(R.id.txtSelectTitle);
        txtTtsLabel = findViewById(R.id.txtTtsLabel);
        txtContrastLabel = findViewById(R.id.txtContrastLabel);
        btnLang = findViewById(R.id.btnLang);
        swTts = findViewById(R.id.swTts);
        swContrast = findViewById(R.id.swContrast);
        styleSwitch(swTts);
        styleSwitch(swContrast);
        recyclerView = findViewById(R.id.deviceList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setNestedScrollingEnabled(true);
        adapter = new DeviceAdapter();
        recyclerView.setAdapter(adapter);

        btnLang.setOnClickListener(v -> showLanguagePicker());
        swTts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingToggles) {
                return;
            }
            toggleTts(isChecked);
        });
        swContrast.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingToggles) {
                return;
            }
            toggleContrast(isChecked);
        });

        TtsManager.warmUp(this);
        applyLanguage();
        applyContrast();
        updateToggles();
        renderDevices();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyLanguage();
        applyContrast();
        updateToggles();
        renderDevices();
    }

    private void renderDevices() {
        List<DeviceStore.Device> devices = DeviceStore.load(this);
        adapter.setDevices(devices);
    }

    private float dp(int value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private void styleSwitch(SwitchCompat sw) {
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
        sw.setScaleX(1.0f);
        sw.setScaleY(1.0f);
    }

    private void openAdd() {
        TtsManager.speak(this, UiStrings.get(this, UiStrings.KEY_ADD));
        startActivity(new Intent(this, ConnectActivity.class));
    }

    private void openMain(DeviceStore.Device device) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("base_url", device.baseUrl);
        intent.putExtra("device_name", device.name);
        intent.putExtra("preview", false);
        intent.putExtra("connected", false);
        intent.putExtra("control_port", 8080);
        startActivity(intent);
    }

    private void openPreview() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("preview", true);
        startActivity(intent);
    }

    private void applyLanguage() {
        txtSelectTitle.setText(UiStrings.get(this, UiStrings.KEY_SELECT_TITLE));
        btnLang.setText(UiStrings.get(this, UiStrings.KEY_LANG_BUTTON));
        txtTtsLabel.setText(UiStrings.get(this, UiStrings.KEY_TTS_BUTTON));
        txtContrastLabel.setText(UiStrings.get(this, UiStrings.KEY_CONTRAST_BUTTON));
    }

    private void updateToggles() {
        isUpdatingToggles = true;
        swTts.setChecked(UiPrefs.isTtsEnabled(this));
        swContrast.setChecked(UiPrefs.isContrastEnabled(this));
        isUpdatingToggles = false;
    }

    private void applyContrast() {
        ContrastHelper.apply(findViewById(R.id.deviceSelectRoot), UiPrefs.isContrastEnabled(this));
    }

    private void toggleTts(boolean enabled) {
        if (enabled) {
            UiPrefs.setTtsEnabled(this, true);
            updateToggles();
            TtsManager.applyLanguage(this);
            Toast.makeText(this, UiStrings.get(this, UiStrings.KEY_TTS_ON), Toast.LENGTH_SHORT).show();
            TtsManager.speak(this, UiStrings.get(this, UiStrings.KEY_TTS_ON));
            return;
        }
        TtsManager.speak(this, UiStrings.get(this, UiStrings.KEY_TTS_OFF));
        UiPrefs.setTtsEnabled(this, false);
        updateToggles();
        Toast.makeText(this, UiStrings.get(this, UiStrings.KEY_TTS_OFF), Toast.LENGTH_SHORT).show();
    }

    private void toggleContrast(boolean enabled) {
        UiPrefs.setContrastEnabled(this, enabled);
        applyContrast();
        updateToggles();
        String msg = UiStrings.get(this, enabled ? UiStrings.KEY_CONTRAST_ON : UiStrings.KEY_CONTRAST_OFF);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        TtsManager.speak(this, msg);
    }

    private void showLanguagePicker() {
        TtsManager.speak(this, UiStrings.get(this, UiStrings.KEY_LANG_BUTTON));
        String[] labels = {"Français", "English", "中文"};
        int[] icons = {R.drawable.flag_fr, R.drawable.flag_uk, R.drawable.flag_cn};
        new AlertDialog.Builder(this)
                .setAdapter(new android.widget.ArrayAdapter<String>(this, android.R.layout.select_dialog_item, labels) {
                    @Override
                    public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                        android.view.View view = super.getView(position, convertView, parent);
                        android.widget.TextView textView = view.findViewById(android.R.id.text1);
                        android.graphics.drawable.Drawable icon = ContextCompat.getDrawable(DeviceSelectActivity.this, icons[position]);
                        if (icon != null) {
                            int w = (int) dp(24);
                            int h = (int) dp(16);
                            icon.setBounds(0, 0, w, h);
                            textView.setCompoundDrawables(icon, null, null, null);
                            textView.setCompoundDrawablePadding((int) dp(8));
                        }
                        return view;
                    }
                }, (dialog, which) -> {
                    String lang = UiStrings.LANG_FR;
                    String spoken = "Français";
                    if (which == 1) {
                        lang = UiStrings.LANG_EN;
                        spoken = "English";
                    } else if (which == 2) {
                        lang = UiStrings.LANG_ZH;
                        spoken = "中文";
                    }
                    UiPrefs.setLang(this, lang);
                    TtsManager.applyLanguage(this);
                    TtsManager.speak(this, spoken);
                    applyLanguage();
                    renderDevices();
                })
                .show();
    }

    private void showDeleteConfirm(DeviceStore.Device device) {
        new AlertDialog.Builder(this)
                .setMessage(UiStrings.get(this, UiStrings.KEY_DELETE_CONFIRM))
                .setPositiveButton(UiStrings.get(this, UiStrings.KEY_YES), (d, w) -> {
                    DeviceStore.removeDevice(this, device.baseUrl);
                    renderDevices();
                })
                .setNegativeButton(UiStrings.get(this, UiStrings.KEY_NO), null)
                .show();
    }

    private int detectControlPort(String baseUrl) {
        ensureWifiRouting();
        String host = extractHost(baseUrl);
        if (host == null || host.trim().isEmpty()) {
            return -1;
        }
        for (int port : CONTROL_PORT_CANDIDATES) {
            if (isHttpReachable(buildUrl(baseUrl, port, "/health"))
                    || isHttpReachable(buildUrl(baseUrl, port, "/status"))
                    || isHttpReachable(buildUrl(baseUrl, port, "/ping"))
                    || isTcpReachable(host, port)) {
                return port;
            }
        }
        return -1;
    }

    private String buildUrl(String base, int port, String path) {
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
        int slash = raw.indexOf('/');
        if (slash >= 0) {
            raw = raw.substring(0, slash);
        }
        int colon = raw.indexOf(':');
        return colon >= 0 ? raw.substring(0, colon) : raw;
    }

    private boolean isHttpReachable(String urlString) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            return (code >= 200 && code < 300) || code == 403 || code == 404 || code == 405;
        } catch (IOException ignored) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private boolean isTcpReachable(String host, int port) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), 4000);
            return true;
        } catch (IOException ignored) {
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
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectionExecutor.shutdownNow();
    }

    private class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

        private List<DeviceStore.Device> devices = java.util.Collections.emptyList();

        void setDevices(List<DeviceStore.Device> list) {
            devices = list == null ? java.util.Collections.emptyList() : list;
            notifyDataSetChanged();
        }

        @Override
        public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_device, parent, false);
            return new DeviceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(DeviceViewHolder holder, int position) {
            DeviceStore.Device device = devices.get(position);
            holder.bind(device);
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        class DeviceViewHolder extends RecyclerView.ViewHolder {
            private final MaterialButton btnDevice;
            private final ImageButton btnDelete;

            DeviceViewHolder(View itemView) {
                super(itemView);
                btnDevice = itemView.findViewById(R.id.btnDevice);
                btnDelete = itemView.findViewById(R.id.btnDelete);
                btnDevice.setInsetTop(0);
                btnDevice.setInsetBottom(0);
                btnDevice.setStateListAnimator(null);
                btnDevice.setMinHeight(0);
                btnDevice.setMinimumHeight(0);
            }

            void bind(DeviceStore.Device device) {
                btnDevice.setText(device.name);
                btnDevice.setOnClickListener(v -> {
                    TtsManager.speak(DeviceSelectActivity.this, device.name);
                    openMain(device);
                });
                btnDelete.setOnClickListener(v -> {
                    TtsManager.speak(DeviceSelectActivity.this,
                            UiStrings.get(DeviceSelectActivity.this, UiStrings.KEY_DELETE));
                    showDeleteConfirm(device);
                });
                btnDelete.setColorFilter(0xFFC94A4A);
                btnDelete.setContentDescription(UiStrings.get(DeviceSelectActivity.this, UiStrings.KEY_DELETE));
            }
        }
    }
}
