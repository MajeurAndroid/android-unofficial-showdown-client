package com.majeur.psclient.util.recyclerview

import android.view.View
import androidx.recyclerview.widget.RecyclerView

interface OnItemClickListener {
    fun onItemClick(itemView: View, holder: RecyclerView.ViewHolder, position: Int)
}