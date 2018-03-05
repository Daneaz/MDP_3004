from bluetooth import *

class AndroidObj(object):

        def __init__(self):
                # Initialize AndroidObj
                self.server_soc = None
                self.client_soc = None
                self.bt_is_connected = False

        def if_bt_is_connected(self):
                # Check for connection
                return self.bt_is_connected

        def init_bt(self):
                # Create socket
                btport = 4
                try:
                        self.server_soc = BluetoothSocket( RFCOMM )
                        self.server_soc.bind(("", btport))
                        self.server_soc.listen(1)
                        self.port = self.server_soc.getsockname()[1]
                        uuid = "00001101-0000-1000-8000-00805F9B34FB"

                        advertise_service( self.server_soc, "SampleServer",
                                           service_id = uuid,
                                           service_classes = [ uuid, SERIAL_PORT_CLASS ],
                                           profiles = [ SERIAL_PORT_PROFILE ],)
                        print("Waiting for BT connection on RFCOMM channel " + self.port)
                        self.client_soc, client_add = self.server_socket.accept()
                        print("Accepted connection from ", client_add)
                        self.bt_is_connected = True

                except Exception as e:
                        print("Error: %s" %str(e))
                        #self.init_bt()

        def write_to_bt(self, message):
                # Write to Android
                try:
                        self.client_soc.send(str(message))
                except BluetoothError:
                        print("Bluetooth Error. Connection re-established.")
                        self.init_bt()
                        
        def read_from_bt(self):
                # Read from Android
                try:
                        msg = self.client_soc.recv(2048)
                        print ("Received [%s] " % msg)
                        return msg
                except BluetoothError:
                        print("Bluetooth Error. Connection reset by peer. Trying to connect...")
                        self.init_bt()

        def close_bt(self):
                # Closing socket
                self.client_soc.close()
                print("Closing client socket")
                self.server_soc.close()
                print("Closing server socket")
                self.bt_is_connected = False
'''
if __name__ == "__main__":
        # Test Android connection
        bt = AndroidObj()
        bt.init_bt()
        print("bluetooth connection successful")
        
        send_msg = input()
        print("Write(): %s " % send_msg)
        bt.write_to_bt(send_msg)

        print("read")
        read_msg = bt.read_from_bt()
        print("data received: %s " % read_msg)

        print("closing sockets")
        bt.close_bt()
'''
