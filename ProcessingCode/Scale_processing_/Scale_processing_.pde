import processing.serial.*;
Serial myPort;
  final char newline= '\n';
  String barcode="";
  int lf = 10;      // ASCII linefeed
  double weight = 0.0;

  
  @Override
  public void setup() {
    myPort=initPort();
    myPort.bufferUntil('\n');

  }
  public Serial initPort(){
    for (int i=0; i< Serial.list().length;i++){
      if(Serial.list()[i].startsWith("/dev/tty.usbmodem")){
        return new Serial(this,Serial.list()[i],9600);
      }
    }
    return null;
  }
  @Override
  public void draw() {
    
  }
  
  void serialEvent(Serial myPort) {
      // read String from the serial port:
      String inString = myPort.readString();
      try {
      weight = Double.parseDouble(inString);
      }catch(Exception e){
        println("got an exception");
        println(e);
      }
  }
  
  public void readWhile(){
    while (myPort.available() > 0) {
      try {
        print("weight ");
        weight = Double.parseDouble((myPort.readString()));
      } catch(Exception e) {
        println("got an exception");
        println(e);
      }
       println(weight);
    }
  }
  
  @Override
  public void keyPressed() {
    barcode=barcode+key;
    if(key==newline){
      String barcodeString = cleanNumber(barcode);
//      print(barcodeString);
      if(barcodeString.equals("9300675013406")) {
        print(weight * (43.0/100.0));
        println("calories");
      } else {
        println("I dont know this barrrcoooodeee");
        println(barcodeString);
      }
      barcode="";
    }
  }
  public String cleanNumber(String value){
    String str = value.replaceAll("[^A-Za-z0-9 ]", "");
    str=str.replaceAll("ONE", "1");
    return str;
  }