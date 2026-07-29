package jp.oist.abcvlib.basicqrtransmitter

import android.os.Bundle
import android.widget.TextView
import jp.oist.abcvlib.core.AbcvlibActivity
import jp.oist.abcvlib.core.inputs.PublisherManager
import jp.oist.abcvlib.core.inputs.phone.QRCodeData
import jp.oist.abcvlib.core.inputs.phone.QRCodeDataSubscriber
import jp.oist.abcvlib.util.Logger
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory
import jp.oist.abcvlib.util.QRCode
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException
import jp.oist.abcvlib.util.SerialCommManager
import jp.oist.abcvlib.util.SerialReadyListener
import jp.oist.abcvlib.util.UsbSerial
import java.util.concurrent.TimeUnit

/**
 * Android application showing connection to the robot hardware, wheels, and Android sensors
 * Initializes socket connection with external python server
 * Runs PID controller locally on Android, but takes PID parameters from python GUI
 * @author Christopher Buckley https://github.com/topherbuckley
 */
class MainActivity : AbcvlibActivity(), SerialReadyListener, QRCodeDataSubscriber {
    private lateinit var qrCode: QRCode
    private lateinit var publisherManager: PublisherManager
    private lateinit var emojiTextView: TextView

    private var speedL = 0f
    private var speedR = 0f
    private val speed = 0.4f

    private enum class ACTIONS { FORWARD_1, BACKWARD_1, TURN_RIGHT, FORWARD_2, BACKWARD_2, TURN_LEFT }
    private var action = ACTIONS.FORWARD_1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        qrCode = QRCode(supportFragmentManager, R.id.qrFragmentView)

        emojiTextView = findViewById(R.id.emojiTextView)
        emojiTextView.text = "😊"
    }

    private val swapAction: Runnable = Runnable {
        when (action) {
            ACTIONS.FORWARD_1 -> {
                action = ACTIONS.BACKWARD_1
                goForward()
                updateEmoji("😁")
            }
            ACTIONS.BACKWARD_1 -> {
                action = ACTIONS.TURN_RIGHT
                goBackward()
                updateEmoji("😅")
            }
            ACTIONS.TURN_RIGHT -> {
                action = ACTIONS.FORWARD_2
                turnRight()
                updateEmoji("😎")
            }
            ACTIONS.FORWARD_2 -> {
                action = ACTIONS.BACKWARD_2
                goForward()
                updateEmoji("😁")
            }
            ACTIONS.BACKWARD_2 -> {
                action = ACTIONS.TURN_LEFT
                goBackward()
                updateEmoji("😅")
            }
            ACTIONS.TURN_LEFT -> {
                action = ACTIONS.FORWARD_1
                turnLeft()
                updateEmoji("🤪")
            }
        }
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
        val executor = ScheduledExecutorServiceWithException(1, ProcessPriorityThreadFactory(Thread.MIN_PRIORITY, "ActionSelector"))
        executor.scheduleAtFixedRate(swapAction, 0, 5, TimeUnit.SECONDS)
    }

    override fun onQRCodeDetected(qrDataDecoded: String) {
        if (qrDataDecoded.isNotEmpty()) {
            Logger.i("qrcode", "QR Code Found and decoded: $qrDataDecoded")
        }
    }

    override fun abcvlibMainLoop() {
        outputs.setWheelOutput(speedL, speedR, false, false)
    }

    private fun goForward() {
        speedL = speed
        speedR = speed
    }

    private fun goBackward() {
        speedL = -speed
        speedR = -speed
    }

    private fun turnRight() {
        speedL = -speed
        speedR = speed
    }

    private fun turnLeft() {
        speedL = speed
        speedR = -speed
    }

    private fun updateEmoji(emoji: String) {
        qrCode.close()
        qrCode.generate(emoji)
        runOnUiThread {
            emojiTextView.text = emoji
        }
    }
}