package com.oboenikui.campusfelica

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.NfcF
import android.os.Bundle
import android.os.Handler
import android.support.v13.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.concurrent.thread
import com.oboenikui.campusfelica.ExecuteNfcF as ex


class ScannerActivity : AppCompatActivity() {
    lateinit var adapter: NfcAdapter
    private var filters: Array<IntentFilter>? = null
    private var techLists: Array<Array<String>>? = null
    private var pendingIntent: PendingIntent? = null
    private val REQUEST_WRITE_STORAGE = 112
    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        adapter = NfcAdapter.getDefaultAdapter(this)
        val tech = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        tech.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        filters = arrayOf(tech)

        pendingIntent = PendingIntent.getActivity(this, 0, Intent(this,
                javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
        techLists = arrayOf(arrayOf<String>(NfcF::class.java.name))
        val hasPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        if (!hasPermission) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_STORAGE)
        }
    }

    public override fun onResume() {
        adapter.enableForegroundDispatch(
                this, pendingIntent, filters, techLists)
        super.onResume()
    }

    public override fun onPause() {
        if (this.isFinishing) {
            adapter.disableForegroundDispatch(this)
        }
        super.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_WRITE_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, "The app was not allowed to write to your storage. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show()
                }
            }
        }

    }

    override fun onNewIntent(intent: Intent) {
        val action = intent.action
        if (action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            val handler = Handler()
            val textView = findViewById(R.id.scan_results) as TextView
            thread {

                val nfcF = NfcF.get(intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG))
                nfcF.connect()

                val idm = try {
                    Arrays.copyOfRange(nfcF.transceive(ex.POLLING_COMMAND), 2, 10)
                } catch (e: TagLostException) {
                    return@thread
                }

                val stream = ByteArrayOutputStream()
                println(nfcF.maxTransceiveLength)
                val service = ex.createService(CampusFeliCa.SERVICE_CODE_MEMBER_INFORMATION, CampusFeliCa.SERVICE_CODE_MONEY_INFORMATION)
                val block = ex.createBlock(3, 1)
                stream.write(2 + idm.size + service.size + block.size)
                stream.write(6)
                stream.write(idm)
                stream.write(service)
                stream.write(block)
                val array = stream.toByteArray()
                stream.close()
                val result = ex.bytesToText(nfcF.transceive(array))
                handler.post {
                    textView.text = result
                }
                /*for (i in index..65535) {
                    val stream = ByteArrayOutputStream()
                    if (i % 100 == 0) {
                        println(i)
                    }
                    val blockList = ex.createBlock(10)
                    stream.write(2 + idm.size + 3 + blockList.size)
                    stream.write(6)
                    stream.write(idm)

                    stream.write(1)
                    stream.write(ByteBuffer.allocate(2).putChar(i.toChar()).array())
                    stream.write(blockList)
                    try {
                        val response = nfcF.transceive(stream.toByteArray())

                        if (i == 0) {
                            first = response
                        } else {
                            handler.post {
                                file.appendText("$i:${ex.bytesToText(response ?: byteArrayOf())}\n")
                            }
                        }
                        index = i
                    } catch (e: TagLostException) {
                        e.printStackTrace()
                        System.err.println(i)
                        break
                    } finally {
                        stream.close()
                    }
                }*/
                nfcF.close()
            }
        }
    }
}
