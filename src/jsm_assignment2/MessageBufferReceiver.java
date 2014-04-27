package jsm_assignment2;

import javax.sound.midi.*;

/**
 * Receives the data from the MIDI device. Implements Receiver class meaning it
 * will function as a MIDI receiver.
 *
 * @author James Hill
 * Student Number: 11006807
 */
public class MessageBufferReceiver implements Receiver {

    private MessageBuffer theBuffer;

    /**
     * Constructor for the class
     *
     * @param b - Passed in MessageBuffer object
     */
    public MessageBufferReceiver(MessageBuffer b) {
        theBuffer = b;
    }

    /**
     * Run whenever MIDI data is transmitted from the device.
     *
     * @param theMessage - The MidiMessage data
     * @param timeStamp - TimeStamp of transmission
     */
    public void send(MidiMessage theMessage, long timeStamp) {
        try {
            //Gets the message
            byte[] byteData = theMessage.getMessage();
            int statusValue = byteData[0] & 0xF0;

            boolean save = false;
            if (statusValue < 240) {
                switch (statusValue) {
                    case ShortMessage.NOTE_OFF:
                    case ShortMessage.NOTE_ON:
                        save = true;
                        break;
                    default:
                        save = false;
                        break;
                }
            } else {
                //System messages
                //For these messages we will always want to save them as there is an "Other Messages"
                //box that will increment no matter what the message is
                save = true;
            }
            if (save) {
                //System.out.println(byteData.length);
                theBuffer.put(byteData);
            }
        } catch (Exception e) {
            System.out.println("Something went wrong in Receiver: " + e);
        }
    }

    /**
     * Closes the receiver
     */
    public void close() {
        System.out.println("Closing Receiver");
    }
}
