package com.github.statnett.loadflowservice

import com.powsybl.commons.datasource.DataSourceUtil
import com.powsybl.commons.datasource.ReadOnlyMemDataSource
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

class FileContent(val name: String, val bytes: ByteArray) {
    fun contentHash(): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(this.bytes).joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
    }

    fun contentAsStream(): ByteArrayInputStream {
        return ByteArrayInputStream(this.bytes)
    }

    fun asReadOnlyMemDataSource(): ReadOnlyMemDataSource {
        if (name.endsWith(".zip")) {
            return zippedArchiveReadOnlyMemDataSource()
        }
        return singleFileReadOnlyMemDataSource()
    }

    private fun singleFileReadOnlyMemDataSource(): ReadOnlyMemDataSource {
        val dataSource = ReadOnlyMemDataSource(DataSourceUtil.getBaseName(name))
        dataSource.putData(name, bytes)
        return dataSource
    }

    private fun zippedArchiveReadOnlyMemDataSource(): ReadOnlyMemDataSource {
        val dataSource = ReadOnlyMemDataSource(DataSourceUtil.getBaseName((name)))
        ZipInputStream(contentAsStream())
            .use { stream ->
                generateSequence { stream.nextEntry }
                    .filterNot { entry -> entry.isDirectory }
                    .forEach { entry ->
                        dataSource.putData(entry.name, stream.readAllBytes())
                    }
            }
        return dataSource
    }
}
