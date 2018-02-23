#include <PinChangeInt.h>
#include <DualVNH5019MotorShield.h>
#include <SharpIR.h>
#include <PID_v1.h>

#define PIDInputPinM1 A0
#define PIDInputPinM2 A1

DualVNH5019MotorShield md(4, 2, 6, A0, 7, 8, 12, A1);

SharpIR sensorFR(GP2Y0A21YK0F, A0);
SharpIR sensorFL(GP2Y0A21YK0F, A1);
SharpIR sensorL(GP2Y0A21YK0F, A3);
SharpIR sensorRF(GP2Y0A21YK0F, A2);
SharpIR sensorRB(GP2Y0A21YK0F, A4);
volatile int mLTicks = 0;
volatile int mRTicks = 0;

char inData;

void setup() {
  // put your setup code here, to run once:
  pinMode(4, INPUT);  //Interrupt Pin 4
  pinMode(13, INPUT); //Interrupt Pin 13
  
  md.init();
  
  PCintPort::attachInterrupt(11, &compute_mL_ticks, RISING);  //Attached to Pin 11
  PCintPort::attachInterrupt(3, &compute_mR_ticks, RISING); //Attached to Pin 3

  Serial.begin(115200);
  Serial.println("Waiting for data: ");
}

boolean flag =false;

void loop() {
  // put your main code here, to run repeatedly:
  while (Serial.available() > 0) {
                   inData += (char)Serial.read();
                   delay(2);
            }
//            Serial.println(inData);

//            Debug
//            if(flag == false)
//            {
//               flag =true;
//               moveForward();
//                delay(1000);
//            }
            switch(inData)
            {
              

              case 'F':
                moveForward();
                getSensorsData();
                break;
              case 'R':
                turnRight();
                getSensorsData();
                break;
              case 'L':
                turnLeft();
                getSensorsData();
                break;
              case 'S':
                getSensorsData();
                break;
              default:
                break;        

            }
            inData = '\0';
}

void compute_mL_ticks() 
{
  mLTicks++;
}

void compute_mR_ticks() 
{
  mRTicks++;
}



int pidControlForward(int LeftPosition, int RightPosition){

    int error;
    int prev_error;
    double integral,derivative,output;
    double Kp = 1;                  //prefix Kp Ki, Kd dont changed if want to changed pls re declared
    double Kd = 0.4;
    double Ki = 0.1;

    error = LeftPosition - RightPosition;
    integral += error;
    derivative = (error - prev_error);
    output = Kp * error + Ki * integral + Kd * derivative;
    prev_error = error;

    return output;
}

void moveForward(){
  
  float dTotalTicks = 0;
  float output;
  int count =0;
  float avg, total=0;
  int pwm1=245, pwm2=225; 

  
  dTotalTicks = 285 / 10.0 * 10;  // *10 = 10cm
//  dTotalTicks = 265;
  while(mLTicks < dTotalTicks)
  { 
    if(mLTicks <=100)
    {
       pwm1 = 150;
       pwm2 = 115;
    }
    else if(mLTicks <=200)
    {
       pwm1 = 250;
       pwm2 = 225;
    }
    else 
    {
       pwm1 = 245;
       pwm2 = 225;
    }   
    
    output = pidControlForward(mLTicks,mRTicks);

    md.setSpeeds(pwm1-output*3, pwm2+output*3);
        
//    md.setSpeeds(400,338);
//    pwm1 = 400;
//    pwm2 = 338;

    //For Debug
//    Serial.print("OutPut:");
//    Serial.println(output);
//    Serial.println(mLTicks-mRTicks);
//    total += abs(mLTicks-mRTicks);
    
//    Serial.print("Left ticks:");
//    Serial.print(mLTicks);
//    Serial.print("/");
//    Serial.print(dTotalTicks);

//    Serial.print(" Right ticks:");
//    Serial.print(mRTicks);
//    Serial.print("/");
//    Serial.println(dTotalTicks);

//    count++;

//Debug
    Serial.print(mLTicks);
    Serial.print("/");
    Serial.println(mRTicks);

  }
//  avg =total/count;
//  Serial.print("Avg:");
//  Serial.println(avg);

  forwardBrake();
}

void forwardBrake(){
  
  for(int i = 0; i < 100; i++)
  {
    md.setBrakes(400,400);
  }
  
  delay(100);
  
  mLTicks = 0;
  mRTicks = 0;
}

void turnRight(){
  double dTotalTicks = 0;
  
  dTotalTicks = 383/ 10.0 * 10;


  while(mLTicks < dTotalTicks)
  {      
     md.setSpeeds(400,-400);
  }
 
  leftBrake();
}

void rightBrake(){
  
  for(int i = 0; i < 100; i++)
  {
    md.setBrakes(325,310);
  }
  
  delay(100);
  mLTicks = 0;
  mRTicks = 0;
}



void turnLeft(){
  double dTotalTicks = 0;
  
  dTotalTicks = 360 / 10.0 * 10;


  while(mLTicks < dTotalTicks)
  {      
     md.setSpeeds(-400,380);
  }

  rightBrake();
}

void leftBrake(){
  
  for(int i = 0; i < 100; i++)
  {
    md.setBrakes(370,350);
  }
  
  delay(100);
  
  mLTicks = 0;
  mRTicks = 0;
}

float readSensor(SharpIR sensor, float offset)
{
  float distance;
  float avg = 0;
  for(int i =0; i<7; i++)
  {
      distance = sensor.getDistance() - offset;
      avg += distance;
  }
  avg = avg/7;
  return avg;
}


void getSensorsData(){
  float disFL, disFR, disL, disRF, disRB;

  disFL = readSensor(sensorFL,2.0); //Calculate the distance in centimeters and store the value in a variable
  disFR = readSensor(sensorFR,2.6);
  disL = readSensor(sensorL,0);
  disRF = readSensor(sensorRF,0);
  disRB = readSensor(sensorRB,0);
  
  Serial.print("FL:"); //Print the value to the serial monitor
  Serial.println(disFL);
  Serial.print("FR:");
  Serial.println( disFR);
  Serial.print("L:");
  Serial.println(disL);
  Serial.print("RF:");
  Serial.println(disRF);
  Serial.print("RB:");
  Serial.println(disRB);
  Serial.println();
  
}

