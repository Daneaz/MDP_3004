#include <PinChangeInt.h>
#include <DualVNH5019MotorShield.h>
#include <RunningMedian.h>
#include <SharpIR.h>

//Sharp IR sensor define
SharpIR sensorFR(GP2Y0A21YK0F, A0);
SharpIR sensorFL(GP2Y0A21YK0F, A1);
SharpIR sensorFC(GP2Y0A21YK0F, A3);
//SharpIR sensorR(GP2Y0A02YK0F, A2);
#define sensorR A2
SharpIR sensorLF(GP2Y0A21YK0F, A5);
SharpIR sensorLB(GP2Y0A21YK0F, A4);

//motor pins
DualVNH5019MotorShield md(4, 2, 6, A0, 7, 8, 12, A1);

//Median variables
static RunningMedian FrontR = RunningMedian(50);
static RunningMedian FrontL = RunningMedian(50);
static RunningMedian FrontC = RunningMedian(50);
static RunningMedian Right = RunningMedian(50);
static RunningMedian LeftF = RunningMedian(50);
static RunningMedian LeftB = RunningMedian(50);

volatile int mLTicks = 0;
volatile int mRTicks = 0;

//Global var declaration
boolean flag = false;         //for starting alligment
String inString = "";         //for cmd
int previousLF;               //to record the previous value of Left sensor for calibration
int previousR;                //to record the previous value of Right sensor for calibration
int movementCount = 0;        //for forward adjustment
int adjustFrontFailCount = 0; //for check fail to adjust front
int adjustFailCount = 0;      //for check fail to adjust
double disFL, disFC, disFR, disLF, disLB, disR;   //for sensor calibration
int FL, FC, FR, LF, LB, R;    //for sensor feedback to pc

void setup() {
  // put your setup code here, to run once:
  pinMode(4, INPUT);  //Interrupt Pin 4
  pinMode(13, INPUT); //Interrupt Pin 13

  md.init();    // init robot

  PCintPort::attachInterrupt(11, &compute_mL_ticks, RISING);  //Attached to Pin 11
  PCintPort::attachInterrupt(3, &compute_mR_ticks, RISING); //Attached to Pin 3

  Serial.begin(115200);
  Serial.setTimeout(10);        // for reading string time out intervel
  //  Serial.println("Waiting for data: ");
}

int moveCount = 0;    //counter

void loop() {
  // put your main code here, to run repeatedly:

  //    getSensorsData();

  // Read string from serial
  while (Serial.available() > 0)
  {
    inString = Serial.readString();
  }

  // Only print when it is not empty string
  if (inString != "")
  {
    String decodeString;
    //    Serial.println(inString);
    char cmd = inString.charAt(0);
    //    Serial.println(cmd);
    for (int i = 1; i < inString.length(); i++)
    {
      decodeString += inString.charAt(i);
    }
    int target = decodeString.toInt();
    //    Serial.println(target);

    runCMD(cmd, target);

  }

  // Clear the string
  inString = "";
}

void fastPath(String str)
{
  char cmd;
  String cdis;
  int dis = 0;

  for (int i = 1; i < str.length(); i++)
  {
    if (str[i] == 'M')
    {
      cmd = 'M';
      i++;
      cdis = str[i];
      i++;
      cdis += str[i];
    }
    else
    {
      cmd = str[i];
    }
    dis = cdis.toInt();
    Serial.println(cmd);
    Serial.println(dis);

    switch (cmd)
    {
      case 'M':
        fastForward(dis);
        //        Serial.println(dis);
        break;
      case 'R':
        turnRight(90);
        //        Serial.println("Fast R");
        break;
      case 'L':
        turnLeft(90);
        //        Serial.println("Fast L");
        break;
      case 'U':
        turnLeft(90);
        turnLeft(90);
        //        Serial.println("Fast U");
        break;
    }
    cdis = "";
    dis = 0;
  }
}

//*cmd
void runCMD(char cmd, int target)
{
  switch (cmd)
  {
    case 'M':
      moveSpeedup(target);
      getSensorsData();
      break;
    case 'R':
      turnRight(target);
      getSensorsData();
      break;
    case 'L':
      turnLeft(target);
      getSensorsData();
      break;
    case 'U':
      turnLeft(90);
      turnLeft(90);
      getSensorsData();
      break;
    case 'S':
      moveBack(target);
      getSensorsData();
      break;
    case 'A':
      turnLeft(90);
      autoCalibrate();
      delay(20);
      turnRight(90);
      autoCalibrate();
      delay(20);
      //      getSensorsData();
      Serial.println("POK");
      break;
    case 'Q':
      turnLeft(90);
      autoCalibrate();
      delay(20);
      turnRight(90);
      //      getSensorsData();
      Serial.println("POK");
      break;
    case 'E':
      turnRight(90);
      autoCalibrate();
      delay(20);
      turnLeft(90);
      //      getSensorsData();
      Serial.println("POK");
      break;
    case 'C':
      autoCalibrate();
      //      getSensorsData();
      Serial.println("POK");
      break;
    case 'X':
      fastPath(inString);
      break;
    case 'D':
      if (flag == false)
      {
        turnLeft(90);
        turnLeft(90);
        autoCalibrate();
        delay(20);
        turnRight(90);
        autoCalibrate();
        delay(20);
        turnRight(90);
        flag = true;
      }
      getSensorsData();
      break;
    //debug function
    case 'F':
      getSensorsDataFront();
      break;
    case 'G':
      getSensorsDataLeft();
      break;
    case 'H':
      getSensorsDataDistanceAdjust();
      break;
    case '+':
      moveAdjustF();
      break;
    case '-':
      moveAdjustB();
      break;
    //checklist function
    //    case 'Z':
    //      moveDigonal();
    //      break;
    default:
      break;
  }

}



//*pid
int pidControl(int LeftPosition, int RightPosition) {

  int error;
  int prev_error;
  double integral, derivative, output;
  double Kp = 3;                  //prefix Kp Ki, Kd dont changed if want to changed pls re declared
  double Kd = 0;
  double Ki = 1;

  error = LeftPosition - RightPosition;
  integral += error;
  derivative = (error - prev_error);

  output = Kp * error + Ki * integral + Kd * derivative;
  prev_error = error;
  return output;
}

//*msu

void moveSpeedup(int dis)
{

  double dTotalTicks = 0;
  double output;
  //  int count = 0;
  //  double avg, total = 0;

  //  int pwm1 = 282, pwm2 = 330;
  //    int pwm1 = 360, pwm2 = 370;
  //  int pwm1 = 374, pwm2 = 375;
  int pwm1 = 374, pwm2 = 368;

  //  dTotalTicks = 295 / 10.0 * 10;  // *10 = 10cm
  if (dis <= 1)
  {
    //    dTotalTicks = 310;  // 1 box
    dTotalTicks = 265;   //B2
  }
  else if (dis > 1 && dis <= 3 )
  {
    dTotalTicks = 281 * dis;  // 1 to 3 box
  }
  else if (dis > 3 && dis <= 5)
  {
    dTotalTicks = 289 * dis;  // 3 to 5 box
  }
  else if (dis > 5 && dis <= 7)
  {
    dTotalTicks = 291 * dis;  //5 to 7 box
  }
  else if (dis > 7 && dis <= 10)
  {
    dTotalTicks = 293 * dis;  //7 to 10 box
  }
  else if (dis > 10 && dis <= 13)
  {
    dTotalTicks = 296 * dis;  //7 to 10 box
  }


  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 5, pwm2 + output * 3);    //check coiffient for debug

    //    count++;
    //    total += abs(mLTicks - mRTicks);
    //    Serial.println(mLTicks - mRTicks);
    //    Serial.print(mLTicks);
    //    Serial.print("/");
    //    Serial.println(mRTicks);
  }
  //  avg = total / count;
  //  Serial.print("Avg:");
  //  Serial.println(avg);

  fbrake();

  movementCount++;
  if ( movementCount >= 4)
  {
    while (mLTicks < 6)
      md.setSpeeds(200, 0);
    movementCount = 0;
  }
  fbrake();
}

void fastForward(int dis)
{

  double dTotalTicks = 0;
  double output;

  //  int pwm1 = 282, pwm2 = 330;
  int pwm1 = 375, pwm2 = 374;

  if (dis <= 1)
  {
    //    dTotalTicks = 310;  // 1 box
    dTotalTicks = 265;   //B2
  }
  else if (dis > 1 && dis <= 3 )
  {
    dTotalTicks = 276 * dis;  // 1 to 3 box
  }
  else if (dis > 3 && dis <= 5)
  {
    dTotalTicks = 286 * dis;  // 3 to 5 box
  }
  else if (dis > 5 && dis <= 7)
  {
    dTotalTicks = 286 * dis;  //5 to 7 box
  }
  else if (dis > 7 && dis <= 10)
  {
    dTotalTicks = 290 * dis;  //7 to 10 box
  }
  else if (dis > 10 && dis <= 13)
  {
    dTotalTicks = 292 * dis;  //7 to 10 box
  }
  else if (dis > 13 && dis <= 17)
  {
    dTotalTicks = 293 * dis;  //7 to 10 box
  }

  while (mLTicks < dTotalTicks)
  {
    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 5, pwm2 + output * 3);    //check coiffient for debug
  }
  fbrake();
}

void fbrake() {

  for (int i = 0; i < 10; i++)
  {
    md.setBrakes(368, 400);
    //    md.setBrakes(370, 400);
  }

  delay(100);

  mLTicks = 0;
  mRTicks = 0;
}

//*b
void brake() {

  for (int i = 0; i < 3; i++)
  {
    md.setBrakes(400, 400);
  }

  delay(100);

  mLTicks = 0;
  mRTicks = 0;
}

void brakeabit() {
  for (int i = 0; i < 3; i++)
  {
    md.setBrakes(400, 400);
  }

  delay(10);
  mLTicks = 0;
  mRTicks = 0;
}

//*tr
void turnRight(int degree) {

  double dTotalTicks = 0;
  double output;
  //  int count = 0;
  //  double avg, total = 0;
  //  int pwm1 = 355, pwm2 = -330;        //Battery 1
  int pwm1 = 344, pwm2 = -316;

  if (degree == 0)
  {
    //    dTotalTicks = 391;
    //    dTotalTicks = 379 / 90 * 90;     //Battery 1
    dTotalTicks = 367;        //Battery 2
  }
  else if (degree <= 45)
  {
    dTotalTicks = 189 / 45 * degree; // 45 degree
    //    dTotalTicks = 204 / 45 * degree; // 45 degree
  }
  else if (degree > 45 && degree <= 90 )
  {
    dTotalTicks = 379 / 90 * degree; // 90 degree
    //    dTotalTicks = 381 / 90 * degree; // 90 degree
  }
  else if (degree > 90)
  {
    dTotalTicks = 1657 / 360 * degree; // 360 degree  // For checklist
  }

  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);

    md.setSpeeds(pwm1 + output * 3, pwm2 - output * 3);
    //    count++;
    //    total += abs(mLTicks - mRTicks);
    //    Serial.println(mLTicks - mRTicks);
    //    Serial.print(mLTicks);
    //    Serial.print("/");
    //    Serial.print(mRTicks);
    //    Serial.print("/");
    //    Serial.println(dTotalTicks);
  }
  //  avg = total / count;
  //  Serial.print("Avg:");
  //  Serial.println(avg);

  brake();
}


void turnLeft(int degree) {
  double dTotalTicks = 0;
  double output;
  //  int count = 0;
  //  double avg, total = 0;
  //  int pwm1 = -355, pwm2 = 316;    //Battery 1
  int pwm1 = -344, pwm2 = 316;
  //    int pwm1 = -355, pwm2 = 326;        //Battery 2
  if (degree == 0)
  {
    //    dTotalTicks = 405 / 90 * 90;    //Battery 1
    dTotalTicks = 362;      //Battery 2
  }
  else if (degree <= 45)
  {
    dTotalTicks = 198 / 45 * degree; // 45 degree
    //    dTotalTicks = 206 / 45 * degree; // 45 degree
  }
  else if (degree > 45 && degree <= 90 )
  {
    dTotalTicks = 405 / 90 * degree; // 90 degree
    //    dTotalTicks = 410 / 90 * degree; // 90 degree
  }
  else if (degree > 90 && degree <= 180)
  {
    dTotalTicks = 808 / 180 * degree; // 180 degree
    //    dTotalTicks = 818 / 180 * degree; // 180 degree
  }
  else if (degree > 180 && degree <= 360)
  {
    dTotalTicks = 1656; // 360 degree
    //    dTotalTicks = 1632 / 360 * degree; // 360 degree


  }

  while (mLTicks < dTotalTicks)
  {
    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 3, pwm2 + output * 3);

    //    count++;
    //    total += abs(mLTicks - mRTicks);
    //    Serial.println(mLTicks - mRTicks);
    //    Serial.print(mLTicks);
    //    Serial.print("/");
    //    Serial.print(mRTicks);
    //    Serial.print("/");
    //    Serial.println(dTotalTicks);
  }
  //  avg = total / count;
  //  Serial.print("Avg:");
  //  Serial.println(avg);

  brake();
}

void turnRightabit(int degree) {

  double dTotalTicks = 0;
  double output;

  int pwm1 = 355, pwm2 = -330;        //Battery 1

  dTotalTicks = 189 / 45 * degree; // 45 degree
  while (mLTicks < dTotalTicks)
  {
    output = pidControl(mLTicks, mRTicks);

    md.setSpeeds(pwm1 + output * 3, pwm2 - output * 3);
  }

  brakeabit();
}


void turnLeftabit(int degree) {
  double dTotalTicks = 0;
  double output;

  int pwm1 = -355, pwm2 = 316;    //Battery 1

  dTotalTicks = 198 / 45 * degree; // 45 degree

  while (mLTicks < dTotalTicks)
  {
    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 3, pwm2 + output * 3);

  }

  brakeabit();
}


void moveBack(int dis)
{
  double dTotalTicks = 0;
  double output;
  //  int count = 0;
  //  double avg, total = 0;
  //  int pwm1 = -355, pwm2 = -315;
  int pwm1 = -344, pwm2 = -316;
  if (dis <= 1)
  {
    dTotalTicks = 285;  // 1 box
  }
  else if (dis > 1 && dis <= 3 )
  {
    dTotalTicks = 285 * dis;  // 1 to 3 box
  }
  else if (dis > 3 && dis <= 5)
  {
    dTotalTicks = 285 * dis;  // 3 to 5 box
  }
  else if (dis > 5 && dis <= 7)
  {
    dTotalTicks = 285 * dis;  //5 to 7 box
  }
  else if (dis > 7 && dis <= 10)
  {
    dTotalTicks = 285 * dis;  //7 to 10 box
  }
  else if (dis > 10 && dis <= 13)
  {
    dTotalTicks = 285 * dis;  //10 to 13 box
  }

  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 5, pwm2 - output * 5);

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
  double dTotalTicks = 0;
  double output;
  //  int pwm1 = 333, pwm2 = 353;
  int pwm1 = 344, pwm2 = 316;
  dTotalTicks = 5;

  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 5, pwm2 + output * 5);
  }
  brakeabit();
}

void moveAdjustB()
{
  double dTotalTicks = 0;
  double output;
  int pwm1 = -355, pwm2 = -315;
  //  int pwm1 = -100, pwm2 = -80;

  dTotalTicks = 4;

  while (mLTicks < dTotalTicks)
  {
    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 , pwm2 );

  }
  brakeabit();
}


double readLongRange(uint8_t sensor, double offset)
{
  double data;
  int distance;
  //Reading analog voltage of sensor
  data = analogRead(sensor);

  distance = (-0.000000478715139 * pow(data, 3) + 0.000697599228477 * pow(data, 2) - 0.423545499540302 * data + 115.325540693115000) - offset;

  if (distance <= 19)
    distance = 10;
  else if (distance <= 25)
    distance = 20;
  else if (distance > 25 &&  distance <= 35)
    distance = 30;
  else if (distance > 35 && distance <= 45)
    distance = 40;
  else if (distance > 45 && distance <= 55)
    distance = 50;
  else if (distance > 55 && distance <= 65)
    distance = 60;
  return distance;
}

double readSensor(SharpIR sensor, double offset)
{
  double dis;

  dis = sensor.getDistance() + offset;

  return dis;
}

//*grm
//get all sensor data
void getRMedian()
{
  for (int sCount = 0; sCount < 50 ; sCount++)
  {
    //Calculate the distance in centimeters and store the value in a variable
    disFL = readSensor(sensorFL, -4);
    disFC = readSensor(sensorFC, -3);
    disFR = readSensor(sensorFR, -4);
    disLF = readSensor(sensorLF, -5);
    disLB = readSensor(sensorLB, -5);
    disR = readLongRange(sensorR, 0);

    //add the variables into arrays as samples
    FrontR.add(disFR);
    FrontL.add(disFL);
    FrontC.add(disFC);
    Right.add(disR);
    LeftF.add(disLF);
    LeftB.add(disLB);
  }
}

//seperate from the main function to save time
//get front sensor data only
//offset to increase the acurracy for short distance
void getRMedianFront()
{
  for (int sCount = 0; sCount < 20 ; sCount++)
  {
    disFL = readSensor(sensorFL, -5.8);

    disFC = readSensor(sensorFC, -4.8);
    disFR = readSensor(sensorFR, -5.8);

    //add the variables into arrays as samples
    FrontR.add(disFR);
    FrontL.add(disFL);
    FrontC.add(disFC);
  }
}

//seperate from the main function to save time
//get left sensor data only
//offset to increase the acurracy for short distance
void getRMedianLeft()
{
  for (int sCount = 0; sCount < 20 ; sCount++)
  {

    disLF = readSensor(sensorLF, -10);
    disLB = readSensor(sensorLB, -9.9);

    //add the variables into arrays as samples
    LeftF.add(disLF);
    LeftB.add(disLB);
  }
}

//For distance adjustment no offset
void getRMedianDistanceAdjust()
{
  for (int sCount = 0; sCount < 20 ; sCount++)
  {
    disFL = readSensor(sensorFL, 0);
    disFC = readSensor(sensorFC, 0);
    disFR = readSensor(sensorFR, 0);

    //add the variables into arrays as samples
    FrontL.add(disFL);
    FrontC.add(disFC);
    FrontR.add(disFR);

  }
}

//*crm
void clearRMedian() {
  FrontR.clear();
  FrontL.clear();
  FrontC.clear();
  Right.clear();
  LeftF.clear();
  LeftB.clear();
}

//*gsd
void getSensorsData() {

  getRMedian();
  FL = FrontL.getMedian() / 10 + 1;
  FC = FrontC.getMedian() / 10 + 1;
  FR = FrontR.getMedian() / 10 + 1;
  LF = LeftF.getMedian() / 10 + 1;
  LB = LeftB.getMedian() / 10 + 1;
  R = Right.getMedian() / 10 + 1;
  clearRMedian();


  checkForCalibration();

  // Message to PC
  Serial.print('P');
  Serial.print(disFL);
  Serial.print(",");
  Serial.print(disFC);
  Serial.print(",");
  Serial.print(disFR);
  Serial.print(",");
  Serial.print(disLF);
  Serial.print(",");
  Serial.print(disLB);
  Serial.print(",");
  Serial.println(disR);
}

//Debug function for calibration
void getSensorsDataFront() {

  getRMedianFront();
  Serial.print('P');
  Serial.print(FrontL.getAverage());
  Serial.print(",");
  Serial.print(FrontC.getAverage());
  Serial.print(",");
  Serial.println(FrontR.getAverage());
  clearRMedian();
}

//Debug function for calibration
void getSensorsDataLeft() {

  getRMedianLeft();
  Serial.print('P');
  Serial.print(LeftF.getAverage());
  Serial.print(",");
  Serial.println(LeftB.getAverage());
  clearRMedian();
}

//Debug function for calibration
void getSensorsDataDistanceAdjust() {

  getRMedianDistanceAdjust();
  Serial.print('P');
  Serial.print(LeftF.getAverage());
  Serial.print(",");
  Serial.println(LeftB.getAverage());
  clearRMedian();
}

void checkForCalibration()
{
  if ((FL == 1 && FC == 1) || (FL == 1 && FR == 1) || (FC == 1 && FR == 1))
  {
    if (LF == 1 && LB == 1)
    {
      FrontAndLeftWall();
    }
    else
    {
      FrontWall();
    }
  }
  else if (LF == 1 && LB == 1)
  {
    LeftWall();
    adjustFrontFailCount++;
  }
  else
  {
    adjustFailCount++;
  }

  if (adjustFrontFailCount >= 6 && (previousLF == 1 && (LF == 1 || LB == 1)))
  {
    turnLeft(90);
    FrontWall();
    turnRight(90);
    adjustFrontFailCount = 0;
  }
  else if (adjustFailCount >= 5 )
  {
    if (previousR == 1 && R == 1)
    {
      turnRight(90);
      FrontWall();
      turnLeft(90);
      adjustFailCount = 0;
      adjustFrontFailCount = 0;
    }
    //staircase angle calibration
    else if (FL == 1 && FC == 2)
    {
      adjustAngleStaircase("FL1", "FC2");
    }
    else if (FL == 2 && FC == 1)
    {
      adjustAngleStaircase("FL2", "FC1");
    }
    else if (FC == 1 && FR == 2)
    {
      adjustAngleStaircase("FC1", "FR2");
    }
    else if (FC == 2 && FR == 1)
    {
      adjustAngleStaircase("FC2", "FR1");
    }
  }

  previousR = R;
  previousLF = LF;

}


void FrontWall()
{
  adjustDistance();
  adjustAngleFront();
}

void LeftWall()
{
  adjustAngleLeft();
}

void FrontAndLeftWall()
{
  turnLeft(90);
  autoCalibrate();
  delay(20);
  turnRight(90);
  autoCalibrate();
  delay(20);
}

//auto calibration
void autoCalibrate()
{
  adjustDistance();
  adjustAngleFront();
  adjustAngleLeft();
}

//*aa
// Front One Grid Angle Alignment
void adjustAngleFront()
{
  //error variables
  double calibrateFront = 9;
  double calibrateFrontLeft = 9;
  double calibrateFrontRight = 9;

  double dErrorFront = 0;
  double dErrorFrontLeft = 0;
  double dErrorFrontRight = 0;

  double dErrorDiff_1 = 0, dErrorDiff_2 = 0, dErrorDiff_3 = 0;

  //timer for break the loop in case stuck in the loop
  unsigned long preTime = millis();
  unsigned long curTime = 0;

  while (1)
  {
    curTime = millis();

    if (curTime - preTime > 5000)
    {
      break;
    }

    getRMedianFront();
    disFL = FrontL.getAverage();
    disFC = FrontC.getAverage();
    disFR = FrontR.getAverage();
    clearRMedian();

    dErrorFront =  disFC - calibrateFront;
    dErrorFrontLeft =  disFL - calibrateFrontLeft;
    dErrorFrontRight =  disFR - calibrateFrontRight;

    dErrorDiff_1 = dErrorFrontLeft - dErrorFrontRight;
    dErrorDiff_2 = dErrorFront - dErrorFrontLeft;
    dErrorDiff_3 = dErrorFront - dErrorFrontRight;

    if ((disFC > 3 && disFC < 11) || (disFL > 3 && disFL < 11) || (disFR > 3 && disFR < 11))
    {
      if ((disFL > 3 && disFL < 11) && (disFR > 3 && disFR < 11))
      {
        if (abs(dErrorDiff_1) < 0.3)
        {
          break;
        }
        if (dErrorFrontLeft < dErrorFrontRight)
        {
          turnLeftabit(1);
        }
        else
        {
          turnRightabit(1);
        }
      }
      else if ((disFC > 3 && disFC < 11) && (disFL > 3 && disFL < 11))
      {
        if (abs(dErrorDiff_2) < 0.3)
        {
          break;
        }
        if (dErrorFrontLeft > dErrorFront)
        {
          turnRightabit(1);
        }
        else
        {
          turnLeftabit(1);
        }
      }
      else if ((disFC > 3 && disFC < 11) && (disFR > 3 && disFR < 11))
      {
        if (abs(dErrorDiff_3) < 0.3)
        {
          break;
        }
        if (dErrorFrontRight > dErrorFront)
        {
          turnLeftabit(1);
        }
        else
        {
          turnRightabit(1);
        }
      }
    }
    //break the while loop instead of wait for 5s
    else
    {
      break;
    }
  }
  //May need a delay here

}

void adjustAngleLeft()
{
  //error variables
  double calibrateLeftFront = 9;
  double calibrateLeftBack = 9;

  double dErrorLeftFront = 0;
  double dErrorLeftBack = 0;

  double dErrorDiff = 0;

  //timer for break the loop in case stuck in the loop
  unsigned long preTime = millis();
  unsigned long curTime = 0;

  while (1)
  {
    curTime = millis();

    if (curTime - preTime > 5000)
    {
      break;
    }
    getRMedianLeft();
    disLF = LeftF.getAverage();
    disLB = LeftB.getAverage();
    clearRMedian();

    dErrorLeftFront =  disLF - calibrateLeftFront;
    dErrorLeftBack =  disLB - calibrateLeftBack;
    dErrorDiff = dErrorLeftFront - dErrorLeftBack;

    if ((disLF > 3 && disLF < 18) && (disLB > 3 && disLB < 18))
    {

      if (abs(dErrorDiff) < 0.3)
      {
        break;
      }
      if (dErrorLeftFront < dErrorLeftBack)
      {
        turnRightabit(1);

      }
      else if (dErrorLeftFront > dErrorLeftBack)
      {
        turnLeftabit(1);
      }
    }
    //break the while loop instead of wait for 5s
    else
    {
      break;
    }
  }
  //May need a delay here

}

void adjustAngleStaircase(String sensor1, String sensor2)
{
  //error variables
  double calibrateFL = 0;
  double calibrateFC = 0;
  double calibrateFR = 0;

  double dErrorFC = 0;
  double dErrorFL = 0;
  double dErrorFR = 0;

  double dErrorDiffLC = 0, dErrorDiffCR = 0;

  //timer for break the loop in case stuck in the loop
  unsigned long preTime = millis();
  unsigned long curTime = 0;

  if (sensor1 == "FL1" && sensor2 == "FC2")
  {
    calibrateFL = 9;
    calibrateFC = 19;
  }
  else if (sensor1 == "FL2" && sensor2 == "FC1")
  {
    calibrateFL = 19;
    calibrateFC = 9;
  }
  else if (sensor1 == "FC1" && sensor2 == "FR2")
  {
    calibrateFC = 9;
    calibrateFR = 19;
  }
  else if (sensor1 == "FC2" && sensor2 == "FR1")
  {
    calibrateFC = 19;
    calibrateFR = 9;
  }

  while (1)
  {
    curTime = millis();

    if (curTime - preTime > 5000)
    {
      break;
    }

    getRMedianFront();
    disFL = FrontL.getAverage();
    disFC = FrontC.getAverage();
    disFR = FrontR.getAverage();
    clearRMedian();

    dErrorFC =  disFC - calibrateFC;
    dErrorFL =  disFL - calibrateFL;
    dErrorFR =  disFR - calibrateFR;

    dErrorDiffLC = dErrorFL - dErrorFC;
    dErrorDiffCR = dErrorFC - dErrorFR;

    if ((disFL > 3 && disFL < 11) && (disFC > 3 && disFC < 21))
    {
      if (abs(dErrorDiffLC) < 0.3)
      {
        break;
      }
      if (dErrorFL < dErrorFC)
      {
        turnLeftabit(1);
      }
      else
      {
        turnRightabit(1);
      }
    }
    else if ((disFC > 3 && disFC < 21) && (disFL > 3 && disFL < 11))
    {
      if (abs(dErrorDiffLC) < 0.3)
      {
        break;
      }
      if (dErrorFL > dErrorFC)
      {
        turnRightabit(1);
      }
      else
      {
        turnLeftabit(1);
      }
    }
    else if ((disFC > 3 && disFC < 11) && (disFR > 3 && disFR < 21))
    {
      if (abs(dErrorDiffCR) < 0.3)
      {
        break;
      }
      if (dErrorFR > dErrorFC)
      {
        turnLeftabit(1);
      }
      else
      {
        turnRightabit(1);
      }
    }
    else if ((disFC > 3 && disFC < 21) && (disFR > 3 && disFR < 11))
    {
      if (abs(dErrorDiffCR) < 0.3)
      {
        break;
      }
      if (dErrorFR > dErrorFC)
      {
        turnLeftabit(1);
      }
      else
      {
        turnRightabit(1);
      }
    }
    //break the while loop instead of wait for 5s
    else
    {
      break;
    }
  }
  //May need a delay here

}

//*ad
void adjustDistance()
{
  unsigned long preTime = millis();
  unsigned long curTime = 0;

  while (1)
  {
    curTime = millis();
    if (curTime - preTime > 5000)
    {
      break;
    }
    getRMedianDistanceAdjust();
    disFL = FrontL.getAverage();
    disFC = FrontC.getAverage();
    disFR = FrontR.getAverage();
    clearRMedian();

    //Use Front Left Sensor adjust distance
    if (disFL >= 8.2 && disFL <= 17)
    {
      if (disFL >= 8.2 && disFL < 10.5)
      {
        moveAdjustB();
        delay(10);      //original is delay(100) reduce to speedup
      }
      else if (disFL > 11.5 && disFL <= 17)
      {
        moveAdjustF();
        delay(10);
      }
    }
    //use front center
    else if (disFC >= 7.2 && disFC <= 16)
    {
      if (disFC >= 7.2 && disFC < 9.5)
      {
        moveAdjustB();
        delay(10);
      }
      else if (disFC > 10.5 && disFC <= 16)
      {
        moveAdjustF();
        delay(10);
      }
    }
    //use Front Right
    else if (disFR >= 8.2 && disFR <= 17)
    {
      if (disFR >= 8.2 && disFR < 10.5)
      {
        moveAdjustB();
        delay(10);      //original is delay(100) reduce to speedup
      }
      else if (disFR > 11.5 && disFR <= 17)
      {
        moveAdjustF();
        delay(10);
      }
    }
    else
    {
      break;
    }
  }
}

void stairCaseTest()
{
  getRMedian();
  FL = FrontL.getMedian() / 10 + 1;
  FC = FrontC.getMedian() / 10 + 1;
  FR = FrontR.getMedian() / 10 + 1;
  LF = LeftF.getMedian() / 10 + 1;
  LB = LeftB.getMedian() / 10 + 1;
  R = Right.getMedian() / 10 + 1;
  clearRMedian();

  if (FL == 1 && FC == 2)
  {
    adjustAngleStaircase("FL1", "FC2");
  }
  else if (FL == 2 && FC == 1)
  {
    adjustAngleStaircase("FL2", "FC1");
  }
  else if (FC == 1 && FR == 2)
  {
    adjustAngleStaircase("FC1", "FR2");
  }
  else if (FC == 2 && FR == 1)
  {
    adjustAngleStaircase("FC2", "FR1");
  }
}


void compute_mL_ticks()
{
  mLTicks++;
}

void compute_mR_ticks()
{
  mRTicks++;
}


//*md
//void moveDigonal()
//{
//  double dTotalTicks = 0;
//  double output;
//  double disFL, disFC, disFR, disLF, disLB, disR;
//
//  int pwm1 = 331, pwm2 = 322;
//
//  dTotalTicks = 310 / 10 * 100;  // *10 = 10cm
//
//  while (mLTicks < dTotalTicks)
//  {
//    disFL = readSensor(sensorFL, 0); //Calculate the distance in centimeters and store the value in a variable
//    disFR = readSensor(sensorFR, 0);
//    disFC = readSensor(sensorFC, 0);
//    disR = readLongRange(sensorR, 0);
//    disLF = readSensor(sensorLF, 0);
//    disLB = readSensor(sensorLB, 0);
//
//    //add the variables into arrays as samples
//    FrontR.add(disFR);
//    FrontL.add(disFL);
//    FrontC.add(disFC);
//    Right.add(disR);
//    LeftF.add(disLF);
//    LeftB.add(disLB);
//
//    if ((FrontC.getMedian()) < 20 || (FrontR.getMedian()) < 20 || (FrontL.getMedian()) < 20)
//    {
//      break;
//    }
//
//    output = pidControl(mLTicks, mRTicks);
//    md.setSpeeds(pwm1 - output * 5, pwm2 + output * 5);
//  }
//
//  brake();
//
//  delay(200);
//
//  while (1)
//  {
//    disFL = readSensor(sensorFL, 0); //Calculate the distance in centimeters and store the value in a variable
//    disFR = readSensor(sensorFR, 0);
//    disFC = readSensor(sensorFC, 0);
//    disR = readLongRange(sensorR, 0);
//    disLF = readSensor(sensorLF, 0);
//    disLB = readSensor(sensorLB, 0);
//
//    //add the variables into arrays as samples
//    FrontR.add(disFR);
//    FrontL.add(disFL);
//    FrontC.add(disFC);
//    Right.add(disR);
//    LeftF.add(disLF);
//    LeftB.add(disLB);
//
//
//    if ((FrontC.getMedian()) < 13 || (FrontR.getMedian()) < 13 || (FrontL.getMedian()) < 13)
//    {
//      moveAdjustB();
//      delay(10);
//    }
//    else
//    {
//      brake();
//      break;
//    }
//  }
//  delay(200);
//
//  if ((LeftF.getMedian() > 30 || LeftB.getMedian() > 30) && FrontR.getMedian() < 20 )
//  {
//    digonalLeft();
//
//  }
//  else if ((Right.getMedian()) > 30 && (FrontL.getMedian()) < 20 )
//  {
//    digonalRight();
//  }
//  else if (FrontC.getMedian() < 20)
//  {
//    digonalLeft();
//  }
//  else {
//    brake();
//  }
//
//}

//*a
//void avoid()
//{
//  double dTotalTicks = 0;
//  double output;
//  double disFL, disFC, disFR, disLF, disLB, disR;
//
//  int pwm1 = 331, pwm2 = 322;
//
//  dTotalTicks = 310 / 10 * 100;  // *10 = 10cm
//
//  while (mLTicks < dTotalTicks)
//  {
//    disFL = readSensor(sensorFL, 0); //Calculate the distance in centimeters and store the value in a variable
//    disFR = readSensor(sensorFR, 0);
//    disFC = readSensor(sensorFC, 0);
//    disR = readLongRange(sensorR, 0);
//    disLF = readSensor(sensorLF, 0);
//    disLB = readSensor(sensorLB, 0);
//
//    //add the variables into arrays as samples
//    FrontR.add(disFR);
//    FrontL.add(disFL);
//    FrontC.add(disFC);
//    Right.add(disR);
//    LeftF.add(disLF);
//    LeftB.add(disLB);
//
//    if ((FrontC.getMedian()) < 20 || (FrontR.getMedian()) < 20 || (FrontL.getMedian()) < 20)
//    {
//      break;
//    }
//
//    output = pidControl(mLTicks, mRTicks);
//    md.setSpeeds(pwm1 - output * 5, pwm2 + output * 5);
//  }
//
//  brake();
//  delay(200);
//
//  if (LeftF.getMedian() > 30 || LeftB.getMedian() > 30)
//  {
//    turnLeft(90);
//    delay(200);
//    moveSpeedup(2);
//    delay(200);
//    turnRight(90);
//    delay(200);
//    moveSpeedup(5);
//    delay(200);
//    turnRight(90);
//    delay(200);
//    moveSpeedup(2);
//    delay(200);
//    turnLeft(90);
//    delay(200);
//    moveSpeedup(2);
//    delay(200);
//  }
//  else if (Right.getMedian() > 30)
//  {
//    turnRight(90);
//    delay(200);
//    moveSpeedup(2);
//    delay(200);
//    turnLeft(90);
//    delay(200);
//    moveSpeedup(5);
//    delay(200);
//    turnLeft(90);
//    delay(200);
//    moveSpeedup(2);
//    delay(200);
//    turnRight(90);
//    delay(200);
//    moveSpeedup(2);
//    delay(200);
//  }
//  else
//  {
//    brake();
//  }
//}


//*dr
//void digonalRight()
//{
//  turnRight(45);
//  delay(250);
//  moveSpeedup(4);
//  delay(250);
//  turnLeft(45);
//  delay(250);
//  moveSpeedup(2);
//  delay(250);
//  turnLeft(90);
//  delay(250);
//  moveSpeedup(3);
//  delay(250);
//  turnRight(90);
//  delay(250);
//  moveSpeedup(2);
//}

//*dl
//void digonalLeft()
//{
//  turnLeft(45);
//  delay(250);
//  moveSpeedup(4);
//  delay(250);
//  turnRight(45);
//  delay(250);
//  moveSpeedup(2);
//  delay(250);
//  turnRight(90);
//  delay(250);
//  moveSpeedup(3);
//  delay(250);
//  turnLeft(90);
//  delay(250);
//  moveSpeedup(2);
//}

//void threeContMove()
//{
//  getRMedian2();
//  disFL = FrontL.getAverage();
//  disFC = FrontC.getAverage();
//  disFR = FrontR.getAverage();
//  disLF =  LeftF.getAverage();
//  disLB =  LeftB.getAverage();
//  clearRMedian();
//
//  if ((disLF > 5 && disLF < 14) && (disLB > 5 && disLB < 14))
//  {
//
//    if (((disFL > 5 && disFL < 14) && (disFC > 5 && disFC < 14)) || ((disFL > 5 && disFL < 14) && (disFR > 5 && disFR < 14)) || ((disFC > 5 && disFC < 14) && (disFR > 5 && disFR < 14)))
//    {
//      turnLeft(90);
//      autoCalibrate();
//      turnRight(90);
//      autoCalibrate();
//      Serial.println("Left and Front Wall condition!");
//    }
//    else
//    {
//      turnLeft(90);
//      autoCalibrate();
//      turnRight(90);
//      Serial.println("Left Wall condition!");
//    }
//
//  }
//  else if ( ((disFL > 5 && disFL < 14) && (disFC > 5 && disFC < 14)) || ((disFL > 5 && disFL < 14) && (disFR > 5 && disFR < 14)) || ((disFC > 5 && disFC < 14) && (disFR > 5 && disFR < 14)))
//  {
//    autoCalibrate();
//    Serial.println("Front Wall condition");
//  }
//}


//*debug
//void debug(String str)
//{
//  //Sample string        QC10L355R317T295
//  String outputString;
//  for (int i = 1; i < str.length(); i++)
//  {
//    outputString += str[i];
//  }
//  //  Serial.print("outputString:");
//  //  Serial.println(outputString);
//  int coefficient, leftpwm, righpwm, totaltick;
//
//  String value = "";
//
//  for (int i = 1; i < 3; i++)
//  {
//    value += outputString.charAt(i);
//  }
//  coefficient = value.toInt();
//  //  Serial.print("coefficient:");
//  //  Serial.println(coefficient);
//
//
//  value = "";
//  for (int i = 4; i < 7; i++)
//  {
//    value += outputString.charAt(i);
//  }
//  leftpwm = value.toInt();
//  //  Serial.print("leftpwm:");
//  //  Serial.println(leftpwm);
//
//
//  value = "";
//  for (int i = 8; i < 11; i++)
//  {
//    value += outputString.charAt(i);
//  }
//  righpwm = value.toInt();
//  //  Serial.print("righpwm:");
//  //  Serial.println(righpwm);
//
//
//  value = "";
//  int index = outputString.indexOf('T');
//  for (int i = index + 1; i < outputString.length(); i++)
//  {
//    value += outputString.charAt(i);
//  }
//  totaltick = value.toInt();
//  //  Serial.print("totaltick:");
//  //  Serial.println(totaltick);
//
//  double output;
//  double dTotalTicks = totaltick;
//
//  int pwm1 = -leftpwm, pwm2 = righpwm;
//
//  while (mLTicks < dTotalTicks)
//  {
//
//    output = pidControl(mLTicks, mRTicks);
//    md.setSpeeds(pwm1 - output * coefficient, pwm2 + output * coefficient);     //check coiffient for debug
//
//  }
//
//  brake();
//
//}


