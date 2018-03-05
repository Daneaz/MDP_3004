import serial
import time
import sys
import os
import threading

class ArduinoObj(object):
        def __init__(self):
                # Initialize ArduinoObj
                self.port = '/dev/ttyACM0'
                self.baud_rate = 115200
                self.ser = None

        def init_sr(self):
                # Create socket
                try:
                        self.ser = serial.Serial(self.port, self.baud_rate)
                        print"Serial link connected"
                except Exception as e:
                        print "Error (Serial): %s " % str(e)

        def write_to_sr(self, msg):
                # Write to Arduino
                try:
                        self.ser.write(msg)
                        print "Write to arduino: %s " % msg
                except AttributeError:
                        print"Error in serial comm. No value to be written. Check connection!"

        def read_from_sr(self):
                # Read from Arduino
                try:
                        received_data = self.ser.readline()
                        print "Received from arduino: %s " % received_data
                        return received_data
                except AttributeError:
                        print"Error in serial comm. No value received. Check connection!"

        def close_sr(self):
                # Closing socket
                self.ser.close()
                print"Closing serial socket"
        
        def read_from_sr2(self):
                # Read from Arduino
                try:
                  while True:                
                    received_data = self.ser.readline()
                    print "Received from arduino: %s " % received_data
                  
                except AttributeError:
                        print"Error in serial comm. No value received. Check connection!"

if __name__ == "__main__":
        # Test Arduino connection
        sr = ArduinoObj()
        sr.init_sr()
        print "serial connection successful"
        try:
                while True:
                        send_msg = raw_input  ()
                        print "Write(): %s " % send_msg
                        
                        # Create read and write threads for SR
                        read_sr = threading.Thread(target = sr.read_from_sr2, args = (,), name = "sr_read_thread")
                        write_sr = threading.Thread(target = sr.write_to_sr, args = (send_msg,), name = "sr_write_thread")

                        # Set threads as Daemons
                        read_sr.daemon = True
                        write_sr.daemon = True

                        # Start Threads
                        read_sr.start()
                        write_sr.start()

        except KeyboardInterrupt:
                print "closing sockets"
                sr.close_sr()


'''                        
        send_msg = raw_input()
        print "Writing [%s] to arduino" % send_msg
        sr.write_to_sr(send_msg)

        print "read"
        print "data received from serial" % sr.read_from_sr

        print "closing sockets"
        sr.close_sr()'''
