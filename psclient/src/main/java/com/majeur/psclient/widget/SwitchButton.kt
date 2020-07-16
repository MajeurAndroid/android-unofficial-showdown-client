package com.majeur.psclient.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.majeur.psclient.R

class SwitchButton @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var nameView: TextView
    private lateinit var iconView: ImageView

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        when (child.id) {
            R.id.name_view -> nameView = child as TextView
            R.id.dex_icon_view -> iconView = child as ImageView
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        nameView.isEnabled = enabled
        iconView.alpha = if (enabled) 1f else 0.65f
    }

    fun setDexIcon(dexIcon: Drawable?) {
        iconView.setImageDrawable(dexIcon)
    }

    fun setPokemonName(name: String?) {
        nameView.text = name
    }
}