package com.godot.game

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.util.Log
import android.widget.Toast
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot
import java.nio.charset.Charset

class DynamicNfcPlugin(godot: Godot) : GodotPlugin(godot), NfcAdapter.ReaderCallback {

    private val context = activity?.applicationContext;

    private var nfcAdapter: NfcAdapter? = null

    private val tagReadSignalInfo = SignalInfo("tagRead", String::class.java)

    override fun getPluginName() = "InjectedNfcTestPlugin"

    override fun getPluginSignals(): MutableSet<SignalInfo> {
        return mutableSetOf(tagReadSignalInfo)
    }

    override fun onGodotSetupCompleted() {
        super.onGodotSetupCompleted()
        if (context == null) return;

        nfcAdapter = NfcAdapter.getDefaultAdapter(activity?.applicationContext)

        /*val nfcIntentFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        val receiverFlags = Context.RECEIVER_EXPORTED;
        context.registerReceiver(nfcListener, nfcIntentFilter, receiverFlags)*/

        val isNfcSupported: Boolean = this.nfcAdapter != null
        if (!isNfcSupported) {
            Toast.makeText(context, "Nfc is not supported on this device", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (!nfcAdapter?.isEnabled!!) {
            Toast.makeText(
                context,
                "NFC disabled on this device. Turn on to proceed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @UsedByGodot
    private fun startNfc() {

        runOnUiThread {
            val msg = "START NFC; Adapter: " + (nfcAdapter != null)
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            Log.v(pluginName, msg)
        }


        nfcAdapter?.enableReaderMode(
            activity,
            this,
            0,
            //NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    @UsedByGodot
    private fun stopNfc() {
        runOnUiThread {
            val msg = "STOP NFC"
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            Log.v(pluginName, msg)
        }

        nfcAdapter?.disableReaderMode(activity)
    }

    override fun onTagDiscovered(tag: Tag?) {
        runOnUiThread {
            val msg =
                "Tag discovered! Tag:" + (tag != null) + " Name: " + tagReadSignalInfo.name
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
            Log.v(pluginName, msg)
            Log.v(pluginName, tag.toString())
        }

        if (tag == null) return

        for (tech in tag.techList) {
            Log.v(pluginName, "Tech: $tech")
        }
        val ndef = Ndef.get(tag)
        Log.v(pluginName, "--- $ndef")

        //emitSignal(tagReadSignalInfo.name, readTag(tag))
    }

    fun emitNfcTagRead(nfcText: String) {
        emitSignal(tagReadSignalInfo.name, nfcText)
    }

    private fun readTag(tag: Tag): List<String>? {
        return Ndef.get(tag)?.use { ndef ->
            ndef.connect()
            val payload = ndef.ndefMessage.records.map { record ->
                String(record.payload, Charset.forName("US-ASCII"))
            }
            payload
        }
    }
}
