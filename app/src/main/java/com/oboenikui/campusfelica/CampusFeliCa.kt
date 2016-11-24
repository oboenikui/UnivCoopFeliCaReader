package com.oboenikui.campusfelica

import android.nfc.Tag
import java.io.IOException
import java.math.BigInteger
import java.util.*
import com.oboenikui.campusfelica.ExecuteNfcF as exNfc

class CampusFeliCa(private val mTag: Tag) {

    fun readHistories(ex: exNfc): List<CampusFeliCaHistory> {
        try {
            return toCampusFeliCaHistories(ex.executeWithIdm(6, exNfc.createService(SERVICE_CODE_HISTORY) + exNfc.createBlock(10)))
        } catch (e: IOException) {
            return mutableListOf<CampusFeliCaHistory>()
        }
    }

    fun readBasicInformation(ex: exNfc): CampusFeliCaInformation? {
        try {
            return toCampusFeliCaInformation(ex.executeWithIdm(6, exNfc.createService(SERVICE_CODE_MEMBER_INFORMATION, SERVICE_CODE_MONEY_INFORMATION) + exNfc.createBlock(3, 1)))
        } catch (e: IOException) {
            return null
        }
    }

    fun readData(): Pair<CampusFeliCaInformation, List<CampusFeliCaHistory>>? {
        try {
            exNfc(mTag).use {
                it.connect()
                val info = readBasicInformation(it) ?: return null
                val histories = readHistories(it)
                return info to histories
            }
        } catch(e: IOException) {
            return null
        }
    }

    private fun toCampusFeliCaInformation(rawData: ByteArray?): CampusFeliCaInformation? {
        if (rawData == null || rawData.size != 0x4D || rawData[12] != 4.toByte()) {
            return null
        }
        val result = rawData.sliceArray(13..rawData.size - 1)
        val cal = Calendar.getInstance()
        cal.set(toInt(result, 16 + 1, 16 + 2) + 2000,
                toInt(result, 16 + 3) - 1,
                toInt(result, 16 + 4))
        return CampusFeliCaInformation(
                toIdString(result.sliceArray(0..5)),
                result[6] === 4.toByte(),
                cal,
                result[16].toInt() === 1,
                toInt(result, 16 + 5..16 + 7),
                BigInteger(1, byteArrayOf(result[32], result[32 + 1], result[32 + 2], result[32 + 3])).toLong() / 10.0,
                BigInteger(1, byteArrayOf(result[48 + 3], result[48 + 2], result[48 + 1], result[48])).toLong(),
                toInt(result, 48 + 13..48 + 15))
    }

    private fun toCampusFeliCaHistories(rawData: ByteArray?): List<CampusFeliCaHistory> {

        val list = mutableListOf<CampusFeliCa.CampusFeliCaHistory>()
        if (rawData == null || rawData[0] == 0x0C.toByte()) {
            return list
        }
        var i: Int
        val count = rawData[12].toInt()
        val result = rawData.sliceArray(13..rawData.size - 1)
        i = 0
        while (i < count) {
            val cal = Calendar.getInstance()
            cal.set(toInt(result, i * 16, i * 16 + 1),
                    toInt(result, i * 16 + 2) - 1,
                    toInt(result, i * 16 + 3),
                    toInt(result, i * 16 + 4),
                    toInt(result, i * 16 + 5),
                    toInt(result, i * 16 + 6))
            list.add(CampusFeliCaHistory(cal, result[7 + i * 16].toInt() == 0x5, toInt(result, i * 16 + 8..i * 16 + 10), toInt(result, i * 16 + 11..i * 16 + 13)))
            i++
        }
        return list
    }

    inner class CampusFeliCaHistory(val calendar: Calendar, val isPayment: Boolean, val price: Int, val balance: Int)

    inner class CampusFeliCaInformation(val coopId: String, val isMemberId: Boolean, val lastMealDate: Calendar, val isMealUser: Boolean, val mealUsed: Int, val point: Double, val balance: Long, val usageCount: Int)

    companion object {
        val SERVICE_CODE_HISTORY = byteArrayOf(0xcf.toByte(), 0x50.toByte())        // max: 10 blocks
        val SERVICE_CODE_UNKNOWN1 = byteArrayOf(0x4b.toByte(), 0x43.toByte())       // max:  3 blocks
        val SERVICE_CODE_UNKNOWN2 = byteArrayOf(0x8b.toByte(), 0x1a.toByte())       // max:  4 blocks
        val SERVICE_CODE_MEMBER_INFORMATION = byteArrayOf(0xcb.toByte(), 0x50.toByte())    // max:  6 blocks
        val SERVICE_CODE_MONEY_INFORMATION = byteArrayOf(0xd7.toByte(), 0x50.toByte())        // max:  1 block

        private fun toInt(data: ByteArray, range: IntRange): Int {
            var text = ""
            for (i in range) {
                val hexString = String.format("%02X", data[i])
                text += hexString
            }
            return Integer.parseInt(text)
        }

        private fun toInt(data: ByteArray, vararg indexes: Int): Int {
            var text = ""
            for (i in indexes) {
                val hexString = String.format("%02X", data[i])
                text += hexString
            }
            return Integer.parseInt(text)
        }

        private fun toIdString(data: ByteArray): String {
            var text = ""
            for ((i, b) in data.withIndex()) {
                val hexString = String.format("%02X", b)
                text += hexString
                if (i % 2 == 1 && i !== 5) {
                    text += " "
                }
            }
            return text
        }
    }
}
