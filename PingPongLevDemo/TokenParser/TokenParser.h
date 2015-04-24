#ifndef TOKENPARSER_H
#define TOKENPARSER_H

#include "WProgram.h"
#include "../pic32lib/core.h"

class TokenParser {
public:
    TokenParser(us8* newBuffer, us8 newLength) {
        buffer = newBuffer;
        length = newLength;
        head = 0;
        tail = 0;
    }

    boolean compare(const char* string, char wild = '?' )
    {
      us8 i = 0;
      us8 temp = tail;
      while(string[i] != 0) {
        if(string[i] != wild) {
            if((buffer[temp]>0x60 ? buffer[temp]-0x20 : buffer[temp]) != string[i]) {
              return false;
            }
        }
        i++;
        temp++;
      }
      return true;
    }

    int replace(char a, char b )
    {
      us8 i = 0;
      int count = 0;
      while(buffer[i] != 0) {
        if(buffer[i] == a) {
	  buffer[i] = b;
	  count++;
        }
        i++;
      }
      return count;
    }

// todo: enforce consecutive charactors
    boolean contains(const char* string)
    {
        us8 i = 0;
        us8 index = tail;
        while(string[i] != 0 && index < head) {
            if(buffer[index++] == string[i]) {
                i++;
                if(string[i] == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    void print()
    {
        Serial.print(tail, DEC);
        Serial.print(" - ");
        Serial.println(head, DEC);

        Serial.print("'");
        for(us8 i = tail; i <= head; i++) {
          Serial.print(buffer[i]);
        }
        Serial.print("'");
        Serial.println();
    }

    boolean nextToken()
    {
        tail = head;
        tail = skip(tail, true);

        head = tail;
        head = skip(tail, false);

        return (tail < head) ? true : false;
    }

    boolean advanceTail(us8 advance)
    {
        if(advance < (head - tail)) {
            tail += advance;
            return true;
        }
        return false;
    }


    us8 hexCharToNibble(us8 c) {
        // make upper case
        if('a' <= c && c <= 'z') {
            c -= 0x20;
        }

        if('0' <= c && c <= '9') {
            c -= 0x30;
        }
        else if('A' <= c && c <= 'F') {
            c -= 0x37;
        }
        return c & 0xf;
    }

    e16 to_e16()
    {
	bool neg = false;
        e16 temp;
        temp.value = 0;
        temp.exp = 0;
	if(contains("-")) { // todo: change to startsWith("-")
            advanceTail(1);
	    neg = true;
	}
        // todo: change to startsWith("0x")
        if(contains("0x")) { // HEX
            advanceTail(2);
            us8 index = tail;
            do {
                temp.value <<= 4;
                temp.value |= hexCharToNibble(buffer[index++]);
            } while(index < head);
        }
        else if(contains("e")) { // Exp.Notation
            Serial.println("Is Exp.Notation");

        }
        else if(contains(".")) { // REAL
            us16 base = pow(10, (head - tail - 2));

            boolean ad = false; // after decimal, of course...
            us8 index = tail;
            while(index < head) {
                us8 data = buffer[index++];
                if(data == '.') {
                    ad = true;
                }
                else {
                    temp.value += (data - 0x30) * base;
                    base /= 10;

                    if(ad) {
                        temp.exp--;
                    }
                }
            }
        }
        else { // WHOLE
            us16 base;
            while(tail < head) {
                base = pow(10, head - tail - 1);
                temp.value += (buffer[tail] - 0x30) * base;
                tail++;
            }
        }
        if (neg)
	  temp.value *=-1;
	
        return temp;
    }
    e32 to_e32()
    {
	bool neg = false;
        e32 temp;
        temp.value = 0;
        temp.exp = 0;
	if(contains("-")) { // todo: change to startsWith("-")
            advanceTail(1);
	    neg = true;
	}
        // todo: change to startsWith("0x")
        if(contains("0x")) { // HEX
            advanceTail(2);
            us8 index = tail;
            do {
                temp.value <<= 4;
                temp.value |= hexCharToNibble(buffer[index++]);
            } while(index < head);
        }
        else if(contains("e")) { // Exp.Notation
            Serial.println("Is Exp.Notation");

        }
        else if(contains(".")) { // REAL
            us32 base = pow(10, (head - tail - 2));

            boolean ad = false; // after decimal, of course...
            us8 index = tail;
            while(index < head) {
                us8 data = buffer[index++];
                if(data == '.') {
                    ad = true;
                }
                else {
                    temp.value += (data - 0x30) * base;
                    base /= 10;

                    if(ad) {
                        temp.exp--;
                    }
                }
            }
        }
        else { // WHOLE
            us32 base;
            while(tail < head) {
                base = pow(10, head - tail - 1);
                temp.value += (buffer[tail] - 0x30) * base;
                tail++;
            }
        }
        if (neg)
	  temp.value *=-1;
	
        return temp;
    }

    us8 head;
    us8 tail;
private:
    us8 skip(us8 index, boolean whitespace)
    {
        while(0 <= index && index <= length) {
            if(whitespace) {
                if(buffer[index] != 0x20) {
                    break;
                }
            }
            else {
                if(buffer[index] == 0x20) {
                    break;
                }
            }
            index++;
        }

        if(index > length) {
            return 0;
        }
        return index;
    }

    us8* buffer;
    us8 length;
};
#endif // TOKENPARSER_H
