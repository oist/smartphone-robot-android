package jp.oist.abcvlib.basicqrreceiver

import android.os.Bundle
import android.widget.TextView
import jp.oist.abcvlib.core.AbcvlibActivity
import jp.oist.abcvlib.core.inputs.PublisherManager
import jp.oist.abcvlib.core.inputs.phone.QRCodeData
import jp.oist.abcvlib.core.inputs.phone.QRCodeDataSubscriber
import jp.oist.abcvlib.util.SerialCommManager
import jp.oist.abcvlib.util.SerialReadyListener
import jp.oist.abcvlib.util.UsbSerial

/**
 * Android application showing connection to the robot hardware, wheels, and Android sensors
 * Initializes socket connection with external python server
 * Runs PID controller locally on Android, but takes PID parameters from python GUI
 * @author Christopher Buckley https://github.com/topherbuckley
 */
class MainActivity : AbcvlibActivity(), SerialReadyListener, QRCodeDataSubscriber {
    private lateinit var publisherManager: PublisherManager
    private lateinit var emojiTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        emojiTextView = findViewById(R.id.emojiTextView)
    }

    override fun onSerialReady(usbSerial: UsbSerial) {
        publisherManager = PublisherManager()

        val qrCodeData = QRCodeData.Builder(this, publisherManager, this).build()
        qrCodeData.addSubscriber(this)

        publisherManager.initializePublishers()
        publisherManager.startPublishers()

        setSerialCommManager(SerialCommManager(usbSerial))
        super.onSerialReady(usbSerial)
    }

    public override fun onOutputsReady() {
        publisherManager.initializePublishers()
        publisherManager.startPublishers()
    }

    override fun onQRCodeDetected(qrDataDecoded: String) {
        updateEmoji(qrDataDecoded)
    }

    private fun updateEmoji(emoji: String) {
        runOnUiThread {
            emojiTextView.text = emoji
        }
    }

    // don't move
    override fun abcvlibMainLoop() {}

}
