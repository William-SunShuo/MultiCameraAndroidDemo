package com.blink.monitor.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.blink.monitor.R
import com.blink.monitor.bean.PeerDeviceItem
import com.blink.monitor.databinding.ItemCaptureDeviceBinding
import com.blink.monitor.extention.onClick

class CaptureDeviceListAdapter(var connectAction: ((PeerDeviceItem, Int) -> Unit)? = null):
    ListAdapter<PeerDeviceItem, DeviceHolder>(object : DiffUtil.ItemCallback<PeerDeviceItem>() {

    override fun areItemsTheSame(oldItem: PeerDeviceItem, newItem: PeerDeviceItem): Boolean {
        return oldItem.deviceName == newItem.deviceName
                && oldItem.deviceType == newItem.deviceType
    }

    override fun areContentsTheSame(oldItem: PeerDeviceItem, newItem: PeerDeviceItem): Boolean {
       return oldItem.deviceName == newItem.deviceName
               && oldItem.deviceType == newItem.deviceType
               && oldItem.ipAddress == newItem.ipAddress
    }

}) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceHolder {
        return DeviceHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_capture_device, parent, false))
    }

    override fun onBindViewHolder(holder: DeviceHolder, position: Int) {
        holder.setData(currentList[position], position)
        holder.itemView.onClick {
            if(position < itemCount ) {
                connectAction?.invoke(currentList[position], position)
            }
        }
        holder.binding.bottomLine.visibility = if(position != itemCount - 1)
            View.VISIBLE else View.GONE
    }
}


class DeviceHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    var binding = ItemCaptureDeviceBinding.bind(itemView)

    fun setData(captureItem: PeerDeviceItem, position: Int) {
        binding.tvDeviceName.text = captureItem.deviceName
        binding.actionCap.isSelected = captureItem.connectedState
    }

}