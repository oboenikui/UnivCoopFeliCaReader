package com.oboenikui.campusfelica

import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


/**
 * Created by Takaki Hoshikawa on 2016/11/20.
 */
class NavDrawerAdapter(private val mDrawerMenuArr: Array<String>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ViewHolder(itemView: View, viewType: Int) : RecyclerView.ViewHolder(itemView) {
        var mTextView: TextView

        init {
            mTextView = itemView.findViewById(android.R.id.text1) as TextView
        }
    }

    class ToggleViewHolder(itemView: View, viewType: Int) : RecyclerView.ViewHolder(itemView) {
        var mTextView: TextView

        init {
            mTextView = itemView.findViewById(android.R.id.text1) as TextView
        }
    }

    override fun getItemViewType(position: Int): Int {
        return super.getItemViewType(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val itemView = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)

        itemView.isClickable = true

        val outValue = TypedValue()
        parent.context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        itemView.setBackgroundResource(outValue.resourceId)

        val vh = ViewHolder(itemView, viewType)
        return vh
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder) {
            is NavDrawerAdapter.ViewHolder -> {
                val menu = mDrawerMenuArr[position]
                holder.mTextView.text = menu
                holder.itemView.setOnClickListener {  }
            }


        }
    }

    override fun getItemCount(): Int {
        return mDrawerMenuArr.size
    }
}