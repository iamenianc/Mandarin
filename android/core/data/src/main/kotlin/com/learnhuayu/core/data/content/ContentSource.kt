package com.learnhuayu.core.data.content

import java.io.InputStream

interface ContentSource {
    fun open(path: String): InputStream

    fun readText(path: String): String = open(path).bufferedReader().use { reader -> reader.readText() }
}
