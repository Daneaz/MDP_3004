#include <PinChangeInt.h>
#include <DualVNH5019MotorShield.h>

DualVNH5019MotorShield md(4, 2, 6, A0, 7, 8, 12, A1);

volatile int mLTicks = 0;
volatile int mRTicks = 0;
volatile int mLRpm = 0;
volatile int mRRpm = 0;

volatile int motorSpd = 400;


void setup() {
  // put your setup code here, to run once:
  pinMode(4, INPUT);  //Interrupt Pin 11
  pinMode(13, INPUT); //Interrupt Pin 13
  
  md.init();
  
  PCintPort::attachInterrupt(11, &compute_mR_ticks, RISING);  //Attached to Pin 3
  PCintPort::attachInterrupt(3, &compute_mL_ticks, RISING); //Attached to Pin 11
  Serial.begin(9600);
  Serial.println("---Calculating RPM--- ");
}


void loop() {
  Serial.print("Setting speed to ");
  Serial.println(motorSpd);
  md.setSpeeds(motorSpd,motorSpd);
  for(int i=0;i<7;i++)
  {
    mLTicks = 0;
    mRTicks = 0;
    delay(1000);
    compute_Ml_Rpm();
    compute_Mr_Rpm();
  }
  motorSpd -= 50;
  if(motorSpd == 0)
  motorSpd = 400;
  delay(3000);
}

void compute_mL_ticks() 
{
  mLTicks++;
}

void compute_mR_ticks() 
{
  mRTicks++;
}

void compute_Mr_Rpm() 
{
  PCintPort::detachInterrupt(11);
  mLRpm = (mRTicks/562.25)*60;
  Serial.print("RPM of mL: ");
  Serial.println(mLRpm); 
  PCintPort::attachInterrupt(11, &compute_mL_ticks, RISING);
}

void compute_Ml_Rpm() 
{
  PCintPort::detachInterrupt(3);
  mRRpm = (mLTicks/562.25)*60;
  Serial.print("RPM of mR: ");
  Serial.println(mRRpm);
  PCintPort::attachInterrupt(3, &compute_mR_ticks, RISING);
}


