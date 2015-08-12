#include <HX711.h>

//HX711 hx(9, 10, 128, 0.00127551);
HX711 hx(9, 10, 128, -0.0025);
//-0.1966101695


const int buttonPin = 2;     // the number of the pushbutton pin
const int ledPin =  13;
int buttonState = 0;         // variable for reading the pushbutton status

void setup() {
  Serial.begin(9600);
  hx.tare(10);

  pinMode(ledPin, OUTPUT);      
  pinMode(buttonPin, INPUT);     

}
void loop() {
  checkbtn();
  
  double sum0 = 0;
  double sum1 = 0;
  for (int i = 0; i < 10; i++) {
    sum0 += hx.read();
    sum1 += hx.bias_read();
  }

  Serial.println(sum1/10);
  //delay(10);
}


void checkbtn(){
 buttonState = digitalRead(buttonPin);

  // check if the pushbutton is pressed.
  // if it is, the buttonState is HIGH:
  if (buttonState == HIGH) {     
    // turn LED on:    
    digitalWrite(ledPin, HIGH);
    hx.tare(10);
    delay();
  } 
  else {
    // turn LED off:
    digitalWrite(ledPin, LOW); 
  }
}


