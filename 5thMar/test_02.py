import socket
import string
import time
import threading
import thread
import os
import sys
import queue

# Dummy client code

class Test(threading.Thread):
        '''WIFI_IP = "192.168.13.1"
        WIFI_PORT = 5182
        def __init__(self,host=WIFI_IP,port=WIFI_PORT):

                self.host = host
                self.port = port
                self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                self.socket.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
                print "Socket Established"

                self.socket.listen(3);
                print "Waiting for connection from PC..."

                self.client_sock, self.address = self.socket.accept()
                print "Connected to PC @ ", self.address, "!"

        		self.ip = "192.168.13.1" # Connecting to IP address of MDPGrp13
        		self.port = 5182
        		# message = "Hello World!"
        		# message = list(string.ascii_lowercase)


        		# Create a TCP/IP socket
        		self.client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        		self.client_socket.connect((self.ip, self.port))'''
        def __init__(self):
    		threading.Thread.__init__(self)
    		self.ip = "192.168.13.1" # Connecting to IP address of MDPGrp13
    		self.port = 5182
    		# message = "Hello World!"
    		# message = list(string.ascii_lowercase)


    		# Create a TCP/IP socket
    		self.client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    		self.client_socket.connect((self.ip, self.port))


        def write(self):

                try:
                        while True:
                            if not writeQueue.empty():
                                msg = raw_input("Enter your message:")
                                self.client_sock.sendto(msg, self.address)
                                print "Message sent to PC: " + msg
                except socket.error,e:
                        if isinstance(e.args, tuple):
                                print "errno is %d" %e[0]
                                if e[0] == errno.EPIPE:
                                        #remote peer disconnected
                                        print "Detected remote disconnect"

                        else:
                                print"socket error ",e
                        sys.exit()
                '''except IOError, e:
                        print "PC read exception",e
                        print traceback.format_exc()
                        pass

'''
        def read(self):
                try:
                        while True:
                            msg = self.client_sock.recv(1024)
                            print "Message received: %s" %(msg)
                except socket.error,e:
                        if isinstance(e.args, tuple):
                                print "errno is %d" %e[0]
                                if e[0] == errno.EPIPE:
                                        #remote peer disconnected
                                        print "Detected remote disconnect"
                                else:
                                        #for another error
                                        pass
                        else:
                                print"socket error ",e
                        sys.exit()



        def create_threads(self):

                # Create read and write threads for PC
                read_tr = threading.Thread(target = self.read, args = (), name = "read_tr")
                write_tr = threading.Thread(target = self.write, args = (), name = "write_tr")


                # Set threads as Daemons
                read_tr.daemon = True
                write_tr.daemon = True

                # Start Threads
                read_tr.start()
                write_tr.start()
try:
        while True:
                test = Test()
                test.create_threads()

except KeyboardInterrupt:
        print "Terminating the program now..."
        sys.exit()
