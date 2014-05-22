package mesosphere.servicenet.util

import java.io._

object IO {
  def read(f: File): Array[Byte] = {
    read(new FileInputStream(f))
  }

  def read(is: InputStream): Array[Byte] = {
    val os: ByteArrayOutputStream = new ByteArrayOutputStream()
    val buffer = new Array[Byte](1024)
    var len: Int = is.read(buffer, 0, 1024)
    while (len != -1) {
      os.write(buffer, 0, len)
      len = is.read(buffer, 0, 1024)
    }
    os.toByteArray
  }

  def overwrite(f: File, data: String) {
    overwrite(f, data.getBytes("UTF-8"))
  }

  def overwrite(f: File, data: Array[Byte]) {
    val ostream = new FileOutputStream(f)
    ostream.write(data)
    ostream.flush()
    ostream.close()
  }

  def replace(f: File, data: String) {
    replace(f, data.getBytes("UTF-8"))
  }

  def replace(f: File, data: Array[Byte]) {
    val tmp: File = File.createTempFile("servicenet.", ".tmp")
    overwrite(tmp, data)
    if (!tmp.renameTo(f)) throw new IOException
  }
}
