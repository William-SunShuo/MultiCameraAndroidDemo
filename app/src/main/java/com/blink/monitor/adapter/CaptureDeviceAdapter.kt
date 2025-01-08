package com.blink.monitor.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blink.monitor.R
import com.blink.monitor.bean.PeerDeviceItem
import com.blink.monitor.databinding.ItemCaptureDeviceBinding
import com.blink.monitor.extention.onClick


class CaptureDeviceAdapter: RecyclerView.Adapter<DeviceViewHolder>() {

    private var data: MutableList<PeerDeviceItem> = mutableListOf()

    var connectAction: ((PeerDeviceItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        return DeviceViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_capture_device, parent, false))
    }

    fun setData(dat: List<PeerDeviceItem>) {
        data.clear()
        data.addAll(dat)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
       return  data.size
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.setData(data[position], position)
        holder.itemView.onClick {
            data.forEachIndexed { index, _ ->
                data[index].connectedState = (index == position)
            }
            notifyDataSetChanged()
            it.post {
                connectAction?.invoke(data[position])
            }
        }
        holder.binding.bottomLine.visibility = if(position != data.size - 1) View.VISIBLE else View.GONE
    }
}


class DeviceViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    var binding = ItemCaptureDeviceBinding.bind(itemView)

    fun setData(captureItem: PeerDeviceItem, position: Int) {
        binding.tvDeviceName.text = captureItem.deviceName
        binding.actionCap.isSelected = captureItem.connectedState

    }

}


