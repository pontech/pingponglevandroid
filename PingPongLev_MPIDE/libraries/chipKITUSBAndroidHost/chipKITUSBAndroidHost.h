/************************************************************************/
/*																		*/
/*	chipKITUSBAndroidHost.h	-- USB Android Host Class                           */
/*                         Generic Host Class thunk layer to the MAL        */
/*																		*/
/************************************************************************/
/*	Author: 	Keith Vogel 											*/
/*	Copyright 2011, Digilent Inc.										*/
/************************************************************************/
/*
  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this library; if not, write to the Free Software
  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
/************************************************************************/
/*  Module Description: 												*/
/*  Just a class wrapper of the MAL Android HOST code                       */
/*																		*/
/************************************************************************/
/*  Revision History:													*/
/*																		*/
/*	9/06/2011(KeithV): Created											*/
/*																		*/
/************************************************************************/
#ifndef _CHIPKITUSBANDROIDHOSTCLASS_H
#define _CHIPKITUSBANDROIDHOSTCLASS_H

#ifdef __cplusplus
     extern "C"
    {
    #undef BYTE             // Arduino defines BYTE as 0, not what we want for the MAL includes
    #define BYTE uint8_t    // for includes, make BYTE something Arduino will like     
#else
    #define uint8_t BYTE    // in the MAL .C files uint8_t is not defined, but BYTE is correct
#endif

// must have previously included ChipKITUSBHost.h in all .C or .CPP files that included this file
#include "USB/usb_host_android.h"

#ifdef __cplusplus
    #undef BYTE
    #define BYTE 0      // put this back so Arduino Serial.print(xxx, BYTE) will work.
    // also replace all BYTE data types usages with uint8_t
    }
#endif

#ifdef __cplusplus

    class ChipKITUSBAndroidHost 
    {
    public:
        void AppStart(ANDROID_ACCESSORY_INFORMATION* accessoryInfo);
        void Tasks(void);
        uint8_t AppWrite(void* handle, uint8_t* data, DWORD size);
        BOOL AppIsWriteComplete(void* handle, uint8_t* errorCode, DWORD* size);
        uint8_t AppRead(void* handle, uint8_t* data, DWORD size);
        BOOL AppIsReadComplete(void* handle, uint8_t* errorCode, DWORD* size);
        BOOL AppInitialize( uint8_t address, DWORD flags, uint8_t clientDriverID );
        BOOL AppEventHandler( uint8_t address, USB_EVENT event, void *data, DWORD size );
        BOOL AppDataEventHandler( uint8_t address, USB_EVENT event, void *data, DWORD size );
        uint8_t AppHIDSendEvent(uint8_t address, uint8_t id, uint8_t* report, uint8_t length);
        BOOL AppHIDRegister(uint8_t address, uint8_t id, uint8_t* descriptor, uint8_t length);
    };

// pre-instantiated Class for the sketches
extern ChipKITUSBAndroidHost USBAndroidHost;

#endif
#endif
