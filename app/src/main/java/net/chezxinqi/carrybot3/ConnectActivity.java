package net.chezxinqi.carrybot3;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ConnectActivity extends AppCompatActivity {

    private static final String BASE_PREFIX = "http://192.168.4."; // TODO: change to your Pi prefix
    private static final String EXTRA_PREVIEW = "preview";

    private MaterialButton btnConnect;
    private TextView txtConnectStatus;
    private TextView txtBackLabel;
    private EditText edtPiOctet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        TtsManager.warmUp(this);

        btnConnect = findViewById(R.id.btnConnect);
        txtConnectStatus = findViewById(R.id.txtConnectStatus);
        txtBackLabel = findViewById(R.id.txtBackLabel);
        edtPiOctet = findViewById(R.id.edtPiOctet);
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            TtsManager.speak(this, UiStrings.get(this, UiStrings.KEY_BACK));
            startActivity(new android.content.Intent(this, DeviceSelectActivity.class));
            finish();
        });

        ImageView logo = findViewById(R.id.imgConnectLogo);
        logo.setOnClickListener(v -> openPreview());

        btnConnect.setOnClickListener(v -> addDevice());

        applyLanguage();
        applyContrast();
    }

    private void addDevice() {
        String octet = edtPiOctet.getText().toString().trim();
        if (octet.isEmpty()) {
            Toast.makeText(this, UiStrings.get(this, UiStrings.KEY_TOAST_ENTER_IP), Toast.LENGTH_SHORT).show();
            return;
        }
        TtsManager.speak(this, UiStrings.get(this, UiStrings.KEY_ADD));
        String baseUrl = buildBaseUrl(octet);
        String name = DeviceStore.suggestName(this);
        boolean added = DeviceStore.addDevice(this, name, baseUrl);
        if (!added) {
            Toast.makeText(this, UiStrings.get(this, UiStrings.KEY_TOAST_ALREADY_ADDED), Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new android.content.Intent(this, DeviceSelectActivity.class));
        finish();
    }

    private String buildBaseUrl(String input) {
        if (input.contains(".")) {
            return "http://" + input;
        }
        return BASE_PREFIX + input;
    }

    private void openPreview() {
        android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_PREVIEW, true);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyLanguage();
        applyContrast();
    }

    private void applyLanguage() {
        txtConnectStatus.setText(UiStrings.get(this, UiStrings.KEY_CONNECT_DESC));
        btnConnect.setText(UiStrings.get(this, UiStrings.KEY_ADD));
        txtBackLabel.setText(UiStrings.get(this, UiStrings.KEY_BACK));
    }

    private void applyContrast() {
        ContrastHelper.apply(findViewById(R.id.connectRoot), UiPrefs.isContrastEnabled(this));
    }
}
