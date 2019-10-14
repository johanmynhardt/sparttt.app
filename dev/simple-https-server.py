# taken from http://www.piware.de/2011/01/creating-an-https-server-in-python/
# generate server.xml with the following command:
#    openssl req -new -x509 -keyout server.pem -out server.pem -days 365 -nodes
# run as follows:
#    python simple-https-server.py
# then in your browser, visit:
#    https://localhost:4443

import BaseHTTPServer, SimpleHTTPServer
import ssl
import os

#server_ip = '192.168.11.59'
#server_ip = '192.168.8.102'
server_ip = os.getenv('SERVER_IP', '192.168.8.101')
server_port = int(os.getenv('SERVER_PORT', 4443))

print("Using server address from ENV: SERVER_IP, SERVER_PORT: %s:%s" % (server_ip, server_port))

httpd = BaseHTTPServer.HTTPServer((server_ip, server_port), SimpleHTTPServer.SimpleHTTPRequestHandler)
httpd.socket = ssl.wrap_socket (httpd.socket, certfile='./dev-key.pem', server_side=True)
httpd.serve_forever()
