package com.example.controldemo;

import jp.ksksue.driver.serial.FTDriver;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.widget.Toast;

public class SerialClass{
    public static FTDriver mSerial;
    public static boolean connected = false;
    private volatile String ReadHold = "";
    private static byte[] arbuf = new byte[4096];
    private static Integer lastArrayPos_arbuf = 0;
    private static int alen=0;
    
    Context usethis;
    // [FTDriver] Permission String
    private static final String ACTION_USB_PERMISSION =
            "jp.ksksue.tutorial.USB_PERMISSION";
    
    SerialClass (UsbManager manager,Context usethis) {
        mSerial = new FTDriver(manager);

        // [FTDriver] setPermissionIntent() before begin()
        PendingIntent permissionIntent = PendingIntent.getBroadcast(usethis, 0, new Intent(
                ACTION_USB_PERMISSION), 0);
        mSerial.setPermissionIntent(permissionIntent);
        this.usethis = usethis;

        connected = connect(true);
    }

    public boolean connect(boolean connect) {
    	if( connect) {
	        // [FTDriver] Open USB Serial
	        if(mSerial.begin(FTDriver.BAUD115200)) {
	        	connected = true;
	        	return true;
	        } else {
	        	connected = false;
	        	return false;
	        }    
    	}
    	else
    	{
    		mSerial.end();
    	}
    	connected = false;
    	return false;
    }
    
    public synchronized void Write(String output) {
    	Write_NoMutex(output);
    }
    
    public void Write_NoMutex(String output) {
    	if(!connected) {
    		Toast.makeText(usethis, "Not Connected", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	mSerial.write((output + "\r").getBytes());
    }

    public synchronized String WriteRead(String output, int Timeout) {
    	if(!connected) {
    		Toast.makeText(usethis, "Not Connected", Toast.LENGTH_SHORT).show();
    		return "";
    	}
    	while(!ReadHold.equals("")) {
            try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    	Read_Asyn(0); //clear anything in the buffer before writing
    	ReadHold = "";
    	Write_NoMutex(output);
    	return Read_Asyn(Timeout);
    }
    
    public synchronized String Read_Asyn(int Timeout) {
        int len,i;
		byte[] rbuf = new byte[4096];
    	if(Timeout>0) {
	        long start = System.currentTimeMillis();
	    	String input="";
	    	while((System.currentTimeMillis()-start)<Timeout) {
	            len = mSerial.read(rbuf);
	            if( len > 0 ) {
	            	for(i=0;i<len;i++) {
	            		if (rbuf[i] == 0x0D) {
	            			return input;
						} else if (rbuf[i] == 0x0A) {
							//return input;
						} else {
							input = input + "" + (char)rbuf[i];
						}
	            	}
	            }
	            try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	    	}
	    	return input;
    	} else {
	    	if(!connected) {
	    		return "";
	    	}
			String input = "";
			if(lastArrayPos_arbuf>=alen){
				alen = mSerial.read(arbuf);
				lastArrayPos_arbuf = 0;
			}
	    	for(i=lastArrayPos_arbuf;i<alen;i++) {
	    		if (arbuf[i] == 0x0D) { // \r
	    			input = new String(ReadHold);
	    			ReadHold = "";
	    			lastArrayPos_arbuf = i+1;
	    			return input;
				} else if (arbuf[i] == 0x0A) { // \n
					//ignore line feeds
				} else {
					ReadHold = ReadHold + (char)arbuf[i];
				}
	        }
	    	lastArrayPos_arbuf = i+1;
	    	return "";
    	}
    }
}