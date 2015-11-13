#include <HX711.h>
#include <SPI.h>
#include "Adafruit_BLE_UART.h"


//HX711 hx(9, 10, 128, 0.00127551);
HX711 hx(8, 7, 128, -0.0025);

// Connect CLK/MISO/MOSI to hardware SPI
// e.g. On UNO & compatible: CLK = 13, MISO = 12, MOSI = 11
#define ADAFRUITBLE_REQ 10
#define ADAFRUITBLE_RDY 2     // This should be an interrupt pin, on Uno thats #2 or #3
#define ADAFRUITBLE_RST 9

Adafruit_BLE_UART BTLEserial = Adafruit_BLE_UART(ADAFRUITBLE_REQ, ADAFRUITBLE_RDY, ADAFRUITBLE_RST);



void setup() {
  Serial.begin(9600);
  hx.tare(10); 
  BTLEserial.begin();
}
aci_evt_opcode_t laststatus = ACI_EVT_DISCONNECTED;

void loop() {
  
  double sum0 = 0;
  double sum1 = 0;
  for (int i = 0; i < 10; i++) {
    sum0 += hx.read();
    sum1 += hx.bias_read();
  }

  sendbluetooth(sum1/10);
  Serial.println(sum1/10);

  
}


void sendbluetooth(float data){
  BTLEserial.pollACI();

  // Ask what is our current status
  aci_evt_opcode_t status = BTLEserial.getState();
  if (status == ACI_EVT_CONNECTED) {
      String stringVal=String(data,0);
      uint8_t sendbuffer[20];
      stringVal.getBytes(sendbuffer, 20);
      char sendbuffersize = min(20, stringVal.length());

      Serial.print(F("\n* Sending -> \""));
      Serial.print((char *)sendbuffer); 
      Serial.println("\"");

      // write the data
      BTLEserial.write(sendbuffer, sendbuffersize);
  }
}

