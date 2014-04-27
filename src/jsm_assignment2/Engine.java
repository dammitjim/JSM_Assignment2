package jsm_assignment2;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JOptionPane;

/**
 * Main class, has an infinite loop to constantly check for new data in the buffer, if there is new data it outputs it 
 * as the selected waveform.
 *
 * @author James Hill
 * Student Number: 11006807
 */
public class Engine {

    public static void main(String[] args) {

        //Creates a new OrganDialogue, this performs a chain call to the GUI while allowing me to call the getWaveform method.
        OrganDialogue o = new OrganDialogue("Organ");

        try {

            //Creates an array of all MIDI attached to the computer
            MidiDevice.Info[] deviceList = MidiSystem.getMidiDeviceInfo();

            //Prints out each device in the array before it is filtered
            System.out.println("Device list pre filter: ");
            
            for (int i = 0; i < deviceList.length; i++) {
                System.out.println(deviceList[i]);
            }//for

            //Creates object of the MidiFilter class
            MidiFilter filter = new MidiFilter();

            //Uses the filterInputDevices method to only return IN MIDI
            //For information on the parameters please consult the java doc or check the MidiFilter class
            deviceList = filter.filterInputDevices(deviceList, true, 0);
            deviceList = filter.filterInputDevices(deviceList, false, 1);
            deviceList = filter.filterInputDevices(deviceList, false, 2);

            //Creates option pane to select MIDI device
            MidiDevice.Info deviceInfo = (MidiDevice.Info) JOptionPane.showInputDialog(null,
                    "Select MIDI input Device",
                    "Device Selector", JOptionPane.QUESTION_MESSAGE, null,
                    deviceList,
                    null);

            //If a device has been selected (not empty)
            if (deviceInfo != null) {

                //Create new midi device using the MidiSystem class to get the information for the selection (oh god I'm talking in circles)
                try {

                    //Selects the device and opens it
                    MidiDevice theDevice = MidiSystem.getMidiDevice(deviceInfo);
                    System.out.println("\nThe device selected is a " + theDevice.getDeviceInfo());
                    theDevice.open();

                    //Creates a new message buffer and reciever
                    MessageBuffer bufferObject = new MessageBuffer();
                    MessageBufferReceiver recv = new MessageBufferReceiver(bufferObject);

                    //Creates a new transmitter and sets the reciever to it
                    Transmitter transmitter = theDevice.getTransmitter();
                    transmitter.setReceiver(recv);

                    //Asks the user for the number of samples
                    String stringOfSamples = JOptionPane.showInputDialog("Please input the number of samples");
                    //Takes the string and uses regex to remove everything that isn't a number
                    stringOfSamples = stringOfSamples.replaceAll("\\D+", "");

                    int numberOfSamples;

                    //If the string is empty assign the default value, else convert it to an integer.
                    if (stringOfSamples.isEmpty()) {
                        numberOfSamples = 2000;
                    } else {
                        numberOfSamples = Integer.parseInt(stringOfSamples);
                    }

                    //Main array which holds the waveform data
                    int[] intBuffer = new int[numberOfSamples];

                    //Contains left ear (channel) data
                    int[] leftBuffer = new int[numberOfSamples];

                    //Contains right ear data
                    int[] rightBuffer = new int[numberOfSamples];

                    //The panning range
                    int panPosition = 63;

                    //Holds the finalised big endian waveform data
                    byte[] byteBuffer = new byte[4 * numberOfSamples];

                    //Various waveform-generating varaibles
                    double frequency = 0;
                    double previousNote = 0;
                    double samplingRate = 48000.0;
                    double gain = 0;
                    double samplesPerWave = samplingRate / frequency;

                    //Boolean flag to check if the source data line is currently outputting sound
                    boolean isPlaying = false;

                    //Starts at the very first sample
                    int firstSampleNumber = 0;

                    //Declares the audioformat and opens the SDL with it.
                    AudioFormat audioFormat = new AudioFormat((float) samplingRate, 16, 2, true, true);
                    SourceDataLine sdl = AudioSystem.getSourceDataLine(audioFormat);
                    sdl.open(audioFormat, byteBuffer.length);
                    sdl.start();

                    while (true) {
                        try {
                            //Gets the next message in line from the buffer
                            byte[] messageBytes = bufferObject.get();
                            if (messageBytes != null) {

                                //In order to determine which message type it is we need to AND out all information other than status
                                int statusValue = messageBytes[0] & 0xF0;

                                //Calculates the panning position based on the pitch.
                                panPosition = (int) messageBytes[1] - 33;
                                System.out.println(panPosition);
                                switch (statusValue) {
                                    
                                    case ShortMessage.NOTE_OFF:
                                        //System.out.println("Note Off ");
                                        //If the frequency of the note off message is the same as the previousNote stop playback, if a seperate key is lifted ignore the stop playback.
                                        frequency = frequencyFromMidiPitch((int) messageBytes[1]);
                                        if (frequency == previousNote) {
                                            isPlaying = false;
                                        }
                                        break;
                                    case ShortMessage.NOTE_ON:
                                        //Since my keyboard is £20 instead of sending a NOTE_OFF when you release it sends a NOTE_ON with 0 velocity, this accounts for that.
                                        int velocity = messageBytes[2];
                                        if (velocity == 0) {
                                            //System.out.println("Note Off");
                                            //Calculates the frequency from the lookup table
                                            frequency = frequencyFromMidiPitch((int) messageBytes[1]);
                                            if (frequency == previousNote) {
                                                isPlaying = false;
                                            }
                                        } else {
                                            //True note on
                                            
                                            //Sets playing to true
                                            isPlaying = true;
                                            
                                            //Resets the sample number
                                            firstSampleNumber = 0;
                                            
                                            //Resets the gain so it can be heard at full volume
                                            gain = 30000.0;
                                            
                                            //Calculates frequency
                                            frequency = frequencyFromMidiPitch((int) messageBytes[1]);
                                            
                                            //Sets the previousNote to the current frequency
                                            previousNote = frequency;
                                            
                                            //Calculates samples per wave
                                            samplesPerWave = samplingRate / frequency;

                                        }//ifelse
                                        break;
                                } //switch
                            }//if
                        } catch (ArrayIndexOutOfBoundsException e) {
                            System.out.println(e);
                            //Thread.dumpStack();
                        } catch (Exception e) {
                            System.out.println("Something broke, inform the men: " + e);
                        }//trycatch
                        
                        //If there is no sound playing constantly decrease the volume until it reaches 0
                        if (!isPlaying) {
                            if (gain > 100) {
                                gain = gain * 0.95;
                            } else {
                                gain = 0;
                            }
                        }
                        
                        //Checks to see which waveform is selected
                        switch (o.getWaveform()) {
                            //SINE
                            case 0:
                                //Fills the buffer with sine wave data
                                fillSineBuffer(intBuffer, samplesPerWave, gain, firstSampleNumber, numberOfSamples);
                                
                                //Pans each channel to the correct position
                                panSource(panPosition, intBuffer, leftBuffer, rightBuffer);
                                break;
                            //SQUARE
                            case 1:
                                //Fills the buffer with square wave data
                                fillSquareBuffer(intBuffer, samplesPerWave, gain, firstSampleNumber, numberOfSamples);
                                
                                //Pans each channel to the correct position
                                panSource(panPosition, intBuffer, leftBuffer, rightBuffer);
                                break;
                            //SAWTOOTH
                            case 2:
                                //Fills the buffer with sawtooth wave data
                                fillSawtoothBuffer(intBuffer, samplesPerWave, gain, firstSampleNumber, numberOfSamples);
                                
                                //Pans each channel to the correct position
                                panSource(panPosition, intBuffer, leftBuffer, rightBuffer);
                                break;
                            //TRIANGLE
                            case 3:
                                //Triangle
                                break;
                        }
                        
                        //Increments the sample to begin playback on
                        firstSampleNumber += numberOfSamples;
                        
                        //Converts the intBuffer to a big endian byte buffer
                        //convert16bitBigEndian(intBuffer, byteBuffer);
                        convert16bitStereoBigEndian(leftBuffer, rightBuffer, byteBuffer);
                        
                        //Writes the data to the SDL
                        sdl.write(byteBuffer, 0, byteBuffer.length);
                    }

                } catch (MidiUnavailableException mue) {
                    System.out.println("Selected MIDI device unavailable " + mue);
                    System.exit(0);
                } catch (Exception e) {
                    System.out.println("Unknown error: " + e);
                    System.exit(0);
                }//trycatch
            } //if
            else {
                //Executes if no device is selected
                System.out.println("Exiting");
                System.exit(0);
            }
        }//try
        catch (Exception e) {
            System.out.println("Error thrown: " + e);
            System.exit(0);
        }//catch
    } // main () method

    /**
     * Calculates the panning position for the current key.
     *
     * @param midiPanPosition the range to pan
     * @param source the buffer containing the current sine wave
     * @param leftBuffer the left ear channel buffer
     * @param rightBuffer the right ear channel buffer
     */
    
    public static void panSource(int midiPanPosition, int[] source, int[] leftBuffer, int[] rightBuffer) {
        final double PAN_ANGLE_MULTIPLIER = Math.PI / (2.0 * 63.0);
        double panAngle = midiPanPosition * PAN_ANGLE_MULTIPLIER;
        double leftGain = Math.cos(panAngle);
        double rightGain = Math.sin(panAngle);

        for (int i = 0; i < source.length; i++) {
            int sample = source[i];
            leftBuffer[i] = (int) (leftGain * sample);
            rightBuffer[i] = (int) (rightGain * sample);
        }
    }

    /**
     * Converts the input intBuffer to a big endian style byte buffer
     *
     * @param intBuffer input wave
     * @param byteBuffer outputted big endian buffer
     */
    
    public static void convert16bitBigEndian(int[] intBuffer,
            byte[] byteBuffer) {
        int b = 0;
        for (int sample : intBuffer) {
            byte msb = (byte) ((sample >> 8) & 0xFF);
            byte lsb = (byte) (sample & 0xFF);
            byteBuffer[b] = msb;
            b++; // Big-endian order
            byteBuffer[b] = lsb;
            b++;
        }
    }

    /**
     * Similar to convert16bitBigEndian however it does so for both the left and
     * right channels
     *
     * @param leftBuffer leftBuffer received from panSource
     * @param rightBuffer rightBuffer received from panSource
     * @param byteBuffer outputted big endian byte buffer
     */
    
    public static void convert16bitStereoBigEndian(int[] leftBuffer, int[] rightBuffer, byte[] byteBuffer) {
        int b = 0;
        int sample;

        for (int i = 0; i < leftBuffer.length; i++) {
            // Process next sample for LEFT channel
            sample = leftBuffer[i];
            byte msb = (byte) ((sample >> 8) & 0xFF);
            byte lsb = (byte) (sample & 0xFF);
            // Store in the buffer
            byteBuffer[b] = msb;
            b++; // Big-endian
            byteBuffer[b] = lsb;
            b++;

            // Process next sample for RIGHT channel
            sample = rightBuffer[i];
            msb = (byte) ((sample >> 8) & 0xFF);
            lsb = (byte) (sample & 0xFF);
            // Store in the buffer
            byteBuffer[b] = msb;
            b++; // Big-endian
            byteBuffer[b] = lsb;
            b++;
        }
    }

    /**
     * Fills the integer buffer with Sine wave samples
     *
     * @param intBuffer the output buffer
     * @param samplesPerWave how many samples to take per wave
     * @param gain the gain (volume) of the wave
     * @param firstSampleNumber the sample by which the program will begin
     * generating waveforms from
     * @param numberOfSamples number of samples to generate
     */
    
    private static void fillSineBuffer(int[] intBuffer,
            double samplesPerWave,
            double gain,
            int firstSampleNumber,
            int numberOfSamples) {

        // Calculates the factor from radians
        double factor = 2.0 * Math.PI / samplesPerWave;
        // The sample to start from
        int number = firstSampleNumber;
        for (int i = 0; i < numberOfSamples; i++) {
            // Phase angle = point on the curve
            double phaseAngle = factor * number;
            // Moves to next sample
            number++;
            // Adds gain and calculates the sine angle
            int sample = (int) (gain * Math.sin(phaseAngle));
            intBuffer[i] = sample;
        }
        // Hopefully at this point should have a wave that looks like this: ~
    }

    /**
     * Fills the integer buffer with Square wave data
     *
     * @param intBuffer the output buffer
     * @param samplesPerWave how many samples to take per wave
     * @param gain the gain (volume) of the wave
     * @param firstSampleNumber the sample by which the program will begin
     * generating waveforms from
     * @param numberOfSamples number of samples to generate
     */
    
    private static void fillSquareBuffer(int[] intBuffer,
            double samplesPerWave,
            double gain,
            int firstSampleNumber,
            int numberOfSamples) {

        // Calculate sample number ( position along the waveform )
        int sampleNumber = firstSampleNumber % (int) samplesPerWave;

        int sample;

        // Fill buffer with the square wave
        for (int i = 0; i < numberOfSamples; i++) {

            // If the sample is in the first half of the wave
            if (sampleNumber < samplesPerWave / 2) {
                sample = (int) -gain;
            } else {
                sample = (int) +gain;
            }
            //System.out.println(sample);
            intBuffer[i] = sample;
        }
    }

    /**
     * Fills the integer buffer with Sawtooth wave data
     *
     * @param intBuffer the output buffer
     * @param samplesPerWave how many samples to take per wave
     * @param gain the gain (volume) of the wave
     * @param firstSampleNumber the sample by which the program will begin
     * generating waveforms from
     * @param numberOfSamples number of samples to generate
     */
    
    private static void fillSawtoothBuffer(int[] intBuffer,
            double samplesPerWave,
            double gain,
            int firstSampleNumber,
            int numberOfSamples) {
        //Make with the sawtoothing
        //samplenumber%samplesperwave*2/samplesperwave-1
        int sampleNumber = firstSampleNumber % (int) samplesPerWave;
        int sample;

        for (int i = 0; i < numberOfSamples; i++) {
            sample = (sampleNumber % (int) samplesPerWave) * 2 / ((int) samplesPerWave - 1);
            System.out.println(sample);
            intBuffer[i] = sample;
        }
    }

    /**
     * Fills the integer buffer with Triangle wave data
     *
     * @param intBuffer the output buffer
     * @param samplesPerWave how many samples to take per wave
     * @param gain the gain (volume) of the wave
     * @param firstSampleNumber the sample by which the program will begin
     * generating waveforms from
     * @param numberOfSamples number of samples to generate
     */
    
    private static void fillTriangleBuffer(int[] intBuffer,
            double samplesPerWave,
            double gain,
            int firstSampleNumber,
            int numberOfSamples) {
        //Triangles are cool
    }

    /**
     * Obtain the frequency from the notes' midi pitch number
     */
    
    public static double frequencyFromMidiPitch(int midiPitchNumber) {
        if ((midiPitchNumber < MIDI_LOW) || (midiPitchNumber > MIDI_HIGH)) {
            return 0;
        } else {
            return freqFromPitch[midiPitchNumber];
        }
    } // frequencyFromMidiPitch ()
    /*
     Static initialiser for the freqFromPitch lookup table.
     */
    private static final int MIDI_LOW = 33;
    private static final int MIDI_HIGH = 96;
    private static double[] freqFromPitch = new double[MIDI_HIGH + 1];

    static {
        freqFromPitch[33] = 55.00;     // A 55
        freqFromPitch[34] = 58.27;
        freqFromPitch[35] = 61.74;
        freqFromPitch[36] = 65.41;
        freqFromPitch[37] = 69.30;
        freqFromPitch[38] = 73.42;
        freqFromPitch[39] = 77.78;
        freqFromPitch[40] = 82.41;
        freqFromPitch[41] = 87.31;
        freqFromPitch[42] = 92.50;
        freqFromPitch[43] = 98.00;
        freqFromPitch[44] = 103.83;
        freqFromPitch[45] = 110.00;        // A 110
        freqFromPitch[46] = 116.54;
        freqFromPitch[47] = 123.47;
        freqFromPitch[48] = 130.81;
        freqFromPitch[49] = 138.59;
        freqFromPitch[50] = 146.83;
        freqFromPitch[51] = 155.56;
        freqFromPitch[52] = 164.81;
        freqFromPitch[53] = 174.61;
        freqFromPitch[54] = 185.00;
        freqFromPitch[55] = 196.00;
        freqFromPitch[56] = 207.65;
        freqFromPitch[57] = 220.00;        // A 220
        freqFromPitch[58] = 233.08;
        freqFromPitch[59] = 249.94;
        freqFromPitch[60] = 261.63;        // Mid C
        freqFromPitch[61] = 277.18;
        freqFromPitch[62] = 293.66;
        freqFromPitch[63] = 311.13;
        freqFromPitch[64] = 329.63;
        freqFromPitch[65] = 349.23;
        freqFromPitch[66] = 369.99;
        freqFromPitch[67] = 392.00;
        freqFromPitch[68] = 415.30;
        freqFromPitch[69] = 440.00;        // A 440
        freqFromPitch[70] = 466.16;
        freqFromPitch[71] = 493.88;
        freqFromPitch[72] = 523.25;
        freqFromPitch[73] = 554.37;
        freqFromPitch[74] = 587.33;
        freqFromPitch[75] = 622.77;
        freqFromPitch[76] = 659.26;
        freqFromPitch[77] = 698.46;
        freqFromPitch[78] = 739.99;
        freqFromPitch[79] = 783.99;
        freqFromPitch[80] = 830.61;
        freqFromPitch[81] = 880.00;        // A 880
        freqFromPitch[82] = 932.33;
        freqFromPitch[83] = 987.77;
        freqFromPitch[84] = 1046.50;
        freqFromPitch[85] = 1108.73;
        freqFromPitch[86] = 1174.66;
        freqFromPitch[87] = 1244.51;
        freqFromPitch[88] = 1318.51;
        freqFromPitch[89] = 1396.91;
        freqFromPitch[90] = 1479.98;
        freqFromPitch[91] = 1567.98;
        freqFromPitch[92] = 1661.22;
        freqFromPitch[93] = 1760.00;       // A 1760
        freqFromPitch[94] = 1864.66;
        freqFromPitch[95] = 1975.53;
        freqFromPitch[96] = 2093.00;
    } // static
}
