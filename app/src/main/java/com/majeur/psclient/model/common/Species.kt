package com.majeur.psclient.model.common

import java.io.Serializable


class Species : Serializable {
    lateinit var id: String
    lateinit var name: String
    override fun toString() = name
}
