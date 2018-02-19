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
SharpIR sensorR(GP2Y0A21YK0F, A2);

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
  
  Serial.begin(9600);
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
            //Debug
//            if(flag == false)
//            {
//               flag =true;
//               inData = 'F';
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
    double Kp = 5.5;                  //prefix Kp Ki, Kd
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
  
  double dTotalTicks = 0;
  double output;
  int count =0;
  double avg, total=0;
  int pwm1=400, pwm2=355; 

  
  dTotalTicks = 275 / 10.0 * 10;  // *10 = 10cm

  while(mLTicks < dTotalTicks)
  { 
    if(mLTicks <=100){
           pwm1 = 100;
           pwm2 = 55;
        }
    else if(mLTicks <=300){
           pwm1 = mLTicks;
           pwm2 = mRTicks;
        }
    else {
           pwm1 = 400;
           pwm2 = 355;
        }   
    
    output = pidControlForward(mLTicks,mRTicks);

    md.setSpeeds(pwm1-output, pwm2+output);
        
//    md.setSpeeds(400,365);

    //For Debug
    Serial.print("OutPut:");
    Serial.println(output);
    Serial.println(mLTicks-mRTicks);

    total += abs(mLTicks-mRTicks);
    
    Serial.print("Left ticks:");
    Serial.print(mLTicks);
    Serial.print("/");
    Serial.print(dTotalTicks);

    Serial.print(" Right ticks:");
    Serial.print(mRTicks);
    Serial.print("/");
    Serial.println(dTotalTicks);

    count++;
    
  }
  avg =total/count;
  Serial.print("Avg:");
  Serial.println(avg);
  forwardBrake();
}

void forwardBrake(){
  
  for(int i = 0; i < 100; i++)
  {
    md.setBrakes(350,400);
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
     md.setSpeeds(-400,400);
  }

  rightBrake();
}

void leftBrake(){
  
  for(int i = 0; i < 100; i++)
  {
    md.setBrakes(370,-400);
  }
  
  delay(100);
  
  mLTicks = 0;
  mRTicks = 0;
}

void getSensorsData(){
  int disFL, disFR, disL, disR;
  double avgFL = 0, avgFR = 0, avgL = 0, avgR = 0;
  
  for(int i =0; i<7; i++)
  {
      disFL = sensorFL.getDistance(); //Calculate the distance in centimeters and store the value in a variable
      disFR = sensorFR.getDistance();
      disL = sensorL.getDistance();
      disR = sensorR.getDistance();
      
      avgFL += disFL;
      avgFR += disFR;
      avgL += disL;
      avgR += disR; 
  }
  avgFL = avgFL / 7;
  avgFR = avgFR / 7;
  
  avgL = avgL / 7;
  avgR = avgR / 7;
  
  Serial.print("FL:"); //Print the value to the serial monitor
  Serial.println(avgFL);
  Serial.print("FR:");
  Serial.println( avgFR);
  Serial.print("L:");
  Serial.println(avgL);
  Serial.print("R:");
  Serial.println(avgR);
}

