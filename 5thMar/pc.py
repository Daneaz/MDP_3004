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

        def write_to_pc2(self):
                # Write to PC
                while True:
                    msg=raw_input()
                    print "Write to pc: %s"%msg
                    self.client.sendto(message, self.addr)

        def read_from_pc2(self):
                # Read from PC
                while True:
                        try:
                                pc_data = self.client.recv(2048)
                                print "Received %s from PC"%pc_data
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

        def keep_main_alive(self):
                """
                Allows for a Ctrl+C kill while keeping the main() alive
                """
                while True:
                        time.sleep(1)

if __name__ == "__main__":
        # Test PC connection
        pc = PCObj()
        pc.init_pc()
        try:
                # Create read and write threads for BT
                read_pc = threading.Thread(target = pc.read_from_pc2, args = (), name = "pc_read_thread")
                write_pc = threading.Thread(target = pc.write_to_pc2, args = (), name = "pc_write_thread")

                # Set threads as Daemons
                read_pc.daemon = True
                write_pc.daemon = True

                # Start Threads
                read_pc.start()
                write_pc.start()

                pc.keep_main_alive()

        except KeyboardInterrupt:
                print "closing sockets"
                pc.close_pc()
