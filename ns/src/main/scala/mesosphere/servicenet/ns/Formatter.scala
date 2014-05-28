package mesosphere.servicenet.ns

import com.github.mkroli.dns4s
import com.github.mkroli.dns4s.dsl._

object Formatter {
  def format(query: dns4s.Message): String = query match { ////////////// Sucks
    case Query(_) ~ Questions(QName(s) ~ TypeNS() :: Nil)    => s"$s NS"
    case Query(_) ~ Questions(QName(s) ~ TypeA() :: Nil)     => s"$s A"
    case Query(_) ~ Questions(QName(s) ~ TypeMD() :: Nil)    => s"$s MD"
    case Query(_) ~ Questions(QName(s) ~ TypeMF() :: Nil)    => s"$s MF"
    case Query(_) ~ Questions(QName(s) ~ TypeCNAME() :: Nil) => s"$s CNAME"
    case Query(_) ~ Questions(QName(s) ~ TypeSOA() :: Nil)   => s"$s SOA"
    case Query(_) ~ Questions(QName(s) ~ TypeMB() :: Nil)    => s"$s MB"
    case Query(_) ~ Questions(QName(s) ~ TypeMG() :: Nil)    => s"$s MG"
    case Query(_) ~ Questions(QName(s) ~ TypeMR() :: Nil)    => s"$s MR"
    case Query(_) ~ Questions(QName(s) ~ TypeNULL() :: Nil)  => s"$s NULL"
    case Query(_) ~ Questions(QName(s) ~ TypeWKS() :: Nil)   => s"$s WKS"
    case Query(_) ~ Questions(QName(s) ~ TypePTR() :: Nil)   => s"$s PTR"
    case Query(_) ~ Questions(QName(s) ~ TypeHINFO() :: Nil) => s"$s HINFO"
    case Query(_) ~ Questions(QName(s) ~ TypeMINFO() :: Nil) => s"$s MINFO"
    case Query(_) ~ Questions(QName(s) ~ TypeMX() :: Nil)    => s"$s MX"
    case Query(_) ~ Questions(QName(s) ~ TypeTXT() :: Nil)   => s"$s TXT"
    case Query(_) ~ Questions(QName(s) ~ TypeAAAA() :: Nil)  => s"$s AAAA"
    case Query(_) ~ Questions(QName(s) ~ TypeAXFR() :: Nil)  => s"$s AXFR"
    case Query(_) ~ Questions(QName(s) ~ TypeMAILB() :: Nil) => s"$s MAILB"
    case Query(_) ~ Questions(QName(s) ~ TypeMAILA() :: Nil) => s"$s MAILA"
    case Query(_) ~ Questions(QName(s) ~ TypeAsterisk() :: Nil) =>
      s"$s Asterisk"
    case _ => "[! non-question DNS message !]"
  }
}
