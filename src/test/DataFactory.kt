import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.http.content.*
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun ieeeCdfNetwork14CgmesFile(): File {
    val file = File.createTempFile("network_cgmes", ".zip")
    file.deleteOnExit()

    IeeeCdfNetworkFactory.create14().write("CGMES", null, Paths.get(file.path.replace(".zip", "")))

    // Read the produced files in EQ/SSH/SV/TP and create one zip archive
    val withOutExtension = file.path.toString().replace(".zip", "")
    val cimXmlFiles = listOf("EQ", "TP", "SV", "SSH").map { profile -> "${withOutExtension}_${profile}.xml" }

    val fileOutputStream = FileOutputStream(file)
    val zipOutputStream = ZipOutputStream(fileOutputStream)


    cimXmlFiles.forEach { cimXmlFile -> addToArchiveAndDeleteFile(cimXmlFile, zipOutputStream) }
    zipOutputStream.close()
    return file
}

fun addToArchiveAndDeleteFile(filename: String, outStream: ZipOutputStream) {
    val pattern = Regex(pattern = """([^/]+$)""")  // Extract everything after the last slash
    val match = pattern.find(filename)!!
    val baseName = match.groupValues[1]
    val zipEntry = ZipEntry(baseName)
    outStream.putNextEntry(zipEntry)
    val cimFile = File(filename)
    val bytes = cimFile.readBytes()
    outStream.write(bytes, 0, bytes.size)
    outStream.closeEntry()
    cimFile.delete()
}

fun formDataMinimalNetworkRawx(): List<PartData> {
    return formData {
        append(
            "network",
            minimalRawx(),
            Headers.build {
                append(HttpHeaders.ContentDisposition, "filename=network.rawx")
            }
        )
    }
}

fun ieeeCdfNetwork14File(): File {
    // Initialize temporary file
    val file = File.createTempFile("network", ".xiidm")
    file.deleteOnExit()

    IeeeCdfNetworkFactory.create14().write("XIIDM", Properties(), Paths.get(file.path))
    return file
}

fun minimalRawx(): ByteArray {
    return ("{\"network\":{\"caseid\":{" +
            "\"fields\":[\"ic\",\"sbase\",\"rev\",\"xfrrat\",\"nxfrat\",\"basfrq\",\"title1\"]," +
            "\"data\":[0,100.00,35,0,0,60.00,\"PSS(R)EMinimumRAWXCase\"]}," +
            "\"bus\":{\"fields\":[\"ibus\",\"name\",\"baskv\",\"ide\"]," +
            "\"data\":[[1,\"Slack-Bus\",138.0,3],[2,\"Load-Bus\",138.01]]}," +
            "\"load\":{\"fields\":[\"ibus\",\"loadid\",\"stat\",\"pl\",\"ql\"]," +
            "\"data\":[[2,\"1\",1,40.0,15.0]]}," +
            "\"generator\":{\"fields\":[\"ibus\",\"machid\",\"pg\",\"qg\"]," +
            "\"data\":[[1,\"1\",\"40.35\",\"10.87\"]]}," +
            "\"acline\":{\"fields\":[\"ibus\",\"jbus\",\"ckt\",\"rpu\",\"xpu\",\"bpu\"]," +
            "\"data\":[[1,2,\"1\",0.01938,0.05917,0.05280]]}}}").toByteArray()
}

fun formDataWithEmptyNetwork(): List<PartData> {
    return formData {
        append(
            "network",
            byteArrayOf(),
            Headers.build {
                append(HttpHeaders.ContentDisposition, "filename=emptyFile.xiidm")
            }
        )
    }
}

fun formDataFromFile(file: File): List<PartData> {
    return formData {
        append(
            "network",
            file.readBytes(),
            Headers.build {
                append(HttpHeaders.ContentDisposition, "filename=${file.name}")
            },
        )
    }
}