#include <HX711.h>
#include <SPI.h>
#include "Adafruit_BLE_UART.h"
#include <SoftwareSerial.h>

//HX711 hx(9, 10, 128, 0.00127551);
HX711 hx(8, 7, 128, -0.0025);
// Connect CLK/MISO/MOSI to hardware SPI
// e.g. On UNO & compatible: CLK = 13, MISO = 12, MOSI = 11
int bluetoothTx = 3;  // TX-O pin of bluetooth mate, Arduino D2
int bluetoothRx = 2;  // RX-I pin of bluetooth mate, Arduino D3

SoftwareSerial bluetooth(bluetoothTx, bluetoothRx);

void setup() {
  Serial.begin(9600);
  
  hx.tare(10);
  
  bluetooth.begin(115200);  // The Bluetooth Mate defaults to 115200bps
  delay(100);  // Short delay, wait for the Mate to send back CMD
  bluetooth.println("U,9600,N");  // Temporarily Change the baudrate to 9600, no parity
  // 115200 can be too fast at times for NewSoftSerial to relay the data reliably
  bluetooth.begin(9600);  // Start bluetooth serial at 9600
  
}

void loop() { 
  double sum0 = 0;
  double sum1 = 0;
  
  for (int i = 0; i < 10; i++) {
    sum0 += hx.read();
    sum1 += hx.bias_read();
  }
    
  int weight=int(abs(sum1/10));
  //Serial.print(weight);
  if(weight>10){
    bluetooth.println("{\"w\":"+String(weight)+"}");
  }
  if(bluetooth.available()){
    Serial.print((char)bluetooth.read());
  }
}


