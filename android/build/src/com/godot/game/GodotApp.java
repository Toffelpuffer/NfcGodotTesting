/**************************************************************************/
/*  GodotApp.java                                                         */
/**************************************************************************/
/*                         This file is part of:                          */
/*                             GODOT ENGINE                               */
/*                        https://godotengine.org                         */
/**************************************************************************/
/* Copyright (c) 2014-present Godot Engine contributors (see AUTHORS.md). */
/* Copyright (c) 2007-2014 Juan Linietsky, Ariel Manzur.                  */
/*                                                                        */
/* Permission is hereby granted, free of charge, to any person obtaining  */
/* a copy of this software and associated documentation files (the        */
/* "Software"), to deal in the Software without restriction, including    */
/* without limitation the rights to use, copy, modify, merge, publish,    */
/* distribute, sublicense, and/or sell copies of the Software, and to     */
/* permit persons to whom the Software is furnished to do so, subject to  */
/* the following conditions:                                              */
/*                                                                        */
/* The above copyright notice and this permission notice shall be         */
/* included in all copies or substantial portions of the Software.        */
/*                                                                        */
/* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,        */
/* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF     */
/* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. */
/* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY   */
/* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,   */
/* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE      */
/* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                 */
/**************************************************************************/

package com.godot.game;

import org.godotengine.godot.GodotActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Template activity for Godot Android builds.
 * Feel free to extend and modify this class for your custom logic.
 */
public class GodotApp extends GodotActivity {
    private DynamicNfcPlugin dynamicNfcPlugin;

    private NfcAdapter nfcAdapter;

    private final String LogTag = "GodotCustom";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.GodotAppMainTheme);
        super.onCreate(savedInstanceState);

        dynamicNfcPlugin = new DynamicNfcPlugin(this.getGodot());
        dynamicNfcPlugin.onRegisterPluginWithGodotNative();
        dynamicNfcPlugin.onGodotSetupCompleted();
    }

    @Override
    protected void onResume() {
        super.onResume();

        nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

        Log.v(LogTag, "nfc enabled? " + (nfcAdapter.isEnabled()));
        if (nfcAdapter.isEnabled()) {
            Intent intent = new Intent(getActivity(), getActivity().getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(), 0, intent, PendingIntent.FLAG_MUTABLE);
            //IntentFilter[] intentFilter = new IntentFilter[]{};

            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.v(LogTag, intent.getAction() != null ? intent.getAction() : "null");
        super.onNewIntent(intent);

        setIntent(intent);

        NdefMessage[] messages = readNdefMsgsFromIntent(intent);
        String messageText = "";
        if (messages.length > 0) {
            messageText = decodeNdefMsg(messages[0]);
        }

        dynamicNfcPlugin.emitNfcTagRead(messageText);
    }

    private NdefMessage[] readNdefMsgsFromIntent(Intent intent) {
        String action = intent.getAction();
        Log.v(LogTag, action != null ? action : "action is null");

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
                /*|| NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)*/
        ) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            return msgs;
        }

        return new NdefMessage[0];
    }

    private String decodeNdefMsg(NdefMessage message) {
        String text = "";
        byte[] payload = message.getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 2 << 6) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLength = payload[0] & 0063; // Language code, e.g. "en"

        try {
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }

        return text;
    }

    /**
     * Adopted from https://stackoverflow.com/questions/64920307/how-to-write-ndef-records-to-nfc-tag
     *
     * @param text
     * @param tag
     * @throws IOException
     * @throws FormatException
     */
    private void writeNdefMsg(String text, Tag tag) throws IOException, FormatException {

        Ndef mNdef = Ndef.get(tag);

        if (mNdef != null) {

            NdefMessage mNdefMessage = mNdef.getCachedNdefMessage();
            Log.d(LogTag, "overwrite: " + mNdefMessage.toString());
            mNdefMessage.getRecords();

            NdefRecord mRecord = NdefRecord.createTextRecord("en", text);
            NdefMessage mMsg = new NdefMessage(mRecord);

            try {
                mNdef.connect();
                mNdef.writeNdefMessage(mMsg);

                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(),
                            "Write to NFC Success",
                            Toast.LENGTH_SHORT).show();
                });

				/*
				// Make a Sound
				try {
					Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
					Ringtone r = RingtoneManager.getRingtone(getApplicationContext(),
							notification);
					r.play();
				} catch (Exception e) {
					// Some error playing sound
				}
				*/

            } catch (FormatException | SecurityException | IOException e) {
                Log.e(LogTag, e.toString());
            } finally {
                try {
                    mNdef.close();
                } catch (IOException e) {
                    Log.e(LogTag, e.toString());
                }
            }

        }
    }
}
