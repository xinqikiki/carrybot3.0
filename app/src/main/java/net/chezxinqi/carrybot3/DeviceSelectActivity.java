package net.chezxinqi.carrybot3;

import android.content.Intent;
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

import java.util.List;

public class DeviceSelectActivity extends AppCompatActivity {

    private TextView txtSelectTitle;
    private TextView txtTtsLabel;
    private TextView txtContrastLabel;
    private MaterialButton btnLang;
    private SwitchCompat swTts;
    private SwitchCompat swContrast;
    private boolean isUpdatingToggles = false;
    private RecyclerView recyclerView;
    private DeviceAdapter adapter;

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

    private void openAdd() {
        TtsManager.speak(this, UiStrings.get(this, UiStrings.KEY_ADD));
        startActivity(new Intent(this, ConnectActivity.class));
    }

    private void openMain(DeviceStore.Device device) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("base_url", device.baseUrl);
        intent.putExtra("device_name", device.name);
        intent.putExtra("preview", false);
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
