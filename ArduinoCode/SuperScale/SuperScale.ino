#include <HX711.h>
#include <SPI.h>
#include "Adafruit_BLE_UART.h"
#include <SoftwareSerial.h>

//HX711 hx(9, 10, 128, 0.00127551);
HX711 hx(8, 7, 128, -0.0025);
// Connect CLK/MISO/MOSI to hardware SPI
// e.g. On UNO & compatible: CLK = 13, MISO = 12, MOSI = 11
#define ADAFRUITBLE_REQ 10
#define ADAFRUITBLE_RDY 2     // This should be an interrupt pin, on Uno thats #2 or #3
#define ADAFRUITBLE_RST 9

Adafruit_BLE_UART BTLEserial = Adafruit_BLE_UART(ADAFRUITBLE_REQ, ADAFRUITBLE_RDY, ADAFRUITBLE_RST);
SoftwareSerial mySerial(3,6); // RX, TX
aci_evt_opcode_t laststatus = ACI_EVT_DISCONNECTED;

void setup() {
  Serial.begin(9600);
  mySerial.begin(9600);//Start software serail
  hx.tare(10);
  BTLEserial.begin();
}

void loop() { 
  double sum0 = 0;
  double sum1 = 0;
  for (int i = 0; i < 10; i++) {
    sum0 += hx.read();
    sum1 += hx.bias_read();
  }

  sendWeight(sum1/10);
  Serial.println(sum1/10);
  
  String barCode="";
  do{
    if(mySerial.available()){
    char chr=mySerial.read();
    String hex=String(chr,HEX);
    Serial.println(hex);
    if(hex=="a"){
      //Serial.print(barCode);
      String stringVal= "{\"b\":\""+barCode+"\"}\n";
      uint8_t sendbuffer[100];
      stringVal.getBytes(sendbuffer, 100);
      char sendbuffersize = min(100, stringVal.length());
      BTLEserial.write(sendbuffer, sendbuffersize);
      //Serial.print(barCode);
    }else{
      barCode=barCode+chr;
    }
    }
   }
  while (mySerial.available()) ;
  
}


void sendWeight(float data){
  BTLEserial.pollACI();
   
  // Ask what is our current status
  aci_evt_opcode_t status = BTLEserial.getState();
  if (status == ACI_EVT_CONNECTED) {
      String stringVal= "{\"w\":"+String(data,0)+"}\n";//converting float into string with no decimal number
      uint8_t sendbuffer[20];
      stringVal.getBytes(sendbuffer, 20);
      char sendbuffersize = min(20, stringVal.length());
      BTLEserial.write(sendbuffer, sendbuffersize);
      
  }
}

