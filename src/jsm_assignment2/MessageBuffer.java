package jsm_assignment2;

import javax.sound.midi.*;

/**
 * Buffer classed used as a common data storage. Utilises synchronized functions
 * in order to prevent bugs.
 *
 * @author James Hill
 * Student Number: 11006807
 */
public class MessageBuffer {

    //Buffer queue variable. Stores an array of arrays
    private byte bufferQueue[][] = new byte[16][];
    //Used to count how many items there are in the array
    private int count = 0;
    //Used to track the put position in the queue
    private int putIndex = 0;
    //Used to track the get position in the queue
    private int getIndex = 0;

//    public synchronized void put(MidiMessage theMessage) {
//        this.buffer = theMessage.getMessage();
//        storeTime(this.buffer);
//    }//put(MidiMessage, long)
    
    /**
     * Puts the byte data into the buffer queue
     *
     * @param theByteArray
     */
    public synchronized void put(byte[] theByteArray) {

        //If there is space in the queue
        if (theByteArray != null) {
            if (count < bufferQueue.length) {
                //Increase the data counter
                count++;
                //Moves the put counter up
                putIndex++;
                //System.out.println(putIndex);
                if (putIndex >= 16) {
                    putIndex = 0;
                }
                bufferQueue[putIndex] = theByteArray;
            } else {
                System.out.println("Buffer is full");
            }
        }
    }//put

    /**
     * Retrieves the next data item from the buffer queue
     *
     * @return
     */
    public synchronized byte[] get() {
        //If there is something to be retrieved
        if (count > 0) {
            //Reduce the data counter
            count--;
            //Move the get counter up
            getIndex++;
            //
            if (getIndex >= 16) {
                getIndex = 0;
            }
            return bufferQueue[getIndex];
        } else {
            return null;
        }

    }//get()
    
}//MessageBuffer
