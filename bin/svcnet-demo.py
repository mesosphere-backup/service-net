#!/usr/bin/env python
import re
import socket
import sys

from BaseHTTPServer import HTTPServer
from SimpleHTTPServer import SimpleHTTPRequestHandler

class FileSender(SimpleHTTPRequestHandler):
    def do_GET(self):
        return SimpleHTTPRequestHandler.do_GET(self)
 
class HTTPServer6(HTTPServer):
    address_family = socket.AF_INET6
 
def web(ip='::', port=8080):
    server = HTTPServer6((ip, port), FileSender)
    server.serve_forever()
 
def cli():
    p = argparse.ArgumentParser(prog='svcnet-demo.py')
    p.add_argument('--web',
                   help='Starts file server on [IPv6](:port)?')
    parsed = p.parse_args()

    if parsed.web:
        m = re.match(r'\[([0-9a-fA-F:]+)\](:([0-9]+))?', parsed.http)
        if m:
            ip, _, port = m.groups()
            if port:
                web(ip, int(port))
            else:
                web(ip)

if __name__ == '__main__':
    main()
