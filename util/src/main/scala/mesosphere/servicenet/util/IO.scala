package mesosphere.servicenet.util

import java.io._

object IO {
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
  def overwrite(f: File, data: Array[Byte]) {
    val ostream = new FileOutputStream(f)
    ostream.write(data)
    ostream.flush()
    ostream.close()
  }
}
