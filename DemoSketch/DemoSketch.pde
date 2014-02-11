char buff[0x20];//buffer to hold input
char ctr; //keeps track of position in buffer
char ch; //holds last character received
void setup() {               
  Serial.begin(115200);
}
void loop() {
  if (Serial.available() > 0)
  {
    ch = Serial.read();
    if( ctr < sizeof(buff)) { //don't over run the buffer
      buff[ctr++] = ch;
    }
    if (ch == '\r') // when I get a carriage return
    {
      buff[ctr-1] = 0; //replace the carriage return in the buffer with 0 which
                       //terminates strings
      ctr = 0; //reset the buffer pointer
      if (strncmp(buff, "pinmode ",8) == 0)
      {
        char* point = buff+8;
        unsigned char pin = parseNumber(point);
        point += findSpaceOffset(point);
        unsigned char mode = parseNumber(point);
        pinMode(pin, mode);
      }
      else if (strncmp(buff, "digitalwrite ",13) == 0)
      {
        char* point = buff+13;
        unsigned char pin = parseNumber(point);
        point += findSpaceOffset(point);
        unsigned char val = parseNumber(point);
        digitalWrite(pin, val);
      }
      else if (strncmp(buff, "digitalread ",12) == 0)
      {
        char* point = buff+12;
        unsigned char pin = parseNumber(point);
        Serial.println(digitalRead(pin),DEC);
      }
      else if (strncmp(buff, "analogread ",11) == 0)
      {
        char* point = buff+11;
        unsigned char pin = parseNumber(point);
        Serial.println(analogRead(pin),DEC);
      }
      else if (strcmp(buff, "reset") == 0)
      {
        Serial.println("Close serial terminal, resetting board in..."); 
        unsigned char sec = 5; 
        while(sec >= 1) 
        { 
          Serial.print(sec, DEC); 
          Serial.println(" seconds..."); 
          delay(1000); 
          sec--; 
        }
        Reset();
      }
    } // end of if I get a carriage return
  } //end of serial available
}
unsigned int parseNumber(char* s)
{
  unsigned char i = 0;
  unsigned int out = 0;
  while(s[i] != 0 && s[i] != ' ')
    out = out * 10 + (s[i++]-'0');
  return out;
}
unsigned char findSpaceOffset(char* s)
{
  unsigned char i = 0;
  while(s[i] != 0 && s[i] != ' ')
    i++;
  return i+1;
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

