#include <chipKITUSBHost.h>
#include <chipKITUSBAndroidHost.h>

// Current accessory device info
static char manufacturer[] = "NNTU";
static char model[] = "Android accessory basic demo";
static char description[] = "Android accessory basic demo: accepts 'ledon' and 'ledoff' commands, sends back 'ok' as reply";
static char version[] = "1.0";
static char uri[] = "https://github.com/1i7/snippets";
static char serial[] = "N/A";

ANDROID_ACCESSORY_INFORMATION myDeviceInfo = {
    manufacturer, sizeof(manufacturer),
    model, sizeof(model),
    description, sizeof(description),
    version, sizeof(version),
    uri, sizeof(uri),
    serial, sizeof(serial)
};

// Commands from Android device
#define CMD_LEDON "ledon"
#define CMD_LEDOFF "ledoff"
#define CMD_LETMEGO "letmego"

// Replyes for Android device
#define REPLY_OK "ok"
#define REPLY_GETOUT "getout"
#define REPLY_UNKNOWN_CMD "dontunderstand"

// Test LED pin
#define LED_PIN 13

// Local variables
BOOL deviceAttached = FALSE;
static void* deviceHandle = NULL;

static char read_buffer[128];
static char write_buffer[128];
int write_size;
BOOL writeInProgress = FALSE;

BOOL USBEventHandlerApplication( uint8_t address, USB_EVENT event, void *data, DWORD size ) {
    BOOL fRet = FALSE;

    // Call event handler from base host controller
    // (this is important to be done, because it also turns on and off power on microcontroller
    // pins on events EVENT_VBUS_REQUEST_POWER и EVENT_VBUS_RELEASE_POWER)
    fRet = USBHost.DefaultEventHandler(address, event, data, size);
  
    switch( event ) {
        // События от драйвера Android
        case EVENT_ANDROID_DETACH:
            Serial.println("Device NOT attached");
            deviceAttached = FALSE;
            return TRUE;
            break;

        case EVENT_ANDROID_ATTACH:
            Serial.println("Device attached");
            deviceAttached = TRUE;
            deviceHandle = data;
            return TRUE;

        default :
            break;
    }
    return fRet;
}


/**
* Process input - parse string, execute command.
* @return size of reply in bytes (0 for no reply).
*/
int processInput(char* buffer, int size, char* reply_buffer) {
    int replySize = 0;
    reply_buffer[0] = 0;
    Serial.print("Read: ");
    Serial.println(buffer);
    // Turn led on on command "ledon", turn off on command "ledoff"
    if(strcmp(buffer, CMD_LEDON) == 0) {
        Serial.println("Command 'ledon': turn light on");
        
        // Execute command
        digitalWrite(LED_PIN, HIGH);
        
        // Prepare reply
        strcpy(reply_buffer, REPLY_OK);
        replySize = strlen(write_buffer) + 1;
    } else if (strcmp(buffer, CMD_LEDOFF) == 0) {
        Serial.println("Command 'ledoff': turn light off");
        
        // Execute command
        digitalWrite(LED_PIN, LOW);
        
        // Prepare reply
        strcpy(reply_buffer, REPLY_OK);
        replySize = strlen(write_buffer) + 1;
    } else if (strcmp(buffer, CMD_LETMEGO) == 0) {
        Serial.println("Command 'letmego': send 'getout' reply");
        
        // Prepare reply
        strcpy(reply_buffer, REPLY_GETOUT);
        replySize = strlen(write_buffer) + 1;
    } else {
        Serial.print("Unknown command: ");
        Serial.println(buffer);
        
        // Prepare reply
        strcpy(reply_buffer, REPLY_UNKNOWN_CMD);
        replySize = strlen(write_buffer) + 1;
    }
    
    return replySize;
}

void setup() {
    // Debug messages on serial port:
    Serial.begin(9600);
    Serial.println("Start android accessory demo");
  
    // Init USB Host controller:
    // Pass the instance for event handler
    USBHost.Begin(USBEventHandlerApplication);
    // Send info about device to Android driver
    USBAndroidHost.AppStart(&myDeviceInfo);

    // Pin for tests
    pinMode(LED_PIN, OUTPUT);
}

void loop() {
    DWORD readSize;
    BOOL readyToRead = TRUE;
    DWORD writeSize;
    uint8_t errorCode;
    
    // Run periodic tasks to keep USB stack alive and healthy.
    // Run at least once per cycle or when you need to update inner state of USB host controller.
    USBTasks();

    if(deviceAttached) {
        // Read data from Android device - wait for a command
        if(readyToRead) {
            // Call is not blocked - will check if read is complete with AndroidAppIsReadComplete
            errorCode = USBAndroidHost.AppRead(deviceHandle, (uint8_t*)&read_buffer, (DWORD)sizeof(read_buffer));
            if(errorCode == USB_SUCCESS) {
                // Received command - will not read the next one until all data is received,
                // will check for that in next loop iterations.
                readyToRead = FALSE;
            }
        }

        // Let's check if read is complete
        if(USBAndroidHost.AppIsReadComplete(deviceHandle, &errorCode, &readSize)) {
            if(errorCode == USB_SUCCESS) {
                // Data portion is read, let's add finishing zero to make it zero-terminated string.
                read_buffer[readSize] = 0;
                
                // and we can execute the command now, reply will go to write_buffer
                writeSize = processInput(read_buffer, readSize, write_buffer);
                
                // Allow to read next command
                readyToRead = TRUE;
                readSize = 0;
                
                // If writeSize is not 0, send back reply - init write procedure
                // for the next loop iteration (data is already inside write_buffer)
                write_size = writeSize;
            } else {
                Serial.print("Error trying to complete read: errorCode=");
                Serial.println(errorCode, HEX);
            }
        }
        
        // Send data to Android device
        if(write_size > 0 && !writeInProgress) {
            Serial.print("Write: ");
            Serial.print(write_buffer);
            Serial.println();
          
            writeSize = write_size;
            // Require command is already in the buffer to be sent.
            // Call is not blocked - will check if write is complete with AndroidAppIsWriteComplete
            errorCode = USBAndroidHost.AppWrite(deviceHandle, (uint8_t*)&write_buffer, writeSize);
                        
            if(errorCode == USB_SUCCESS) {
                writeInProgress = TRUE;
            } else {
                Serial.print("Error trying to complete read: errorCode=");
                Serial.println(errorCode, HEX);
                
                write_size = 0;
            }
        }
        
        if(writeInProgress) {
            // Let's check if write is complete
            if(USBAndroidHost.AppIsWriteComplete(deviceHandle, &errorCode, &writeSize)) {
                writeInProgress = FALSE;
                write_size = 0;
    
                if(errorCode != USB_SUCCESS) {
                    Serial.print("Error trying to complete read: errorCode=");
                    Serial.println(errorCode, HEX);
                }
            }
        }
    }
}

