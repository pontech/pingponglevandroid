package com.example.controldemo;


import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.ParcelFileDescriptor;

public class AccessorySerial {
	private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";
	private final static int READ_BUFFER_SIZE = 128;
	public static final String REPLY_GETOUT = "getout";
	public static final String CMD_LETMEGO = "letmego";
	
	private UsbManager usbManager;
	private UsbAccessory usbAccessory;
	private ParcelFileDescriptor fileDescriptor;
	private FileInputStream accessoryInput;
	private FileOutputStream accessoryOutput;
	public boolean connected = false;

	private PendingIntent permissionIntent;
	public boolean requestingPermission = false;
	
	private final Handler handler = new Handler();
	private boolean waitingforanswer = false;
	private String answer = "";
    Context usethis;

    AccessorySerial(Context usethis) {
		usbManager = (UsbManager) usethis.getSystemService(Context.USB_SERVICE);
		permissionIntent = PendingIntent.getBroadcast(usethis, 0, new Intent(
				ACTION_USB_PERMISSION), 0);

		final IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		// ACTION_USB_ACCESSORY_ATTACHED¸ BroadcastReceiver,
		// filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
		usethis.registerReceiver(usbReceiver, filter);
        this.usethis = usethis;
    }
    public final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				final UsbAccessory accessory = (UsbAccessory) intent
						.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
						false)) {
					debug("Broadcast: accessory permission granted");

					openAccessory(accessory);
				} else {
					debug("Broadcast: permission denied for accessory");
				}
				requestingPermission = false;
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				final UsbAccessory accessory = (UsbAccessory) intent
						.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if (accessory != null && accessory.equals(usbAccessory)) {
					debug("Broadcast: accessory detached");

					disconnectFromAccessory();
				}
			}
		}
	};

	public void connectToAccessory() {
		final UsbAccessory[] accessories = usbManager.getAccessoryList();
		final UsbAccessory accessory = (accessories == null ? null
				: accessories[0]);

		if (accessory != null) {
			if (usbManager.hasPermission(accessory)) {
				debug("connectToAccessory: has permission => openAccessory");
				openAccessory(accessory);
			} else {
				if (!requestingPermission) {
					debug("connectToAccessory: no permission => requestPermission");
					requestingPermission = true;
					usbManager.requestPermission(accessory, permissionIntent);
				} else {
					debug("connectToAccessory: requesting permission => skip");
				}
			}
		} else {
			debug("connectToAccessory: no accessories found");
		}
	}

	public void debug(final String msg) {
		//txtDebug.append(msg + "\n");
		System.out.println(msg);
	}

	public void disconnectFromAccessory() {
		try {
			if (fileDescriptor != null) {
				sendCommand(CMD_LETMEGO);

				fileDescriptor.close();

				debug("Disconnected from accessory");
			}
		} catch (IOException e) {
		} finally {
			usbAccessory = null;
			fileDescriptor = null;
			accessoryInput = null;
			accessoryOutput = null;
			connected = false;
		}
	}
	public void openAccessory(UsbAccessory accessory) {
		fileDescriptor = usbManager.openAccessory(accessory);
		if (fileDescriptor != null) {
			this.usbAccessory = accessory;
			final FileDescriptor fd = fileDescriptor.getFileDescriptor();

			accessoryInput = new FileInputStream(fd);
			accessoryOutput = new FileOutputStream(fd);
			connected = true;
			final Thread inputThread = new Thread(new Runnable() {

				@Override
				public void run() {
					byte[] buffer = new byte[READ_BUFFER_SIZE];
					int readBytes = 0;
					while (readBytes >= 0) {
						try {
							handler.post(new Runnable() {
								@Override
								public void run() {
									debug("read bytes...");
								}
							});
							readBytes = accessoryInput.read(buffer);
							final String reply = new String(buffer);

							final String postMessage = "Read: " + "num bytes="
									+ readBytes + ", value="
									+ new String(buffer);
							answer = new String(buffer).trim();
							//answer = answer.substring(0, answer.length()-1);
							buffer = new byte[READ_BUFFER_SIZE];
							waitingforanswer = false;
							handler.post(new Runnable() {
								@Override
								public void run() {
									debug(postMessage);

//									Toast.makeText(usethis,
//											"input " + postMessage, Toast.LENGTH_SHORT)
//											.show();
								}
							});

							if (REPLY_GETOUT.equals(reply)) {
								break;
							}
						} catch (final Exception e) {
							handler.post(new Runnable() {
								@Override
								public void run() {
									debug("Accessory read error: "
											+ e.getMessage());
								}
							});
							e.printStackTrace();
							break;
						}
					}
					handler.post(new Runnable() {
						@Override
						public void run() {
							debug("Input reader thread finish");
						}
					});
				}
			});
			inputThread.start();

			debug("openAccessory: connected accessory: manufacturer="
					+ usbAccessory.getManufacturer() + ", model="
					+ usbAccessory.getModel());
		} else {
			debug("openAccessory: Failed to open accessory");
		}
	}

	public synchronized String sendCommand(String command) {
		return sendCommand( command, 250);
	}
	
	public synchronized String sendCommand(String command, int timeout_ms) {
		if (accessoryOutput != null) {
			try {
				debug("Write: " + command);
		        long start = System.currentTimeMillis();

				accessoryOutput.write(command.getBytes());
				accessoryOutput.flush();
				waitingforanswer = true;
				while(waitingforanswer) {
					if (System.currentTimeMillis()-start > timeout_ms) {
						break;
					}
				}
				return answer;
			} catch (IOException e) {
				debug("Write error: " + e.getMessage());
				e.printStackTrace();
			}
		}
		return "";
	}

	public void resumeconnect() {
		if (usbAccessory == null) {
			final UsbAccessory[] accessories = usbManager.getAccessoryList();
			final UsbAccessory accessory = (accessories == null ? null
					: accessories[0]);
			connectToAccessory();
		}
	}



}
