#include <PinChangeInt.h>
#include <DualVNH5019MotorShield.h>

DualVNH5019MotorShield md(2, 4, 6, A0, 8, 7, 12, A1);

volatile int mLTicks = 0;
volatile int mRTicks = 0;

unsigned long curTime = 0;
//IRsend irsend;
char inData;

void setup() {
  // put your setup code here, to run once:
  pinMode(4, INPUT);  //Interrupt Pin 11
  pinMode(12, INPUT); //Interrupt Pin 13
  
  md.init();
  
  PCintPort::attachInterrupt(4, &compute_mL_ticks, RISING);  //Attached to Pin 3
  PCintPort::attachInterrupt(12, &compute_mR_ticks, RISING); //Attached to Pin 11
  Serial.begin(9600);
  Serial.println("Waiting for data: ");
}


void compute_mL_ticks() 
{
  mLTicks++;
}

void compute_mR_ticks() 
{
  mRTicks++;
}

void loop() {
  // put your main code here, to run repeatedly:
  while (Serial.available() > 0) {
                   inData += (char)Serial.read();
                   delay(2);
            }
            Serial.println(inData);
            switch(inData)
            {
              case 'F':
                moveForward();
                
                break;
              case 'R':
                turnRight();
                break;
              case 'L':
                turnLeft();
                break;
              default:
                break;        
            }
            inData = NULL;
}

void moveForward(){
  
  double dTotalTicks = 0;
  
  dTotalTicks = 288 / 10.0 * 10;


  while(mLTicks < dTotalTicks)
  {      
    md.setSpeeds(334,400);
  }

  delay(1000);
  forwardBrake();
}

void forwardBrake(){
  
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
  
  dTotalTicks = 288 / 10.0 * 10;


  while(mLTicks < dTotalTicks)
  {      
     md.setSpeeds(350,-400);
  }
 
  delay(440);
  leftBrake();
}

void leftBrake(){
  
  for(int i = 0; i < 100; i++)
  {
    md.setBrakes(325,310);
  }
  
  delay(100);
  
  mLTicks = 0;
  mRTicks = 0;
}



void turnRight(){
  double dTotalTicks = 0;
  
  dTotalTicks = 288 / 10.0 * 10;


  while(mLTicks < dTotalTicks)
  {      
     md.setSpeeds(-350,400);
  }
  
  delay(440);
  rightBrake();
}

void rightBrake(){
  
  for(int i = 0; i < 100; i++)
  {
    md.setBrakes(370,-400);
  }
  
  delay(100);
  
  mLTicks = 0;
  mRTicks = 0;
}




