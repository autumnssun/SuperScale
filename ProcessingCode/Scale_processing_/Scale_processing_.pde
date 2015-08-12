import processing.serial.*;
import controlP5.*;

JSONArray jsonAr;
ControlP5 cp5;
Textlabel f;
Textlabel w;
Textlabel c;
Textlabel t;
Textlabel s;

Textlabel foodLabel;
Textlabel weightLabel;
Textlabel caloriesLabel;
Textlabel servingsLabel;
Textlabel totalCalories;

Serial myPort;
final char newline= '\n';
String barcode="";
int lf = 10;      // ASCII linefeed
Float weight = 0.0;
JSONObject currentItem;
Float weightZeroOffset = 0.0;

@Override
  public void setup() {
  myPort=initPort();
  myPort.bufferUntil('\n');
  size(700, 400);
  cp5 = new ControlP5(this);
  setUpUI();
}

@Override
  public void draw() {
  drawUI();
}
void setUpUI() {
  f=cp5.addTextlabel("f").setText("Item:").setPosition(10, 20).setColorValue(0x000000).setFont(createFont("Helvetica", 30, true));
  w=cp5.addTextlabel("w").setText("Weight:").setPosition(10, 70).setColorValue(0x000000).setFont(createFont("Helvetica", 30, true));
  c=cp5.addTextlabel("c").setText("Cal:").setPosition(10, 120).setColorValue(0x000000).setFont(createFont("Helvetica", 30, true));
  s=cp5.addTextlabel("s").setText("Servings:").setPosition(10, 170).setColorValue(0x000000).setFont(createFont("Helvetica", 30, true));  
  t=cp5.addTextlabel("t").setText("Total:").setPosition(10, 220).setColorValue(0x000000).setFont(createFont("Helvetica", 30, true)); 

  foodLabel=cp5.addTextlabel("foodLabel").setText("Food name:").setPosition(150, 20).setColorValue(0x000000).setFont(createFont("Helvetica", 35, true));
  weightLabel=cp5.addTextlabel("weightLabel").setText("w").setPosition(150, 70).setColorValue(0x000000).setFont(createFont("Helvetica", 35, true));
  caloriesLabel=cp5.addTextlabel("caloriesLabel").setText("kcal").setPosition(150, 120).setColorValue(0x000000).setFont(createFont("Helvetica", 35, true));
  servingsLabel=cp5.addTextlabel("servingsLabel").setText("sers").setPosition(150, 170).setColorValue(0x000000).setFont(createFont("Helvetica", 35, true));
  totalCalories=cp5.addTextlabel("totalCalories").setText("tol").setPosition(150, 220).setColorValue(0x000000).setFont(createFont("Helvetica", 35, true));
}
void drawUI() {
  background(255);
}

public JSONObject getNutrition(String matchBarcode ) {
  jsonAr = loadJSONArray("foodData.json");
  for (int i=0; i<jsonAr.size(); i++) {
    JSONObject json= jsonAr.getJSONObject(i);
    String barcode = json.getString("barcode");
    if (barcode.equals(matchBarcode)) {
      return json;
    }
  }
  
  JSONObject nulljson = new JSONObject();
  nulljson.setString("name", "Product not recorgnized");
  nulljson.setString("barcode", "null");
  nulljson.setFloat("calories", 0);
  nulljson.setFloat("servingSize", 0);
  return null;
}

public Serial initPort() {
  for (int i=0; i< Serial.list().length; i++) {
    if (Serial.list()[i].startsWith("/dev/tty.usbmodem")) {
      return new Serial(this, Serial.list()[i], 9600);
    }
  }
  return null;
}

boolean noItemOnScale(Float weight) {
  return weight < 1.0;
}

void serialEvent(Serial myPort) {
  // read String from the serial port:
  String inString = myPort.readString();
  try {
    weight = Float.parseFloat(inString);
    Float zeroedWeight = weight - weightZeroOffset;
    if (zeroedWeight < 1 || noItemOnScale(weight)) {
      weightLabel.setText(str(0));
      caloriesLabel.setText(str(0));
      servingsLabel.setText(str((0)));

      if (noItemOnScale(weight)) {
        weightZeroOffset = 0.0;
        currentItem = null;
        foodLabel.setText("");
      }
      return;
    }

    weightLabel.setText(String.format("%.0f",(zeroedWeight)) + "g");
    if (currentItem == null) {
      return;
    }

    float calories = currentItem.getFloat("calories");
    float servingSize = currentItem.getFloat("servingSize");
    caloriesLabel.setText(String.format("%.0f", (calories/100*zeroedWeight)));
    //String.format("%.2f", floatValue);

    servingsLabel.setText(String.format("%.2f", (zeroedWeight/servingSize)));
  }
  catch(Exception e) {
    println("got an exception");
    println(e);
  }
}

public void readWhile() {
  while (myPort.available() > 0) {
    try {
      print("weight ");
      weight = Float.parseFloat((myPort.readString()));
      println(weight);
    } 
    catch(Exception e) {
      println("got an exception");
      println(e);
    }
    println(weight);
  }
}

@Override
  public void keyPressed() {
  barcode=barcode+key;
  if (key==newline) {
    String barcodeString = cleanNumber(barcode);
    currentItem = getNutrition(barcodeString);
    if (currentItem == null) {
     foodLabel.setText("Product not recognized");
     weightZeroOffset = weight;
     barcode="";
     return;
    }
    String name = currentItem.getString("name");
    foodLabel.setText(name);
    weightZeroOffset = weight;
    barcode="";
  }
}
public String cleanNumber(String value) {
  String str = value.replaceAll("[^A-Za-z0-9 ]", ""); //<>//
  str=str.replaceAll("ONE", "1");
  return str;
}