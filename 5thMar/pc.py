import socket
import sys

class PCObj(object):
        def __init__(self):
                # Initialize PCObj
                self.tcp_ip = "192.168.13.1"
                self.port = 5182
                self.conn = None
                self.client = None
                self.addr = None
                self.pc_is_connected = False

        def if_pc_is_connected(self):
                # Check for connection
                return self.pc_is_connected

        def init_pc(self):
                # Create socket
                try:
                        self.conn = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                        self.conn.bind((self.tcp_ip, self.port))
                        self.conn.listen(3)
                        print "Listening for incoming connections from PC..."
                        self.client, self.addr = self.conn.accept()
                        print "Connected! Connection address: ", self.addr
                        self.pc_is_connected = True
                except Exception as e:
                        print "Error: %s" % str(e)
                        print "Try again in a few seconds"

        def write_to_pc(self, message):
                # Write to PC
                try:
                        self.client.sendto(message, self.addr)
                except TypeError:
                        print "Error: Null value cannot be sent"

        def read_from_pc(self):
                # Read from PC
                try:
                        pc_data = self.client.recv(2048)
                        return pc_data
                except Exception as e:
                        print "Error: %s " % str(e)
                        print "Value not read from PC"

        def close_pc(self):
                # Closing socket
                self.conn.close()
                print "Closing server socket"

                self.client.close()
                print "Closing client socket"
                self.pc_is_connected = False

if __name__ == "__main__":
        # Test PC connection
        pc = PCObj()
        pc.init_pc()
        send_msg = input()
        print  "Writing to PC: %s " % send_msg
        pc.write_to_pc(send_msg)

        print "read"
        read_msg = pc.read_from_pc()
        print "Received from PC: %s " % read_msg

        print "closing sockets"
        pc.close_pc()
