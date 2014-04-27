package jsm_assignment2;

import javax.sound.midi.*;

/**
 * Filter class used for the device list
 *
 * @author James Hill
 * Student Number: 11006807
 */
public class MidiFilter {

    public MidiFilter() {
    }//constructor

    /**
     * <p>Filters the input devices so that it returns an array only containing
     * devices that are MIDI-IN.</p>
     *
     * <p>Essentially what it does is creates an empty array, loops through the
     * first array to check what it should be copying and then transfers the
     * items due to be copied into the new empty array.</p>
     *
     * <p>I modified it so that you could specify the type of device to filter
     * out, this results in less repeated code overall.</p>
     *
     * @param infoList - The passed in list of devices
     * @param select - For the transmitter it decides whether to use MIDI-IN or
     * MIDI-OUT. True for IN, False for OUT. For synth and sequencer it boils
     * down to true(add it the list) or false(don't want it in the list)
     * @param type - Type of device to filter for. 0=Transmitter, 1=Synthesizer,
     * 2=Sequencer
     *
     * So for example if you wanted to display all MIDI-IN you would call the
     * method with the parameters select=true and type=0 If you wanted the list
     * to not include sequencers you will call the method with the parameters
     * select=false and type=2
     */
    public MidiDevice.Info[] filterInputDevices(MidiDevice.Info[] infoList, boolean select, int type) {
        //boolean array to decide which elements will be copied
        boolean[] copyElement = new boolean[infoList.length];
        //running total of the amount of IN devices
        int deviceCount = 0;
        //loop until end of the infoList
        for (int index = 0; index < infoList.length; index++) {
            try {
                //gets the boolean device at the index position of the array
                MidiDevice device = MidiSystem.getMidiDevice(infoList[index]);
                //checks to see if the MIDI device has any of the specified type
                switch (type) {
                    //transmitter
                    case 0:
                        boolean hasTransmitter = (device.getMaxTransmitters() != 0);
                        //if that index is to be copied
                        if (hasTransmitter == select) {
                            //set the element at the index to be true in order to move it to the second array
                            copyElement[index] = true;
                            //increment deviceCount
                            deviceCount++;
                        }
                        break;
                    //synthesizer
                    case 1:
                        //if the device is a synth
                        boolean isSynthesizer = (device instanceof Synthesizer);
                        if (isSynthesizer == select) {
                            copyElement[index] = true;
                            deviceCount++;
                        }
                        break;
                    //sequencer
                    case 2:
                        boolean isSequencer = (device instanceof Sequencer);
                        if (isSequencer == select) {
                            copyElement[index] = true;
                            deviceCount++;
                        }
                        break;
                    //if it gets here, something has gone horribly wrong and everyone should panic.
                    default:
                        throw new IllegalArgumentException();
                }//switch
            } catch (MidiUnavailableException mue) {
                System.out.println("Midi Unavailable: " + mue);
            } catch (IllegalArgumentException iae) {
                System.out.println("Invalid type passed into filterInputDevice: " + iae);
                System.exit(0);
            }
        }//for
        //creates new list for the filtered devices
        MidiDevice.Info[] outList = new MidiDevice.Info[deviceCount];
        //counter for the filtered array
        int outIndex = 0;
        for (int index = 0; index < infoList.length; index++) {
            //copyElement[index] will either be true or false so this is actually if(true) if(false)
            if (copyElement[index]) {
                //sets the output array to be whatever is in the index of the original
                outList[outIndex] = infoList[index];
                //increments output counter
                outIndex++;
            }//if
        }//for
        return outList;
    }//filterInputDevices()
}
