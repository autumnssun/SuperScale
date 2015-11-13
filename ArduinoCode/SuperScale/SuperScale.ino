#include <HX711.h>

//HX711 hx(9, 10, 128, 0.00127551);
HX711 hx(8, 7, 128, -0.0025);
//-0.1966101695

int buttonState = 0;         // variable for reading the pushbutton status

void setup() {
  Serial.begin(9600);
  hx.tare(10); 

}
void loop() {
  
  double sum0 = 0;
  double sum1 = 0;
  for (int i = 0; i < 10; i++) {
    sum0 += hx.read();
    sum1 += hx.bias_read();
  }

  Serial.println(sum1/10);
  //delay(10);
}
