#include <PinChangeInt.h>
#include <DualVNH5019MotorShield.h>
#include <SharpIR.h>
#include <PID_v1.h>

#define PIDInputPinM1 A0
#define PIDInputPinM2 A1

DualVNH5019MotorShield md(4, 2, 6, A0, 7, 8, 12, A1);

SharpIR sensorFR(GP2Y0A21YK0F, A0);
SharpIR sensorFL(GP2Y0A21YK0F, A1);
SharpIR sensorFC(GP2Y0A21YK0F, A3);
SharpIR sensorR(GP2Y0A02YK0F, A2);    //Long range
SharpIR sensorLF(GP2Y0A21YK0F, A5);
SharpIR sensorLB(GP2Y0A21YK0F, A4);


volatile int mLTicks = 0;
volatile int mRTicks = 0;



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
char inData;
void loop() {
  // put your main code here, to run repeatedly:

  while (Serial.available() > 0) {
                   inData += (char)Serial.read();
                   delay(2);
            }
//            Serial.println(inData);
//              getSensorsData();
//              delay(500);
//            Debug
//            if(flag == false)
//            {
//              flag =true;
//               moveSpeedup100();
//              turnRight();
//              delay(2000);
//            }
            switch(inData)
            {
              case 'F':
                moveSpeedup();
                getSensorsData();
                break;
              case 'G':
                moveSpeedup100();
                getSensorsData();
                break;
              case 'H':
                moveForward();
                getSensorsData();
                break;
              case 'R':
                turnRight(90);
                getSensorsData();
                break;

              case 'L':
                turnLeft();
                getSensorsData();
                break;
              case 'B':
                turnBack();
                getSensorsData();
                break;
              case 'N':
                turnRight360();
                getSensorsData();
                break;
              case 'S':
                moveBack();
                getSensorsData();
                break;
              case '+':
                moveAdjustF();
                break;
              case '-':
                moveAdjustB();
                break;
              case 'D':
                getSensorsData();
                break;
              case '0':
                realignment();
                break;
              case '[':
                turnLeft45();
                break;
              case ']':
                turnRight45();
                break;
              case 'Z':
                moveDigonal();
                break;
              case 'X':
                realignment();
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

int pidControl(int LeftPosition, int RightPosition){

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

void moveSpeedup100()
{
  float dTotalTicks = 0;
  float output;
  int count =0;
  float avg, total=0;
  int pwm1=355, pwm2=317; 
//  int pwm1=355, pwm2=330; 
  dTotalTicks = 285 / 10.0 * 100;  // *10 = 10cm

  while(mLTicks < dTotalTicks)
  { 

    output = pidControl(mLTicks,mRTicks);
    md.setSpeeds(pwm1-output*15, pwm2+output*15);
    
//    count++;
//    total += abs(mLTicks-mRTicks);
//    Serial.println(mLTicks-mRTicks);
//    Serial.print(mLTicks);
//    Serial.print("/");
//    Serial.println(mRTicks);
  }
//  avg =total/count;
//  Serial.print("Avg:");
//  Serial.println(avg);
//  
  brake();
  
}

void moveSpeedup()
{
  float dTotalTicks = 0;
  float output;
  int count =0;
  float avg, total=0;
  int pwm1=355, pwm2=317; 
//  int pwm1=355, pwm2=330; 
  dTotalTicks = 295 / 10.0 * 10;  // *10 = 10cm

  while(mLTicks < dTotalTicks)
  { 
    
    output = pidControl(mLTicks,mRTicks);
    md.setSpeeds(pwm1-output*15, pwm2+output*15);
    
//    count++;
//    total += abs(mLTicks-mRTicks);
//    Serial.println(mLTicks-mRTicks);
//    Serial.print(mLTicks);
//    Serial.print("/");
//    Serial.println(mRTicks);
  }
//  avg =total/count;
//  Serial.print("Avg:");
//  Serial.println(avg);
  
  brake();
  
}

void moveForward(){
  
  float dTotalTicks = 0;
  float output;
  int count =0;
  float avg, total=0;
  int pwm1=245, pwm2=225; 

  
  dTotalTicks = 285 / 10.0 * 100;  // *10 = 10cm
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
    
    output = pidControl(mLTicks,mRTicks);

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
//    Serial.print(mLTicks);
//    Serial.print("/");
//    Serial.println(mRTicks);

  }
//  avg =total/count;
//  Serial.print("Avg:");
//  Serial.println(avg);

  forwardBrake();
}

void forwardBrake(){
  
  for(int i = 0; i < 3; i++)
  {
    md.setBrakes(370,400);
  }
  
  delay(10);
  
  mLTicks = 0;
  mRTicks = 0;
}


void brake(){
  
  for(int i = 0; i < 3; i++)
  {
    md.setBrakes(400,400);
  }
  
  delay(10);
  
  mLTicks = 0;
  mRTicks = 0;
}

void turnRight(int degree){
  
  double dTotalTicks = 0;
  double output;
  int count =0;
  double avg, total=0;
  int pwm1=345, pwm2=-335; 

  if(degree <= 45)
  {
      dTotalTicks = 4.2 * degree;  // 45 degree
  }
  else if (degree == 90)
  {
      dTotalTicks = 382/ 90.0 * 90;  // 90 degree
  }
  else if (degree == 180)
  {
      dTotalTicks = 800/ 180.0 * 180;  // 180 degree
  }
  md.init();
  while(mLTicks < dTotalTicks)
  {   
        
    output = pidControl(mLTicks,mRTicks);

    md.setSpeeds(pwm1+output*3, pwm2-output*3);
//    count++;
//    total += abs(mLTicks-mRTicks);
//    Serial.println(mLTicks-mRTicks);
//    Serial.print(mLTicks);
//    Serial.print("/");
//    Serial.print(mRTicks);
//    Serial.print("/");
//    Serial.println(dTotalTicks);
  }
//  avg =total/count;
//  Serial.print("Avg:");
//  Serial.println(avg);
 
  brake();
}

void turnRight45(){
  
  float dTotalTicks = 0;
  float output;
  int count =0;
  float avg, total=0;
  int pwm1=355, pwm2=-326; 
  
  dTotalTicks = 191;  // 45 degree

  while(mLTicks < dTotalTicks)
  {   
        
    output = pidControl(mLTicks,mRTicks);

    md.setSpeeds(pwm1+output*3, pwm2-output*3);
//    count++;
//    total += abs(mLTicks-mRTicks);
//    Serial.println(mLTicks-mRTicks);
//    Serial.print(mLTicks);
//    Serial.print("/");
//    Serial.print(mRTicks);
//    Serial.print("/");
//    Serial.println(dTotalTicks);
  }
//  avg =total/count;
//  Serial.print("Avg:");
//  Serial.println(avg);
 
    brake();
}


void turnLeft(){
  float dTotalTicks = 0;
  float output;
  int count =0;
  float avg, total=0;
  int pwm1=-355, pwm2=326; 
  
  dTotalTicks = 370/ 90.0 * 90;  // 90 degree

  while(mLTicks < dTotalTicks)
  {   
    output = pidControl(mLTicks,mRTicks);
    md.setSpeeds(pwm1-output*3, pwm2+output*3);
    
//    count++;
//    total += abs(mLTicks-mRTicks);
//    Serial.println(mLTicks-mRTicks);
//    Serial.print(mLTicks);
//    Serial.print("/");
//    Serial.print(mRTicks);
//    Serial.print("/");
//    Serial.println(dTotalTicks);
  }
//  avg =total/count;
//  Serial.print("Avg:");
//  Serial.println(avg);
 
  brake();
}

void turnLeft45(){
  float dTotalTicks = 0;
  float output;
  int count =0;
  float avg, total=0;
  int pwm1=-355, pwm2=326; 
  
  dTotalTicks = 191;  // 90 degree

  while(mLTicks < dTotalTicks)
  {   
        
    output = pidControl(mLTicks,mRTicks);

    md.setSpeeds(pwm1-output*3, pwm2+output*3);
//    count++;
//    total += abs(mLTicks-mRTicks);
//    Serial.println(mLTicks-mRTicks);
//    Serial.print(mLTicks);
//    Serial.print("/");
//    Serial.print(mRTicks);
//    Serial.print("/");
//    Serial.println(dTotalTicks);
  }
//  avg =total/count;
//  Serial.print("Avg:");
//  Serial.println(avg);
 
  brake();
}

void turnBack(){
  
  float dTotalTicks = 0;
  float output;
  int count =0;
  float avg, total=0;
  int pwm1=355, pwm2=-326; 
  
  dTotalTicks = 800/ 180.0 * 180;  // 90 degree

  while(mLTicks < dTotalTicks)
  {   
        
    output = pidControl(mLTicks,mRTicks);

    md.setSpeeds(pwm1+output*3, pwm2-output*3);
//    count++;
//    total += abs(mLTicks-mRTicks);
//    Serial.println(mLTicks-mRTicks);
//    Serial.print(mLTicks);
//    Serial.print("/");
//    Serial.print(mRTicks);
//    Serial.print("/");
//    Serial.println(dTotalTicks);
  }
//  avg =total/count;
//  Serial.print("Avg:");
//  Serial.println(avg);
 
  brake();
}

void turnRight360(){
  
  float dTotalTicks = 0;
  float output;
  int count =0;
  float avg, total=0;
  int pwm1=355, pwm2=-326; 
  
  dTotalTicks = 1650/ 180.0 * 180;  // 90 degree

  while(mLTicks < dTotalTicks)
  {   
        
    output = pidControl(mLTicks,mRTicks);

    md.setSpeeds(pwm1+output*3, pwm2-output*3);
//    count++;
//    total += abs(mLTicks-mRTicks);
//    Serial.println(mLTicks-mRTicks);
//    Serial.print(mLTicks);
//    Serial.print("/");
//    Serial.print(mRTicks);
//    Serial.print("/");
//    Serial.println(dTotalTicks);
  }
//  avg =total/count;
//  Serial.print("Avg:");
//  Serial.println(avg);
 
  brake();
}

void moveBack()
{
  float dTotalTicks = 0;
  float output;
  int count =0;
  float avg, total=0;
  int pwm1=-355, pwm2=-315; 

  dTotalTicks = 285 / 10.0 * 10;  // *10 = 10cm
  
  while(mLTicks < dTotalTicks)
  { 
    
    output = pidControl(mLTicks,mRTicks);
    md.setSpeeds(pwm1-output*5, pwm2-output*5);
    
//    count++;
//    total += abs(mLTicks-mRTicks);
//    Serial.println(mLTicks-mRTicks);
//    Serial.print(mLTicks);
//    Serial.print("/");
//    Serial.println(mRTicks);
  }
//  avg =total/count;
//  Serial.print("Avg:");
//  Serial.println(avg);
  
  brake();
  
}

void moveAdjustF()
{
  float dTotalTicks = 0;
  float output;
  int count =0;
  float avg, total=0;
  int pwm1=355, pwm2=315; 

  dTotalTicks = 28;  // *10 = 10cm
  
  while(mLTicks < dTotalTicks)
  { 
    
    output = pidControl(mLTicks,mRTicks);
    md.setSpeeds(pwm1-output*5, pwm2+output*5);
    
//    count++;
//    total += abs(mLTicks-mRTicks);
//    Serial.println(mLTicks-mRTicks);
//    Serial.print(mLTicks);
//    Serial.print("/");
//    Serial.println(mRTicks);
  }
//  avg =total/count;
//  Serial.print("Avg:");
//  Serial.println(avg);
  
  brake();
  
}

void moveAdjustB()
{
  float dTotalTicks = 0;
  float output;
  int count =0;
  float avg, total=0;
  int pwm1=-355, pwm2=-315; 
//    int pwm1 = -150, pwm2 = -150;

  dTotalTicks = 28;  
  
  while(mLTicks < dTotalTicks)
  { 
    
    output = pidControl(mLTicks,mRTicks);
    md.setSpeeds(pwm1-output*5, pwm2+output*5);
    
//    count++;
//    total += abs(mLTicks-mRTicks);
//    Serial.println(mLTicks-mRTicks);
//    Serial.print(mLTicks);
//    Serial.print("/");
//    Serial.println(mRTicks);
  }
//  avg =total/count;
//  Serial.print("Avg:");
//  Serial.println(avg);
  
  brake();
  
}


float readSensor(SharpIR sensor, float offset)
{
  float distance;
  float avg = 0;
  for(int i =0; i<7; i++)
  {
      distance = sensor.getDistance() + offset;
      avg += distance;
  }
  avg = avg/7;
  return avg;
}



void getSensorsData(){
  float disFL,disFC,disFR, disLF,disLB, disR;

  disFL = readSensor(sensorFL,0); //Calculate the distance in centimeters and store the value in a variable
  disFR = readSensor(sensorFR,0);
  disFC = readSensor(sensorFC,0);
  disR = readSensor(sensorR,0);
  disLF = readSensor(sensorLF,0);
  disLB = readSensor(sensorLB,0);
  
  Serial.print("FL:"); //Print the value to the serial monitor
  Serial.println(disFL);
  Serial.print("FC:");
  Serial.println( disFC);
  Serial.print("FR:");
  Serial.println( disFR);
  Serial.print("R:");
  Serial.println(disR);
  Serial.print("LF:");
  Serial.println(disLF);
  Serial.print("LB:");
  Serial.println(disLB);
  Serial.println();
//
//  if(disFC <15 || disFL<15 || disFR <15)
//  {
//    
//    if(disLF <15 || disLB <15)
//    {
//      //call realliment here
//      realignment();
//      digonalRight();
//    }
//    else 
//    {
//      realignment();
//      digonalLeft();
//    }
//  }
  
}

void realignment()
{
  float disFL,disFC,disFR, disLF,disLB, disR;
  
  turnLeft();
  delay(200);
  disFL = readSensor(sensorFL,0); //Calculate the distance in centimeters and store the value in a variable
  disFR = readSensor(sensorFR,0);
  disFC = readSensor(sensorFC,0);
  disR = readSensor(sensorR,0);
  disLF = readSensor(sensorLF,0);
  disLB = readSensor(sensorLB,0);
    
  if(disFL <10 || disFC <10 || disFR <10)
  {
    while(1)
    {
      disFL = readSensor(sensorFL,0); //Calculate the distance in centimeters and store the value in a variable
      disFR = readSensor(sensorFR,0);
      disFC = readSensor(sensorFC,0);
      disR = readSensor(sensorR,0);
      disLF = readSensor(sensorLF,0);
      disLB = readSensor(sensorLB,0);
      
      if(disFL <10 || disFC <10 || disFR <10)
      {
        moveAdjustB();
        delay(10);
      }
      else
      {
        brake();
        break;
      }
      
    }
  }
  delay(200);
  turnRight(90);
}

void moveDigonal()
{
  double dTotalTicks = 0;
  double output;
  float disFL,disFC,disFR, disLF,disLB, disR;

  int pwm1=355, pwm2=317; 

  dTotalTicks = 285 / 10.0 * 100;  // *10 = 10cm

  while(mLTicks < dTotalTicks)
  { 
    disFL = readSensor(sensorFL,0); //Calculate the distance in centimeters and store the value in a variable
    disFR = readSensor(sensorFR,0);
    disFC = readSensor(sensorFC,0);
    disR = readSensor(sensorR,0);
    disLF = readSensor(sensorLF,0);
    disLB = readSensor(sensorLB,0);

    
    if(disFC <15 || disFL<15 || disFR <15)
    {
      break;
    }
    
    output = pidControl(mLTicks,mRTicks);
    md.setSpeeds(pwm1-output*15, pwm2+output*15);
  }
  
  brake();
  delay(200);
  while(1)
  {
    disFL = readSensor(sensorFL,0); //Calculate the distance in centimeters and store the value in a variable
    disFR = readSensor(sensorFR,0);
    disFC = readSensor(sensorFC,0);
    disR = readSensor(sensorR,0);
    disLF = readSensor(sensorLF,0);
    disLB = readSensor(sensorLB,0);
  
    if(disFL <13 || disFC <13 || disFR <13)
    {
      moveAdjustB();
      delay(10);
    }
    else
    {
      brake();
      break;
    }
    
  }
  delay(200);
  if(disLF <15 || disLB <15)
  {
      
    digonalRight();
  }
  else 
  {
    digonalLeft();
  }  

}
  


void digonalRight()
{
    turnRight45();
    delay(250);
    moveSpeedup();
    delay(250);
    moveSpeedup();
    delay(250);
    moveSpeedup();
    delay(250);
    moveSpeedup();
    delay(250);
    turnLeft45();
    delay(250);
    moveSpeedup();
    delay(250);
    moveSpeedup();
}

void digonalLeft()
{
    turnLeft45();
    delay(250);
    moveSpeedup();
    delay(250);
    moveSpeedup();
    delay(250);
    moveSpeedup();
    delay(250);
    moveSpeedup();
    delay(250);
    turnRight45();
    delay(250);
    moveSpeedup();
    delay(250);
    moveSpeedup();
}

