package com.oboenikui.campusfelica

import android.nfc.Tag
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.util.*

class CampusFeliCa(private val mTag: Tag) {

    fun readHistories(ex: ExecuteNfcF): List<CampusFeliCaHistory> {
        val stream = ByteArrayOutputStream()
        stream.write(1)
        try {
            stream.write(SERVICE_CODE_HISTORY)
            stream.write(ExecuteNfcF.createBlockList(10))
            return toCampusFeliCaHistories(ex.executeWithIdm(6, stream.toByteArray()))
        } catch (e: IOException) {
            return mutableListOf<CampusFeliCaHistory>()
        }
    }

    fun readBasicInformation(ex: ExecuteNfcF): CampusFeliCaInformation? {
        val stream = ByteArrayOutputStream()
        stream.write(2)
        try {
            stream.write(SERVICE_CODE_INFORMATION)
            stream.write(SERVICE_CODE_BALANCE)
            stream.write(ExecuteNfcF.createBlockList(3, 1))
            return toCampusFeliCaInformation(ex.executeWithIdm(6, stream.toByteArray()))
        } catch (e: IOException) {
            return null
        }
    }

    fun readData(): Pair<CampusFeliCaInformation, List<CampusFeliCaHistory>>? {
        try {
            ExecuteNfcF(mTag).use {
                it.connect()
                val info = readBasicInformation(it) ?: return null
                val histories = readHistories(it)
                return info to histories
            }
        } catch(e: IOException) {
            return null
        }
    }

    private fun toCampusFeliCaInformation(result: ByteArray?): CampusFeliCaInformation? {
        if(result == null || result.size ?: 0 != 0x4D || result[12] != 4.toByte()) {
            return null
        }
        val cal = Calendar.getInstance()
        cal.set(toInt((result[30].toInt()+0x20).toByte(), result[31]),
                toInt(result[32]) - 1,
                toInt(result[33]))
        return CampusFeliCaInformation(toIdString(result.copyOfRange(13,19)),
                result[19] == 4.toByte(),
                cal,
                toInt(result[34], result[35], result[36]),
                BigInteger(1, byteArrayOf(result[45], result[46], result[47], result[48])).toLong()/10.0,
                BigInteger(1, byteArrayOf(result[64], result[63], result[62], result[61])).toLong())
    }

    private fun toCampusFeliCaHistories(result: ByteArray?): List<CampusFeliCaHistory> {

        val list = mutableListOf<CampusFeliCa.CampusFeliCaHistory>()
        if (result == null || result[0] == 0x0C.toByte()) {
            return list
        }
        var i: Int
        val count = result[12].toInt()
        i = 0
        while (i < count) {
            val cal = Calendar.getInstance()
            cal.set(toInt(result[13 + i * 16], result[14 + i * 16]),
                    toInt(result[15 + i * 16]) - 1,
                    toInt(result[16 + i * 16]),
                    toInt(result[17 + i * 16]),
                    toInt(result[18 + i * 16]),
                    toInt(result[19 + i * 16]))
            list.add(CampusFeliCaHistory(cal, result[20 + i * 16].toInt() == 0x5, toInt(result[21 + i * 16], result[22 + i * 16], result[23 + i * 16]), toInt(result[24 + i * 16], result[25 + i * 16], result[26 + i * 16])))
            i++
        }
        return list
    }

    inner class CampusFeliCaHistory(val calendar: Calendar, val isPayment: Boolean, val price: Int, val balance: Int)

    inner class CampusFeliCaInformation(val coopId: String, val isMemberId: Boolean, val lastMealDate: Calendar, val mealBalance: Int, val point: Double, val balance: Long)

    companion object {
        val SERVICE_CODE_HISTORY = byteArrayOf(0xcf.toByte(), 0x50.toByte())
        val SERVICE_CODE_UNKNOWN1 = byteArrayOf(0x4b.toByte(), 0x43.toByte())
        val SERVICE_CODE_UNKNOWN2 = byteArrayOf(0x8b.toByte(), 0x1a.toByte())
        val SERVICE_CODE_INFORMATION = byteArrayOf(0xcb.toByte(), 0x50.toByte())
        val SERVICE_CODE_BALANCE = byteArrayOf(0xd7.toByte(), 0x50.toByte())

        private fun toInt(vararg data: Byte): Int {
            var text = ""
            for (b in data) {
                val hexString = String.format("%02X", b)
                text += hexString
            }
            return Integer.parseInt(text)
        }

        private fun toIdString(data: ByteArray): String {
            var text = ""
            for ((i, b) in data.withIndex()) {
                val hexString = String.format("%02X", b)
                text += hexString
                if(i % 2 == 1 && i !== 5) {
                    text += " "
                }
            }
            return text
        }
    }
}
