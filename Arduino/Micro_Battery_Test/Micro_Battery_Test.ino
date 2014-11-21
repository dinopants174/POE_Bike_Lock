/*********************************************************************
This is an example for our nRF8001 Bluetooth Low Energy Breakout

  Pick one up today in the adafruit shop!
  ------> http://www.adafruit.com/products/1697

Adafruit invests time and resources providing this open source code, 
please support Adafruit and open-source hardware by purchasing 
products from Adafruit!

Written by Kevin Townsend/KTOWN  for Adafruit Industries.
MIT license, check LICENSE for more information
All text above, and the splash screen below must be included in any redistribution
*********************************************************************/

// This version uses the internal data queing so you can treat it like Serial (kinda)!

//send information to Phone from Arduino sucessfully
//RSSI signal filtering

#include <SPI.h>
#include "Adafruit_BLE_UART.h"
#include <Servo.h>

// Connect CLK/MISO/MOSI to hardware SPI
// e.g. On UNO & compatible: CLK = 13, MISO = 12, MOSI = 11
#define ADAFRUITBLE_REQ 10
#define ADAFRUITBLE_RDY 2     // This should be an interrupt pin, on Uno thats #2 or #3
#define ADAFRUITBLE_RST 9

Adafruit_BLE_UART BTLEserial = Adafruit_BLE_UART(ADAFRUITBLE_REQ, ADAFRUITBLE_RDY, ADAFRUITBLE_RST);

Servo servo;
int pos = 10;
int desired_pos = 40;  //change this depending on where servo needs to go for unlock command
String phone_str = "";
const int rssi_unlock_val = -40;  //change this depending on rssi signal
const int rssi_lock_val = -90;    //change this depending on rssi signal

String lock_state = "";
String prev_lock_state = "";

/**************************************************************************/
/*!
    Configure the Arduino and start advertising with the radio
*/
/**************************************************************************/
void setup(void)
{ 
  //Serial.begin(9600);
  //while(!Serial); // Leonardo/Micro should wait for serial init
  //Serial.println(F("Adafruit Bluefruit Low Energy nRF8001 Print echo demo"));

  // BTLEserial.setDeviceName("NEWNAME"); /* 7 characters max! */
  BTLEserial.begin();
  servo.attach(8);
  
  servo.write(pos);
  delay(15);
  prev_lock_state = "l";
  
  //BTLEserial.print(prev_lock_state);
  //BTLEserial.print(";");
}

/**************************************************************************/
/*!
    Constantly checks for new events on the nRF8001
*/
/**************************************************************************/
aci_evt_opcode_t laststatus = ACI_EVT_DISCONNECTED;

void loop()
{
  BTLEserial.pollACI();

  aci_evt_opcode_t status = BTLEserial.getState();

  if (status != laststatus) {
    if (status == ACI_EVT_DEVICE_STARTED) {
        //Serial.println(F("* Advertising started"));
    }
    if (status == ACI_EVT_CONNECTED) {
        //Serial.println(F("* Connected!"));
    }
    if (status == ACI_EVT_DISCONNECTED) {
        //Serial.println(F("* Disconnected or advertising timed out"));
    }

    laststatus = status;
  }

  if (status == ACI_EVT_CONNECTED) {
    
    if (BTLEserial.available()) {
      //Serial.print("* "); Serial.print(BTLEserial.available()); Serial.println(F(" bytes available from BTLE"));
    }
    
    while (BTLEserial.available()) {
      char c = BTLEserial.read();
      //if (c != 'u' || c != 'l' || c!= 
      //Serial.print(c);
      
      //Serial.print(phone_str);
      //phone_str = BTLEserial.readString();
      //Serial.print(phone_str);
      
      if (c == 'u'){ 
        //Serial.println("I am unlocking becuase of app input");
        pos = desired_pos;
        lock_state = "u";
        }
      else if (c == 'l'){ 
        //Serial.println("I am locking because of app input");
        pos = 10;
        lock_state = "l";
        }
      else{
        if (c != ';'){
           phone_str += c;
          }
        else{
          if (phone_str.toInt() >= rssi_unlock_val){
          //Serial.println("I am unlocking because of RSSI");
          pos = desired_pos;
          lock_state = "u";
          }
          
        else if (phone_str.toInt() <= rssi_lock_val){
          //Serial.println("I am locking because of RSSI"); 
          pos = 10;
          lock_state = "l";
          }
          
          phone_str = "";  
        }
      
      servo.write(pos);
      delay(15);
      }

      if (lock_state != prev_lock_state){
        if (lock_state == ""){
          BTLEserial.print("l");
        }
        else{
        BTLEserial.print(lock_state);
        //BTLEserial.print(";");
        //Serial.println("");
        //Serial.print("I think the lock is currently: ");
        //Serial.print(lock_state);
        //Serial.println("");
        //Serial.println("");
        //Serial.print(";");
        prev_lock_state = lock_state;   
          }    
        }
      }
   }
}
