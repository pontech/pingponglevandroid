/************************************************************************/
/*																		*/
/*	chipKITUSBAndroidHost.cpp	-- USB Android Host Class                       */
/*                         Android Host Class thunk layer to the MAL        */
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

#include "chipKITUSBHost.h"
#include "chipKITUSBAndroidHost.h"

//******************************************************************************
//******************************************************************************
// Thunks to the Generic USB HOST code in the MAL
//******************************************************************************
//******************************************************************************


void ChipKITUSBAndroidHost::AppStart(ANDROID_ACCESSORY_INFORMATION* accessoryInfo)
{
    AndroidAppStart(accessoryInfo);
}

void ChipKITUSBAndroidHost::Tasks(void)
{
    AndroidTasks();
}

uint8_t ChipKITUSBAndroidHost::AppWrite(void* handle, uint8_t* data, DWORD size)
{
    return(AndroidAppWrite(handle, data, size));
}

BOOL ChipKITUSBAndroidHost::AppIsWriteComplete(void* handle, uint8_t* errorCode, DWORD* size)
{
    return(AndroidAppIsWriteComplete(handle, errorCode, size));
}

uint8_t ChipKITUSBAndroidHost::AppRead(void* handle, uint8_t* data, DWORD size)
{
    return(AndroidAppRead(handle, data, size));
}

BOOL ChipKITUSBAndroidHost::AppIsReadComplete(void* handle, uint8_t* errorCode, DWORD* size)
{
    return(AndroidAppIsReadComplete(handle, errorCode, size));
}

BOOL ChipKITUSBAndroidHost::AppInitialize( uint8_t address, DWORD flags, uint8_t clientDriverID )
{
    return(AndroidAppInitialize(address, flags, clientDriverID));
}

BOOL ChipKITUSBAndroidHost::AppEventHandler( uint8_t address, USB_EVENT event, void *data, DWORD size )
{
    return(AndroidAppEventHandler(address, event, data, size));
}

BOOL ChipKITUSBAndroidHost::AppDataEventHandler( uint8_t address, USB_EVENT event, void *data, DWORD size )
{
    return(AndroidAppDataEventHandler(address, event, data, size));
}

uint8_t ChipKITUSBAndroidHost::AppHIDSendEvent(uint8_t address, uint8_t id, uint8_t* report, uint8_t length)
{
    return(AndroidAppHIDSendEvent(address, id, report, length));
}

BOOL ChipKITUSBAndroidHost::AppHIDRegister(uint8_t address, uint8_t id, uint8_t* descriptor, uint8_t length)
{
    return(AndroidAppHIDRegister(address, id, descriptor, length));
}


//******************************************************************************
//******************************************************************************
// Instantiate the Android Class
//******************************************************************************
//******************************************************************************
ChipKITUSBAndroidHost USBAndroidHost;

