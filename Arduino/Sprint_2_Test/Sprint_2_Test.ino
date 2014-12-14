//send information to Phone from Arduino sucessfully
//RSSI signal filtering

#include <SPI.h>
#include "Adafruit_BLE_UART.h"
#include <Servo.h>
#include <Average.h>
#include <EEPROM.h>

// Connect CLK/MISO/MOSI to hardware SPI
// e.g. On UNO & compatible: CLK = 13, MISO = 12, MOSI = 11
#define ADAFRUITBLE_REQ 10
#define ADAFRUITBLE_RDY 2     // This should be an interrupt pin, on Uno thats #2 or #3
#define ADAFRUITBLE_RST 9

int address = 0;
byte value;
int check;
int passcode;
byte eeprom_store = 0;
boolean first_connection;
boolean phone_secure = false;
boolean call_response_start = true;

Adafruit_BLE_UART BTLEserial = Adafruit_BLE_UART(ADAFRUITBLE_REQ, ADAFRUITBLE_RDY, ADAFRUITBLE_RST);

Servo servo;
int pos = 85;
int desired_pos = 70;  //change this depending on where servo needs to go for unlock command

String phone_str = "";
int rssi_val = 0;

Average<int> rssi_vals(5);
Average<int> diffs(5);

//int rssi_array[10];
int current_rssi_avg = 0;
int prev_rssi_avg = 0;
//int current_rssi_val = 0;
//int prev_rssi_val = 0;
int diff = 0;
int avg_diff = 0;
int i = 0;
const int rssi_unlock_val = -65;  //change this depending on rssi signal
const int rssi_lock_val = -75;    //change this depending on rssi signal
//int count = 0;

String lock_state = "";
String prev_lock_state = "l";

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
  
  for (i=0; i<= 10; i++){
    rssi_vals.push(0);
    diffs.push(0);
  }
  
  value = EEPROM.read(address);
  if (value == 255){//default value in EEPROM right here right now
    //yo, I need to take the first thing the phone sends me after my first connection
    first_connection = true;
    Serial.println("Yo I am connecting for first time");
  }
  else{
    first_connection = false; 
    Serial.println("Yo I am a connection veteran");
  }
  
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
        Serial.println(F("* Advertising started"));
    }
    if (status == ACI_EVT_CONNECTED) {
        Serial.println(F("* Connected!"));
    }
    if (status == ACI_EVT_DISCONNECTED) {
        Serial.println(F("* Disconnected or advertising timed out"));
    }    
    if (status == ACI_EVT_DEVICE_STARTED && laststatus == ACI_EVT_CONNECTED){
      phone_secure = false;
      call_response_start = true;
    }
    laststatus = status;
  }
  

  if (status == ACI_EVT_CONNECTED) {
    if (call_response_start){
    while (BTLEserial.available()){  //call response protocol
      char c = BTLEserial.read();
      Serial.println(c);
      Serial.println("Austin has successfully sent me the character 'a'");
      if (c == 'a'){
         BTLEserial.print("z");
         Serial.println("And I have responded to Austin with the character 'z'");
         call_response_start = false;
      }
     }
    }
    
    if (first_connection && !phone_secure && !call_response_start){ //take the passcode and store it in EEPROM
      while (BTLEserial.available()) {
         char c = BTLEserial.read();
         Serial.println(c);
         if (c != ':'){
           Serial.println("I am writing the passcode to my EEPROM nnow");
           EEPROM.write(address, c - '0');
           address = address+1;
//           if (address == 4){
//             address = 0;
//           }
         }
         else{
           Serial.println("Yo I have finished pairing and I am done writing to the EEPROM now. Phone is now secure.");
           first_connection = false;
           phone_secure = true;
           BTLEserial.print("f");
         }
      }
    }
    
   else if (!first_connection && !call_response_start){
      if (!phone_secure){
        while (BTLEserial.available()){
         char c = BTLEserial.read();
         if (c != ':'){
           phone_str += c;
           Serial.println(c);
         }
         else{
           check = phone_str.toInt();  //this is the passcode the phone has sent me
           Serial.print("You sent me: ");
           Serial.println(check);
           phone_str = "";
           address = 0;
           for (i=0; i<4; i++){
             value = EEPROM.read(address);
             phone_str += String(value);
             address = address + 1;
           }
           
           passcode = phone_str.toInt();
           phone_str = "";
           Serial.print("The passcode in my memory is: ");
           Serial.println(passcode);
           if (check == passcode){
             Serial.println("Fabulous, this is the phone I should give lock control too");
             phone_secure = true;
             BTLEserial.print("f");
           }
           else{
             Serial.println("GET THE FUCK OUT BITCH THIS AINT YO LOCK");
             phone_secure = false;
             call_response_start = true;
             BTLEserial.print("d");
             }
         }
       }
     }
   }
   
   if (phone_secure){ 
    while (BTLEserial.available()) {
      char c = BTLEserial.read();
      //if (c != 'u' || c != 'l' || c!= 
      //Serial.print(c);
      
      //Serial.print(phone_str);
      //phone_str = BTLEserial.readString();
      //Serial.print(phone_str);
      
      if (c == 'u'){ 
        Serial.println("I am unlocking becuase of app input");
        pos = desired_pos;
        lock_state = "u";
        }
      else if (c == 'l'){ 
        Serial.println("I am locking because of app input");
        pos = 85;
        lock_state = "l";
        }
      else{
        if (c != ';'){
           phone_str += c;
          }
        else{
          rssi_val = phone_str.toInt();
          //Serial.print("RSSI Value at this moment: ");
          //Serial.println(rssi_val);
          phone_str = "";
          
          current_rssi_avg = rssi_vals.rolling(rssi_val);
          
          diff = current_rssi_avg - prev_rssi_avg;
          //current_rssi_val = rssi_val; 
          //diff = current_rssi_val - prev_rssi_val;
//          Serial.println("--------------------------------");
//          Serial.print("Current RSSI Avg: ");
//          Serial.println(current_rssi_avg);
          
          //Serial.println("");
          //Serial.print("Previous RSSI Avg: ");
          //Serial.println(prev_rssi_avg);
          
//          Serial.println("");
          
          prev_rssi_avg = current_rssi_avg;
          //prev_rssi_val = current_rssi_val;
          
          //Serial.print("RSSI AVG: ");
          //Serial.println(current_rssi_avg);
          //Serial.print("RSSI DIFFERENCE: ");
          //Serial.println(diff);
          
          avg_diff = diffs.rolling(diff);
          
//          Serial.println("");
//          Serial.print("Avg Diff: ");
//          Serial.println(avg_diff);
//          Serial.println("");
       }
                      
       if (current_rssi_avg <= rssi_lock_val && avg_diff < 0){
             Serial.println("I am locking because of RSSI");
             pos = 85;
             lock_state = "l";
          }
            
          else if (current_rssi_avg >= rssi_unlock_val && avg_diff > 0){
              Serial.println("I am unlocking because of RSSI"); 
              pos = desired_pos;
              lock_state = "u";
           }
         
          //lock_state = "l";  
          servo.write(pos);
          delay(15);
        }
       }
       
     if (lock_state != prev_lock_state){
       if (lock_state == ""){
         lock_state = "l";
         prev_lock_state = "l";
         BTLEserial.print(lock_state);
        }
        else{
          BTLEserial.print(lock_state);
          //BTLEserial.print(";");
//          Serial.println("");
//          Serial.print("I think the lock is currently: ");
//          Serial.print(lock_state);
//          Serial.println("");
//          Serial.println("--------------------------------");     

//          Serial.println("");
          //Serial.print(";");
          prev_lock_state = lock_state;   
        }
    }
   }
   else{}
  }
}
   

