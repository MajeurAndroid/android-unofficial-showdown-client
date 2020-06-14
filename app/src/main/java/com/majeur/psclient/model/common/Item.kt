package com.majeur.psclient.model.common

class Item {

    lateinit var id: String
    lateinit var name: String
    lateinit var description: String
    var spriteId = 0

    override fun toString() = name
}