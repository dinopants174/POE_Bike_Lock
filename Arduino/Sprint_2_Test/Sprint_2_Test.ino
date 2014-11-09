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
const int rssi_unlock_val = -55;  //change this depending on rssi signal
const int rssi_lock_val = -80;    //change this depending on rssi signal

String lock_state = "";
String prev_lock_state = "";
//String sad_unlock_val = "-40";
//String sad_lock_string = "-90";

/**************************************************************************/
/*!
    Configure the Arduino and start advertising with the radio
*/
/**************************************************************************/
void setup(void)
{ 
  Serial.begin(9600);
  while(!Serial); // Leonardo/Micro should wait for serial init
  Serial.println(F("Adafruit Bluefruit Low Energy nRF8001 Print echo demo"));

  // BTLEserial.setDeviceName("NEWNAME"); /* 7 characters max! */
  
  BTLEserial.begin();
  servo.attach(8);
  
  servo.write(pos);
  delay(15);
  prev_lock_state = "l";
  
//  uint8_t sendbuffer[20];
//  prev_lock_state.getBytes(sendbuffer, 20);
//  char sendbuffersize = min(20, prev_lock_state.length());
//  Serial.print(F("\n* Sending -> \"")); Serial.print((char *)sendbuffer); Serial.println("\"");
//  BTLEserial.write(sendbuffer, sendbuffersize);  
}

/**************************************************************************/
/*!
    Constantly checks for new events on the nRF8001
*/
/**************************************************************************/
aci_evt_opcode_t laststatus = ACI_EVT_DISCONNECTED;

void loop()
{
  // Tell the nRF8001 to do whatever it should be working on.
  BTLEserial.pollACI();

  // Ask what is our current status
  aci_evt_opcode_t status = BTLEserial.getState();
  // If the status changed....
  if (status != laststatus) {
    // print it out!
    if (status == ACI_EVT_DEVICE_STARTED) {
        Serial.println(F("* Advertising started"));
    }
    if (status == ACI_EVT_CONNECTED) {
        Serial.println(F("* Connected!"));
    }
    if (status == ACI_EVT_DISCONNECTED) {
        Serial.println(F("* Disconnected or advertising timed out"));
    }
    // OK set the last status change to this one
    laststatus = status;
  }

  if (status == ACI_EVT_CONNECTED) {
    // Lets see if there's any data for us!
    
    if (BTLEserial.available()) {
      Serial.print("* "); Serial.print(BTLEserial.available()); Serial.println(F(" bytes available from BTLE"));
    }
    // OK while we still have something to read, get a character and print it out
    while (BTLEserial.available()) {
      char c = BTLEserial.read();
      Serial.print(c);
      
      
      //Serial.print(phone_str);
      //phone_str = BTLEserial.readString();
      //Serial.print(phone_str);
      
      if (c == 'u'){ 
        Serial.println("I am unlocking now");
        pos = desired_pos;
        //lock_state = "u";
      }
      else if (c == 'l'){ 
        Serial.println("TROLOLOL LOCKING FOREVER KTHX BYE");
        pos = 10;
        //lock_state = "l";
      }
      else{
        if (c != ';'){
           phone_str += c;
        }
        else{
          if (phone_str.toInt() >= rssi_unlock_val){
            Serial.println("I am unlocking because of RSSI");
            pos = desired_pos;
          }
          
          else if (phone_str.toInt() <= rssi_lock_val){
            Serial.println("I am locking because of RSSI"); 
            pos = 10;
          }
          
          phone_str = "";
        }
      }
      
      servo.write(pos);
      delay(15);
    }
  }
}
      
              //|| phone_str.toInt() <= rssi_lock_val){
              //|| phone_str.toInt() >=  rssi_unlock_val){
                

//      if (lock_state != prev_lock_state){
//        uint8_t sendbuffer[20];
//        lock_state.getBytes(sendbuffer, 20);
//        char sendbuffersize = min(20, lock_state.length());
//        Serial.print(F("\n* Sending -> \"")); Serial.print((char *)sendbuffer); Serial.println("\"");
//        BTLEserial.write(sendbuffer, sendbuffersize);
//        
//        prev_lock_state = lock_state;       
//      }
      

      
      
      
//      if (pos == desired_pos){
//        lock_state = "unlock";
//        uint8_t sendbuffer[20];
//        lock_state.getBytes(sendbuffer, 20);
//        char sendbuffersize = min(20, lock_state.length());
//        
//        Serial.print(F("\n* Sending -> \"")); Serial.print((char *)sendbuffer); Serial.println("\"");
//        BTLEserial.write(sendbuffer, sendbuffersize);
//      }
//      
//      else{
//        lock_state = "lock";
//        uint8_t sendbuffer[20];
//        lock_state.getBytes(sendbuffer, 20);
//        char sendbuffersize = min(20, lock_state.length());
//        
//        Serial.print(F("\n* Sending -> \"")); Serial.print((char *)sendbuffer); Serial.println("\"");
//        BTLEserial.write(sendbuffer, sendbuffersize);
//      }
      


    // Next up, see if we have any data to get from the Serial console

//    if (Serial.available()) {
//      // Read a line from Serial
//      Serial.setTimeout(100); // 100 millisecond timeout
//      String s = Serial.readString();
//
//      // We need to convert the line to bytes, no more than 20 at this time
//      uint8_t sendbuffer[20];
//      s.getBytes(sendbuffer, 20);
//      char sendbuffersize = min(20, s.length());
//
//      Serial.print(F("\n* Sending -> \"")); Serial.print((char *)sendbuffer); Serial.println("\"");
//
//      // write the data
//      BTLEserial.write(sendbuffer, sendbuffersize);
//    }
