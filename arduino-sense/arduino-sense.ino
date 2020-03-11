#include <Arduino_LPS22HB.h>
#include <Arduino_HTS221.h>
#include <Arduino_APDS9960.h>
#include <ArduinoBLE.h>

 // BLE Sensor Service
BLEService sensorService("19B10000-E8F2-537E-4F6C-D104768A1214"); 

BLEUnsignedCharCharacteristic proximity_char("19B10000-E8F2-537E-4F6C-D104768A1215",
    BLERead | BLENotify);
BLEUnsignedLongCharacteristic rgbc_char("19B10000-E8F2-537E-4F6C-D104768A1216",  
    BLERead | BLENotify); 
BLEIntCharacteristic temp_char("19B10000-E8F2-537E-4F6C-D104768A1217", 
    BLERead | BLENotify); 
BLEUnsignedLongCharacteristic humid_char("19B10000-E8F2-537E-4F6C-D104768A1218", 
    BLERead | BLENotify); 
BLEUnsignedLongCharacteristic baro_char("19B10000-E8F2-537E-4F6C-D104768A1219", 
    BLERead | BLENotify); 
    
int old_prox = 0;
unsigned long old_rgbc = 0;
float old_humid = 0;
float old_temp = 0;
float old_baro = 0;
  
long previousMillis = 0;

#define USE_SERIAL 1

void setup() {
#ifdef USE_SERIAL  
  Serial.begin(9600);
  while (!Serial);
#endif

  pinMode(LED_BUILTIN, OUTPUT);
  
  if (!APDS.begin()) {
#ifdef USE_SERIAL     
    Serial.println("Error initializing APDS9960 sensor.");
#endif
    while (1);
  }

  if (!HTS.begin()) {
#ifdef USE_SERIAL     
    Serial.println("Failed to initialize humidity temperature sensor!");
#endif    
    while (1);
  }

  if (!BARO.begin()) {
#ifdef USE_SERIAL    
    Serial.println("Failed to initialize pressure sensor!");
#endif    
    while (1);
  }            

  // begin initialization
  if (!BLE.begin()) {
#ifdef USE_SERIAL     
    Serial.println("starting BLE failed!");
#endif
    while (1);
  }

  BLE.setLocalName("ArduinoSensors");
  BLE.setAdvertisedService(sensorService);
  sensorService.addCharacteristic(proximity_char); 
  sensorService.addCharacteristic(rgbc_char);
  sensorService.addCharacteristic(temp_char); 
  sensorService.addCharacteristic(humid_char);
  sensorService.addCharacteristic(baro_char);  
  BLE.addService(sensorService);
  proximity_char.writeValue(old_prox);
  rgbc_char.writeValue(old_rgbc);
  temp_char.writeValue((int)old_temp);
  humid_char.writeValue((int)old_humid);
  baro_char.writeValue((int)old_baro);  

  BLE.advertise();
#ifdef USE_SERIAL 
  Serial.println("Bluetooth device active, waiting for connections...");
#endif
}

void loop() {

  BLEDevice central = BLE.central();

  if (central) {
#ifdef USE_SERIAL     
    Serial.print("Connected to central: ");
    Serial.println(central.address());
#endif    
    digitalWrite(LED_BUILTIN, HIGH);

    while (central.connected()) {
      long currentMillis = millis();
      // if 1000ms have passed, update sensors:
      if (currentMillis - previousMillis >= 1000) {
        previousMillis = currentMillis;
        updateSensorLevels();
      }
      delay(1000);
    }
    
    digitalWrite(LED_BUILTIN, LOW);
#ifdef USE_SERIAL     
    Serial.print("Disconnected from central: ");
    Serial.println(central.address());
#endif    
  }
}

void updateSensorLevels() {
  int curr_prox = old_prox;
  unsigned long curr_rgbc = old_rgbc;
  float curr_temp = old_temp;
  float curr_humid = old_humid;
  float curr_baro = old_baro;
  int r = 0, g = 0, b = 0, c = 0;
  if (APDS.proximityAvailable()) {
    curr_prox = APDS.readProximity();
  }

  if (curr_prox != old_prox) {
#ifdef USE_SERIAL
    Serial.print("Proximity level is now: ");
    Serial.println(curr_prox);
#endif    
    proximity_char.writeValue(curr_prox);
    old_prox = curr_prox;
  }
  if (APDS.colorAvailable()) {
    APDS.readColor(r,g,b,c);
    curr_rgbc = (0xff & c) << 24 | (0xff & r) << 16 | (0xff & g) << 8 | (0xff & b); 
  }

  if (curr_rgbc != old_rgbc) {
#ifdef USE_SERIAL     
    Serial.print("color is now: 0x");
    Serial.println(curr_rgbc, HEX);
#endif    
    rgbc_char.writeValue(curr_rgbc);
    old_rgbc = curr_rgbc;
  }
  
  curr_temp = HTS.readTemperature();

  if (curr_temp != old_temp) {
#ifdef USE_SERIAL     
    Serial.print("Temp level is now: ");
    Serial.println(curr_temp);
#endif    
    temp_char.writeValue((int)(curr_temp * 1000));
    old_temp = curr_temp;
  }

  curr_humid = HTS.readHumidity();

  if (curr_humid != old_humid) {
#ifdef USE_SERIAL     
    Serial.print("Humid level is now: ");
    Serial.println(curr_humid);
#endif    
    humid_char.writeValue((unsigned long)(curr_humid * 1000));
    old_humid = curr_humid;
  }

  curr_baro = BARO.readPressure();

  if (curr_baro != old_baro) {
#ifdef USE_SERIAL     
    Serial.print("Baro level is now: ");
    Serial.println(curr_baro);
#endif    
    baro_char.writeValue((unsigned long)(curr_baro *1000));
    old_baro = curr_baro;
  }
}
