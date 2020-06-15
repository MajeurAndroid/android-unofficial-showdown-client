package com.majeur.psclient.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ChatRoomInfo(val name: String, val description: String, val userCount: Int) : Parcelable