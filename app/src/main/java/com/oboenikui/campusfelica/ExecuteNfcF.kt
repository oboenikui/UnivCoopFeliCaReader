package com.oboenikui.campusfelica

import android.nfc.Tag
import android.nfc.tech.NfcF
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.util.*

class ExecuteNfcF(tag: Tag) : Closeable {
    val nfcF: NfcF
    private var idm: ByteArray? = null

    init {
        nfcF = NfcF.get(tag)
    }

    fun connect() {
        nfcF.connect()
    }

    override fun close() {
        nfcF.close()
    }

    fun execute(data: ByteArray): ByteArray? {
        try {
            return nfcF.transceive(data)
        } catch (e: IOException) {
            return null
        }
    }

    fun executeWithIdm(command: Int, data: ByteArray): ByteArray? {
        try {
            val idm = this.idm ?: Arrays.copyOfRange(nfcF.transceive(POLLING_COMMAND), 2, 10)
            if(idm == null) this.idm = idm
            val stream = ByteArrayOutputStream()
            stream.write(2 + idm.size + data.size)
            stream.write(command)
            stream.write(idm)
            stream.write(data)

            val array = stream.toByteArray()
            stream.close()
            return nfcF.transceive(array)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    companion object {

        val POLLING_COMMAND = byteArrayOf(0x06.toByte(), 0x00.toByte(), 0xFE.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0F.toByte())

        fun createService(vararg services: ByteArray): ByteArray {
            if(services.size > 0xff) {
                throw IndexOutOfBoundsException("Too many services.")
            }
            return byteArrayOf(services.size.toByte()) + services.flatMap(ByteArray::asIterable)
        }

        fun createBlock(vararg counts: Int): ByteArray {
            ByteArrayOutputStream().use {
                it.write(counts.sum())
                for (i in 0..counts.size-1) {
                    for (j in 0..counts[i]-1) {
                        it.write(0x80 + i)
                        it.write(j)
                    }
                }
                return it.toByteArray()
            }
        }

        fun bytesToText(bytes: ByteArray): String {
            val buffer = StringBuilder()
            for (b in bytes) {
                val hex = "%02X".format(b)
                buffer.append(hex).append(" ")
            }

            val text = buffer.toString().trim { it <= ' ' }
            return text
        }
    }
}
