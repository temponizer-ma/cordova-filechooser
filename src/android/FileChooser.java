package com.megster.cordova;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.provider.OpenableColumns;
import android.database.Cursor;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FileChooser extends CordovaPlugin {

    private static final String TAG = "FileChooser";
    private static final String ACTION_OPEN = "open";
    private static final int PICK_FILE_REQUEST = 1;

    public static final String MIME = "mime";

    CallbackContext callback;

    @Override
    public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {

        if (action.equals(ACTION_OPEN)) {
            JSONObject filters = inputs.optJSONObject(0);
            chooseFile(filters, callbackContext);
            return true;
        }

        return false;
    }

    public void chooseFile(JSONObject filter, CallbackContext callbackContext) {
        String uri_filter = filter.has(MIME) ? filter.optString(MIME) : "*/*";

        // type and title should be configurable

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(uri_filter);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

        Intent chooser = Intent.createChooser(intent, "Select File");
        cordova.startActivityForResult(this, chooser, PICK_FILE_REQUEST);

        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callback = callbackContext;
        callbackContext.sendPluginResult(pluginResult);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PICK_FILE_REQUEST && callback != null) {

            if (resultCode == Activity.RESULT_OK) {

                Uri uri = data.getData();

                if (uri != null) {
                    JSONObject jsonObject = new JSONObject();

                    try {
                        jsonObject.put("uri", uri.toString());
                        Log.w(TAG, uri.toString());

                        try {
                            Cursor cursor = cordova.getActivity().getContentResolver().query(uri, null, null, null, null, null);
                            if (cursor != null && cursor.moveToFirst()) {
                                String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                                jsonObject.put("name", displayName);
                                Log.i(TAG, "Display Name: " + displayName);

                                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                                int size = -1;
                                if (!cursor.isNull(sizeIndex)) {
                                    size = cursor.getInt(sizeIndex);
                                }
                                jsonObject.put("size", size);
                                Log.i(TAG, "Size: " + size);
                            }
                        } catch (Exception e) {}
                    } catch (JSONException e) {
                        callback.error("JSON error");
                        return;
                    }

                    callback.success(jsonObject);

                } else {

                    callback.error("File uri was null");

                }

            } else if (resultCode == Activity.RESULT_CANCELED) {
                // keep this string the same as in iOS document picker plugin
                // https://github.com/iampossible/Cordova-DocPicker
                callback.error("User canceled.");
            } else {

                callback.error(resultCode);
            }
        }
    }
}
