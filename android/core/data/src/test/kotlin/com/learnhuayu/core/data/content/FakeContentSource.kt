package com.learnhuayu.core.data.content

import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream

internal class FakeContentSource(
    private val files: Map<String, String>,
) : ContentSource {
    override fun open(path: String): InputStream {
        val text = files[path] ?: throw FileNotFoundException(path)
        return ByteArrayInputStream(text.toByteArray())
    }
}
