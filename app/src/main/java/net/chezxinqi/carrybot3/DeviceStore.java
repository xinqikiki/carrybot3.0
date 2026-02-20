package net.chezxinqi.carrybot3;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class DeviceStore {

    private static final String PREFS = "carrybot_devices";
    private static final String KEY_DEVICES = "devices";

    private DeviceStore() {
    }

    public static class Device {
        public final String name;
        public final String baseUrl;

        public Device(String name, String baseUrl) {
            this.name = name;
            this.baseUrl = baseUrl;
        }
    }

    public static List<Device> load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_DEVICES, "[]");
        List<Device> devices = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) {
                    continue;
                }
                String name = obj.optString("name", "");
                String baseUrl = obj.optString("baseUrl", "");
                if (!name.isEmpty() && !baseUrl.isEmpty()) {
                    devices.add(new Device(name, baseUrl));
                }
            }
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }
        return devices;
    }

    public static boolean addDevice(Context context, String name, String baseUrl) {
        List<Device> devices = load(context);
        for (Device device : devices) {
            if (device.baseUrl.equalsIgnoreCase(baseUrl)) {
                return false;
            }
        }
        devices.add(new Device(name, baseUrl));
        save(context, devices);
        return true;
    }

    public static String suggestName(Context context) {
        List<Device> devices = load(context);
        int max = 0;
        for (Device device : devices) {
            String name = device.name == null ? "" : device.name.trim();
            if (name.toLowerCase().startsWith("carrybot")) {
                String[] parts = name.split("\\s+");
                if (parts.length > 1) {
                    try {
                        int n = Integer.parseInt(parts[parts.length - 1]);
                        if (n > max) {
                            max = n;
                        }
                    } catch (NumberFormatException ignored) {
                        // Ignore non-numeric suffix.
                    }
                }
            }
        }
        return "CarryBot " + (max + 1);
    }

    public static void removeDevice(Context context, String baseUrl) {
        List<Device> devices = load(context);
        List<Device> filtered = new ArrayList<>();
        for (Device device : devices) {
            if (!device.baseUrl.equalsIgnoreCase(baseUrl)) {
                filtered.add(device);
            }
        }
        save(context, filtered);
    }

    private static void save(Context context, List<Device> devices) {
        JSONArray array = new JSONArray();
        for (Device device : devices) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("name", device.name);
                obj.put("baseUrl", device.baseUrl);
                array.put(obj);
            } catch (JSONException ignored) {
                // Skip invalid entry.
            }
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_DEVICES, array.toString()).apply();
    }
}
