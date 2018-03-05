from pc import *
from android import *
from arduino import *

import sys
import time
import queue
import threading

class RPi(threading.Thread):

        def __init__(self):
                threading.Thread.__init__(self)

                # Create PC, Android and Arduino objects
                self.pc_thread = PCObj()
                self.bt_thread = AndroidObj()
                self.sr_thread = ArduinoObj()

                # Initialize the connections
                self.pc_thread.init_pc()
                self.bt_thread.init_bt()
                self.sr_thread.init_sr()

                # Initialize the queues
                self.pcQueue = queue.Queue(maxsize=0)
                self.btQueue = queue.Queue(maxsize=0)
                self.srQueue = queue.Queue(maxsize=0)

                # Wait for 1.5 seconds before starting
                time.sleep(1)


        # Functions for PC
        def readFromPC(self, btQueue, srQueue):

                # Read from PC
                print "Reading from PC"
                while True:
                        msg = self.pc_thread.read_from_pc()

                        # Send to Android from PC using 'b' header
                        if(msg[0].lower() == 'b'):
                                btQueue.put_nowait(msg[1:])
                                print "Message received from PC to Android: "+ msg[1:]

                        # Send to Arduino from PC using 'a' header
                        elif(msg[0].lower() == 'a'):
                                srQueue.put_nowait(msg[1:])
                                print "Message received from PC to Arduino: " + msg[1:]

                        else:
                                print "Incorrect header received from PC: " + msg[0]
                                time.sleep(1)


        def writeToPC(self, pcQueue):

                # Write to PC
                print "Writing to PC"
                while True:
                        if not pcQueue.empty():
                                msg = pcQueue.get_nowait()
                                self.pc_thread.write_to_pc(msg)
                                print "Message sent to PC: " + msg


        # Functions for Android
        def readFromBT(self, pcQueue, srQueue):

                # Read from Android
                print "Reading from Android"
                while True:
                        msg = self.bt_thread.read_from_bt()

                        # Send to PC from Android using 'p' header
                        if(msg[0].lower() == 'p'):
                                pcQueue.put_nowait(msg[1:])
                                print "Message received from Android to PC: " + msg[1:]

                        # Send to Arduino from Android using 'a' header
                        elif(msg[0].lower() == 'a'):
                                srQueue.put_nowait(msg[1:])
                                print "Message received from Android to Arduino: " + msg[1:]

                        else:
                                print "incorrect header received from Android: " + msg[0]
                                time.sleep(1)


        def writeToBT(self, btQueue):

                # Write to Android
                print("Writing to Android")
                while True:
                        if not btQueue.empty():
                                msg = btQueue.get_nowait()
                                self.bt_thread.write_to_bt(msg)
                                print "Message sent to Android: " + msg


        # Functions for Arduino
        def readFromSR(self, pcQueue, btQueue):

                # Read from Arduino
                print "Reading from Arduino"
                while True:
                        msg = self.sr_thread.read_from_sr()

                        # Send to PC from Arduino using 'p' header
                        if(msg[0].lower() == 'p'):
                                pcQueue.put_nowait(msg[1:])
                                print "Message received from Arduino to PC: " + msg[1:]

                        # Send to Android from Arduino using 'b' header
                        elif(msg[0].lower() == 'b'):
                                btQueue.put_nowait(msg[1:])
                                print "Message received from Arduino to Android: " + msg[1:]

                        else:
                                print "incorrect header received from Arduino: " + msg[0]
                                time.sleep(1)


        def writeToSR(self, srQueue):
                # Write to Arduino
                print("Writing to Arduino")
                while True:
                        if not srQueue.empty():
                                msg = srQueue.get_nowait()
                                self.sr_thread.write_to_sr(msg)
                                print "Message sent to Arduino: " + msg


        # Initialize the threads
        def create_threads(self):

                # Create read and write threads for PC
                read_pc = threading.Thread(target = self.readFromPC, args = (self.btQueue, self.srQueue), name = "pc_read_thread")
                write_pc = threading.Thread(target = self.writeToPC, args = (self.pcQueue), name = "pc_write_thread")

                # Create read and write threads for Android
                read_bt = threading.Thread(target = self.readFromBT, args = (self.pcQueue, self.srQueue), name = "bt_read_thread")
                write_bt = threading.Thread(target = self.writeToBT, args = (self.btQueue), name = "bt_write_thread")

                # Create read and write threads for Arduino
                read_sr = threading.Thread(target = self.readFromSR, args = (self.pcQueue, self.btQueue), name = "sr_read_thread")
                write_sr = threading.Thread(target = self.writeToSR, args = (self.srQueue), name = "sr_write_thread")

                # Set threads as Daemons
                read_pc.daemon = True
                write_pc.daemon = True

                read_bt.daemon = True
                write_bt.daemon = True

                read_sr.daemon = True
                write_sr.daemon = True

                # Start Threads
                read_pc.start()
                write_pc.start()

                read_bt.start()
                write_bt.start()

                read_sr.start()
                write_sr.start()

        def close_all(self):
                pc_thread.close_pc()
                bt_thread.close_bt()
                sr_thread.close_sr()

        def keep_alive(self):
                while True:
                        time.sleep(1)
                        print "hi"


if __name__ == "__main__":
        # Starting main program
        print "Starting main program"
        main = RPi()
        try:
                main.create_threads()
                #main.keep_alive()
        except KeyboardInterrupt:
                print "Exiting main program"
                main.close_all()
