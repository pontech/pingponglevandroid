#include <chipKITUSBHost.h>
#include <chipKITUSBAndroidHost.h>
#include "pic32lib/core.h"
#include "TokenParser/TokenParser.h"
#include "pic32lib/DetectEdge.h"
#include "pic32lib/Cron.h"
#include <PID_v1.h>
// KardCxPx is a multi-dimensional array where the first
// index points the Kard slot and the second point to the
// GPIO pin on the Kard, the first four GPIO pins generaly
// connect to the rail pin Px.
#define WindowSize 1000
#define WantNewLine
#define basefromsensor 9

uint8_t KardCxPx[7][6] = {
                    { 68, 58, 62, 55, 82, 32 }, // Kard 0
                    { 57, 56, 63, 54, 83, 31 }, // Kard 1
                    { 86, 64,  5, 70, 84, 30 }, // Kard 2
                    { 22, 76,  9,  2, 35, 52 }, // Kard 3
                    { 23, 39,  8, 21, 34, 50 }, // Kard 4
                    { 78, 79, 10, 20, 33, 85 }, // Kard 5
                    { 44, 44, 44, 44, 44, 44 }, // Kard Com
                   };
char led[] = {37, 81};

Cron cron;

us8 buff[0x30];
us8 ctr;
us8 ch;
e16 num1;//temporary value to parse into
e32 num2;//temporary value to parse into
char spbuf[20];

bool Running = true;
double Input, Output;
double pid_p = 60;
double pid_i = 7;
double pid_d = 3.5;
double SetPoint = 5;
double position_inches_averaged = 0;
double position_inches_averaged_noisy = 0;
PID myPID(&Input, &Output, &SetPoint,pid_p, pid_i, pid_d, DIRECT);

static char manufacturer[] = "PONTECH";
static char model[] = "Demo";
static char description[] = "Ping Pong Levitation";
static char version[] = "1.0";
static char uri[] = "http://www.pontech.com/";
static char serial[] = "N/A";

ANDROID_ACCESSORY_INFORMATION myDeviceInfo = {
    manufacturer, sizeof(manufacturer),
    model, sizeof(model),
    description, sizeof(description),
    version, sizeof(version),
    uri, sizeof(uri),
    serial, sizeof(serial)
};

// Replyes for Android device
#define REPLY_OK "ok"
#define REPLY_GETOUT "getout"
#define REPLY_UNKNOWN_CMD "dontunderstand"
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
    // pins on events EVENT_VBUS_REQUEST_POWER Ð¸ EVENT_VBUS_RELEASE_POWER)
    fRet = USBHost.DefaultEventHandler(address, event, data, size);
  
    switch( event ) {
        case EVENT_ANDROID_DETACH:
            deviceAttached = FALSE;
            return TRUE;
            break;

        case EVENT_ANDROID_ATTACH:
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
    buffer[size] = ' '; //make string end with a space for token parser
    TokenParser tokpars((us8*) buffer,(us8) size);
    tokpars.nextToken();
    ctr = 0;
    if( tokpars.compare("RESET") ) {
      Serial1.print("Close serial terminal, resetting board in...");
      PrintCR();
      us8 sec;
      for( sec = 5; sec >= 1; sec-- ) {
        Serial1.print(sec, DEC);
        Serial1.print(" seconds...");
        PrintCR();
        delay(1000);
      }
      Reset();
    }
    else if( tokpars.compare("ON") ) {
      Running = true;
      strcpy(reply_buffer, "1");// Prepare reply
    }
    else if( tokpars.compare("OFF") ) {
      Running = false;
      strcpy(reply_buffer, "0");// Prepare reply
    }
    else if( tokpars.compare("READSETPOINT") ) {
      sprintf(spbuf,"%4.4f",SetPoint);
      strcpy(reply_buffer, spbuf);// Prepare reply
    }
    else if( tokpars.compare("SETSETPOINT") ) {
      tokpars.nextToken();
      num2 = tokpars.to_e32();
      SetPoint = (double)num2.value*pow(10,num2.exp);
      sprintf(spbuf,"%4.4f",SetPoint);
      strcpy(reply_buffer, spbuf);// Prepare reply
    }
    else if( tokpars.compare("READP") ) {
      sprintf(spbuf,"%4.4f",pid_p);
      strcpy(reply_buffer, spbuf);// Prepare reply
    }
    else if( tokpars.compare("SETP") ) {
      tokpars.nextToken();
      num2 = tokpars.to_e32();
      pid_p = (double)num2.value*pow(10,num2.exp);
      myPID.SetTunings(pid_p,pid_i,pid_d);
      sprintf(spbuf,"%4.4f",pid_p);
      strcpy(reply_buffer, spbuf);// Prepare reply
    }
    else if( tokpars.compare("READI") ) {
      sprintf(spbuf,"%4.4f",pid_i);
      strcpy(reply_buffer, spbuf);// Prepare reply
    }
    else if( tokpars.compare("SETI") ) {
      tokpars.nextToken();
      num2 = tokpars.to_e32();
      pid_i = (double)num2.value*pow(10,num2.exp);
      myPID.SetTunings(pid_p,pid_i,pid_d);
      sprintf(spbuf,"%4.4f",pid_i);
      strcpy(reply_buffer, spbuf);// Prepare reply
    }
    else if( tokpars.compare("READD") ) {
      sprintf(spbuf,"%4.4f",pid_d);
      strcpy(reply_buffer, spbuf);// Prepare reply
    }
    else if( tokpars.compare("SETD") ) {
      tokpars.nextToken();
      num2 = tokpars.to_e32();
      pid_d = (double)num2.value*pow(10,num2.exp);
      myPID.SetTunings(pid_p,pid_i,pid_d);
      sprintf(spbuf,"%4.4f",pid_d);
      strcpy(reply_buffer, spbuf);// Prepare reply
    }
    else if( tokpars.compare("READSTATUS") ) {
      sprintf(spbuf,"%4.4f %4.4f %4.4f",basefromsensor-position_inches_averaged_noisy,SetPoint,Output);
      strcpy(reply_buffer, spbuf);// Prepare reply
    }
    else if( tokpars.compare("LETMEGO") ) {
      strcpy(reply_buffer, REPLY_GETOUT); // Prepare reply
    }
    else {
      strcpy(reply_buffer, REPLY_UNKNOWN_CMD); // Prepare reply
    }
    replySize = strlen(reply_buffer) + 1;
    return replySize;
}

void ProcessHost() {
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
//                Serial1.print("Error trying to complete read: errorCode=");
//                Serial1.println(errorCode, HEX);
            }
        }
        
        // Send data to Android device
        if(write_size > 0 && !writeInProgress) {
//            Serial1.print("Write: ");
//            Serial1.print(write_buffer);
//            Serial1.println();
          
            writeSize = write_size;
            // Require command is already in the buffer to be sent.
            // Call is not blocked - will check if write is complete with AndroidAppIsWriteComplete
            errorCode = USBAndroidHost.AppWrite(deviceHandle, (uint8_t*)&write_buffer, (DWORD) writeSize);
                        
            if(errorCode == USB_SUCCESS) {
                writeInProgress = TRUE;
            } else {
                Serial1.print("Error trying to complete read: errorCode=");
                Serial1.println(errorCode, HEX);
                
                write_size = 0;
            }
        }
        
        if(writeInProgress) {
            // Let's check if write is complete
            if(USBAndroidHost.AppIsWriteComplete(deviceHandle, &errorCode, &writeSize)) {
                writeInProgress = FALSE;
                write_size = 0;
    
                if(errorCode != USB_SUCCESS) {
//                    Serial1.print("Error trying to complete read: errorCode=");
//                    Serial1.println(errorCode, HEX);
                }
            }
        }
    }
}

void setup() {
  Serial1.begin(115200);
  pinMode(KardCxPx[0][0], OUTPUT);
  pinMode(KardCxPx[0][1], OUTPUT);
  pinMode(PIN_LED1, OUTPUT);
  pinMode(PIN_LED2, OUTPUT);
  digitalWrite(KardCxPx[0][0], LOW);
  digitalWrite(KardCxPx[0][1], LOW);
  cron.add(flash);
  ConfigIntTimer3(T3_INT_ON | T3_INT_PRIOR_3);
  OpenTimer3(T3_ON | T3_PS_1_32, 25);

  myPID.SetOutputLimits(0, WindowSize);
  //turn the PID on
  myPID.SetMode(AUTOMATIC);

  // Init USB Host controller:
  // Pass the instance for event handler
  USBHost.Begin(USBEventHandlerApplication);
  // Send info about device to Android driver
  USBAndroidHost.AppStart(&myDeviceInfo);
}
 
void loop() {
  ProcessHost();
  cron.scheduler();
  double duration, inches, cm;
  duration = readDistanceSensor(KardCxPx[3][3]);
  // convert the time into a distance
  inches = microsecondsToInches(duration);
  if(inches>0) {
    position_inches_averaged = (position_inches_averaged * 0.999) + (inches * 0.001);
    position_inches_averaged_noisy = (position_inches_averaged * 0.6) + (inches * 0.4);
  }
  Input = basefromsensor-position_inches_averaged;
  
  if(Running) {
    myPID.Compute();
  }
  if (Serial1.available() > 0)//  if (Serial.available() > 0 || Serial1.available() > 0)
  {
    ch = Serial1.read();
    if( ctr < sizeof(buff)) {
      buff[ctr++] = ch;
    }
    if (ch == '\r' || ch == '\n')
    {
      buff[ctr-1] = ' ';
      char outbuf[20];
      us8 sizeout = processInput( (char*)buff, ctr-1, outbuf);
      if (sizeout>0) {
        Serial1.println(outbuf);
      }
      ctr = 0;
    }
  }
}

void flash() {
  Cron::CronDetail *self = cron.self();

  digitalWrite(PIN_LED1, self->temp);
  digitalWrite(PIN_LED2, !self->temp);

  self->temp ^= 1;
  self->yield = millis() + 1000;
}

void Reset()
{
#ifdef VIRTUAL_PROGRAM_BUTTON_TRIS
  VIRTUAL_PROGRAM_BUTTON_TRIS = 0; //Set virtual button as output
  VIRTUAL_PROGRAM_BUTTON = 1; //push virtual button
#endif
  SYSKEY = 0x00000000; //write invalid key to force lock
  SYSKEY = 0xAA996655; //write key1 to SYSKEY
  SYSKEY = 0x556699AA; //write key2 to SYSKEY // OSCCON is now unlocked
  RSWRSTSET = 1; //set SWRST bit to arm reset
  unsigned int dummy;
  dummy = RSWRST; //read RSWRST register to trigger reset
  while(1); //prevent any unwanted code execution until reset occurs
}

void PrintCR() {
  #ifdef WantNewLine
  Serial1.print("\r\n");
  #else
  Serial1.print("\r");
  #endif
}

double readDistanceSensor(char pingPin)
{
  delay(2);
  // The PING))) is triggered by a HIGH pulse of 2 or more microseconds.
  // Give a short LOW pulse beforehand to ensure a clean HIGH pulse:
  pinMode(pingPin, OUTPUT);
  digitalWrite(pingPin, LOW);
  delayMicroseconds(2);
  digitalWrite(pingPin, HIGH);
  delayMicroseconds(5);
  digitalWrite(pingPin, LOW);

  // The same pin is used to read the signal from the PING))): a HIGH
  // pulse whose duration is the time (in microseconds) from the sending
  // of the ping to the reception of its echo off of an object.
  pinMode(pingPin, INPUT);
  return pulseIn(pingPin, HIGH);
}

double microsecondsToInches(double microseconds)
{
  // According to Parallax's datasheet for the PING))), there are
  // 73.746 microseconds per inch (i.e. sound travels at 1130 feet per
  // second).  This gives the distance travelled by the ping, outbound
  // and return, so we divide by 2 to get the distance of the obstacle.
  // See: http://www.parallax.com/dl/docs/prod/acc/28015-PING-v1.3.pdf
  return microseconds / 73.746 / 2;
}

double microsecondsToCentimeters(double microseconds)
{
  // The speed of sound is 344.8 m/s or 29.00232019 microseconds per centimeter.
  // The ping travels out and back, so to find the distance of the
  // object we take half of the distance travelled.
  return microseconds / 29.00232019 / 2;
}

extern "C"
{
  void __ISR(_TIMER_3_VECTOR,ipl3) pwmOn(void)
  {
    static us32 counter = 0;
    static bool outholdval = false;
    us8 i;
    mT3ClearIntFlag();  // Clear interrupt flag
    if (Running) //Running
    {
      if (counter < (us32)Output)
      {
        if(!outholdval){
          outholdval = true;
          digitalWrite(KardCxPx[0][0],HIGH);
          digitalWrite(KardCxPx[0][1],HIGH);
        }
      }
      else
      {
        if(outholdval){
          outholdval = false;
          digitalWrite(KardCxPx[0][0],LOW);
          digitalWrite(KardCxPx[0][1],LOW);
        }
      }
      counter++;
      if (counter>=WindowSize)
      {
        counter=0;
      }
    }
    else
    {
          digitalWrite(KardCxPx[0][0],LOW);
          digitalWrite(KardCxPx[0][1],LOW);
    }
  }
} // end extern "C"
