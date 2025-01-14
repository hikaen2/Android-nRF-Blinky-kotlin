/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.blinky.viewmodels

import android.os.ParcelUuid
import androidx.lifecycle.LiveData
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice
import no.nordicsemi.android.blinky.profile.BlinkyManager
import no.nordicsemi.android.support.v18.scanner.ScanResult
import java.util.*

/**
 * This class keeps the current list of discovered Bluetooth LE devices matching filter.
 * Each time @{link [.applyFilter] is called, the observers are notified with a new
 * list instance.
 */
class DevicesLiveData internal constructor(private var mFilterUuidRequired: Boolean, private var mFilterNearbyOnly: Boolean) :
    LiveData<List<DiscoveredBluetoothDevice>>() {

    private val mDevices = ArrayList<DiscoveredBluetoothDevice>()
    private var mFilteredDevices: List<DiscoveredBluetoothDevice>? = null

    @Synchronized
    internal fun bluetoothDisabled() {
        mDevices.clear()
        mFilteredDevices = null
        postValue(null)
    }

    internal fun filterByUuid(uuidRequired: Boolean): Boolean {
        mFilterUuidRequired = uuidRequired
        return applyFilter()
    }

    internal fun filterByDistance(nearbyOnly: Boolean): Boolean {
        mFilterNearbyOnly = nearbyOnly
        return applyFilter()
    }

    @Synchronized
    internal fun deviceDiscovered(result: ScanResult): Boolean {

        // Check if it's a new device.
        var device : DiscoveredBluetoothDevice? = mDevices.find{ it.matches(result) }
        if (device == null) {
            device = DiscoveredBluetoothDevice(result)
            mDevices.add(device)
        }

        // Update RSSI and name.
        device.update(result)

        // Return true if the device was on the filtered list or is to be added.
        return mFilteredDevices?.contains(device) ?: false || matchesUuidFilter(result) && matchesNearbyFilter(device.highestRssi)
    }

    /**
     * Clears the list of devices.
     */
    @Synchronized
    fun clear() {
        mDevices.clear()
        mFilteredDevices = null
        postValue(null)
    }

    /**
     * Refreshes the filtered device list based on the filter flags.
     */
    @Synchronized
    internal fun applyFilter(): Boolean {
        val devices = ArrayList<DiscoveredBluetoothDevice>()
        for (device in mDevices) {
            if (matchesUuidFilter(device.scanResult) && matchesNearbyFilter(device.highestRssi)) {
                devices.add(device)
            }
        }
        mFilteredDevices = devices
        postValue(mFilteredDevices)
        return mFilteredDevices!!.isNotEmpty()
    }

    private fun matchesUuidFilter(result: ScanResult?): Boolean {
        if (!mFilterUuidRequired) {
            return true
        }
        return result?.scanRecord?.serviceUuids?.contains(FILTER_UUID) ?: false
    }

    private fun matchesNearbyFilter(rssi: Int): Boolean {
        if (!mFilterNearbyOnly) {
            return true
        }
        return rssi >= FILTER_RSSI
    }

    companion object {
        private val FILTER_UUID = ParcelUuid(BlinkyManager.LBS_UUID_SERVICE)
        private const val FILTER_RSSI = -50 // [dBm]
    }
}
