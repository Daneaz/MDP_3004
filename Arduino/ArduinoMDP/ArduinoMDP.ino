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
static RunningMedian Right = RunningMedian(100);
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
  Serial.setTimeout(20);        // for reading string time out intervel
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
    //    Serial.println(cmd);
    //    Serial.println(dis);

    switch (cmd)
    {
      case 'M':
        fastForward(dis);
        //        Serial.println(dis);
        break;
      case 'R':
        turnRightFast(90);
        //        Serial.println("Fast R");
        break;
      case 'L':
        turnLeftFast(90);
        //        Serial.println("Fast L");
        break;
      case 'U':
        uTurn();
        //        Serial.println("Fast U");
        break;
    }
    cdis = "";
    dis = 0;
  }
  Serial.println("BS");
}

//*cmd
void runCMD(char cmd, int target)
{
  switch (cmd)
  {
    case 'M':
      moveSpeedup(target);
      getSensorsData(cmd);
      break;
    case 'R':
      turnRightFast(target);
      getSensorsData(cmd);
      break;
    case 'L':
      turnLeftFast(target);
      getSensorsData(cmd);
      break;
    case 'U':
      uTurn();
      getSensorsData(cmd);
      break;
    case 'S':
      moveBack(target);
      getSensorsData(cmd);
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
      adjustDistance();
      adjustAngleLeft();
      adjustAngleFront();
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
      getSensorsData(cmd);
      break;

    case 'P':
      autoCalibrate();
      turnRight(90);
      autoCalibrate();
      turnRight(90);
      for (int i = 0; i < 10; i++)
      {
        autoCalibrate();
        delay(100);
      }
      Serial.println("POK");
      break;

    //debug function
    case '/':
      stairCaseTest();
      break;
    case 'F':
      getSensorsDataFront();
      break;
    case 'G':
      getSensorsDataLeft();
      break;
    case 'H':
      getSensorsDataDistanceAdjust();
      break;
    case 'J':
      getSensorsDataActual();
      break;
    case 'K':
      getSensorsDataStairs();
      break;
    case '+':
      moveAdjustF();
      break;
    case '-':
      moveAdjustB();
      break;
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
  //      int count = 0;
  //      double avg, total = 0;

  //  int pwm1 = 282, pwm2 = 330;   Week 8
  int pwm1 = 370, pwm2 = 358;   //Battery 1
  //  int pwm1 = 360, pwm2 = 380; //Battery 2

  dTotalTicks = 261;

  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 10, pwm2 + output * 10);    //check coiffient for debug
    //            count++;
    //            total += abs(mLTicks - mRTicks);
    //            Serial.println(mLTicks - mRTicks);
    //            Serial.print(mLTicks);
    //            Serial.print("/");
    //            Serial.println(mRTicks);
  }
  //      avg = total / count;
  //      Serial.print("Avg:");
  //      Serial.println(avg);

  fbrake();

  //  movementCount++;
  //  if ( movementCount >= 5)
  //  {
  //    while (mLTicks < 12)
  //      md.setSpeeds(200, 0);
  //    movementCount = 0;
  //  }
  //  fbrake();
}

void fastForward(int dis)
{

  double dTotalTicks = 0;
  double output;
  //  int count = 0;
  //  double avg, total = 0;

  int pwm1 = 380, pwm2 = 384;

  if (dis <= 1)
  {
    dTotalTicks = 265;
  }
  else if (dis == 2 )
  {
    dTotalTicks = 278.5 * dis;
  }
  else if (dis == 3)
  {
    dTotalTicks = 280 * dis;
  }
  else if (dis == 4)
  {
    dTotalTicks = 288 * dis;
  }
  else if (dis == 5)
  {
    dTotalTicks = 289 * dis;
  }
  else if (dis == 6)
  {
    dTotalTicks = 290 * dis;
  }
  else if (dis == 7)
  {
    dTotalTicks = 291 * dis;
  }
  else if (dis == 8)
  {
    dTotalTicks = 292 * dis;
  }
  else if (dis == 9)
  {
    dTotalTicks = 293 * dis;
  }
  else if (dis == 10)
  {
    dTotalTicks = 293.5 * dis;
  }
  else if (dis == 11)
  {
    dTotalTicks = 294 * dis;
  }
  else if (dis == 12)
  {
    dTotalTicks = 295 * dis;
  }
  else if (dis == 13)
  {
    dTotalTicks = 295 * dis;
  }
  else if (dis == 14)
  {
    dTotalTicks = 295.5 * dis;
  }
  else if (dis == 15)
  {
    dTotalTicks = 296 * dis;
  }
  else if (dis == 16)
  {
    dTotalTicks = 296.5 * dis;
  }
  else if (dis == 17)
  {
    dTotalTicks = 297 * dis;
  }


  while (mLTicks < dTotalTicks)
  {
    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 5, pwm2 + output * 5);    //check coiffient for debug
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
}

void fbrake() {

  for (int i = 0; i < 10; i++)
  {
    //    md.setBrakes(390, 400);
    md.setBrakes(368, 400);

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

  dTotalTicks = 359;     //Battery 1
  //    dTotalTicks = 357;        //Battery 2
  while (mLTicks < dTotalTicks)
  {

    //    output = pidControl(mLTicks, mRTicks);

    //    md.setSpeeds(pwm1 + output * 3, pwm2 - output * 3);
    md.setSpeeds(pwm1 , pwm2);
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

  dTotalTicks = 361;    //Battery 1
  //    dTotalTicks = 360;      //Battery 2

  while (mLTicks < dTotalTicks)
  {
    //    output = pidControl(mLTicks, mRTicks);
    //    md.setSpeeds(pwm1 - output * 3, pwm2 + output * 3);
    md.setSpeeds(pwm1, pwm2);
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


//*tr
void turnRightFast(int degree) {

  double dTotalTicks = 0;
  double output;
  //  int count = 0;
  //  double avg, total = 0;

  int pwm1 = 344, pwm2 = -316;
  dTotalTicks = 350;

  while (mLTicks < dTotalTicks)
  {
    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 + output * 3, pwm2 - output * 3);
    //    md.setSpeeds(pwm1 , pwm2);
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


void turnLeftFast(int degree) {
  double dTotalTicks = 0;
  double output;
  //  int count = 0;
  //  double avg, total = 0;

  int pwm1 = -344, pwm2 = 316;
  dTotalTicks = 348;      //Battery 2
  while (mLTicks < dTotalTicks)
  {
    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 3, pwm2 + output * 3);
    //    md.setSpeeds(pwm1, pwm2);
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

void uTurn() {
  double dTotalTicks = 0;
  double output;
  //  int count = 0;
  //  double avg, total = 0;

  int pwm1 = -344, pwm2 = 316;
  dTotalTicks = 759;

  while (mLTicks < dTotalTicks)
  {
    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 3, pwm2 + output * 3);
    //    md.setSpeeds(pwm1, pwm2);
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

//read long range sensor data
double readLongRange(uint8_t sensor, double offset)
{
  double data;
  int distance;
  //Reading analog voltage of sensor
  data = analogRead(sensor);

  distance = (-0.00000082448212984193 * pow(data, 3) + 0.001131576262231 * pow(data, 2) - 0.601043546879649 * data + 139.718362171335) - offset;

  if (distance < 18)
    distance = 10;
  else if (distance < 25)
    distance = 20;
  else if (distance >= 25 &&  distance < 35)
    distance = 30;
  else if (distance >= 35 && distance < 45)
    distance = 40;
  else if (distance >= 45 && distance < 55)
    distance = 50;
  else if (distance >= 55 && distance < 65)
    distance = 60;
  return distance;
}

//read short range sensors data
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
  //sample 50 times for short range sensors
  for (int sCount = 0; sCount < 50 ; sCount++)
  {
    //Calculate the distance in centimeters and store the value in a variable
    disFL = readSensor(sensorFL, -2);
    disFC = readSensor(sensorFC, -1);
    disFR = readSensor(sensorFR, -2);
    disLF = readSensor(sensorLF, -7);
    disLB = readSensor(sensorLB, -7);

    //add the variables into arrays as samples
    FrontR.add(disFR);
    FrontL.add(disFL);
    FrontC.add(disFC);
    LeftF.add(disLF);
    LeftB.add(disLB);
  }
  //sample 100 times for short range sensors
  for (int sCount = 0; sCount < 100 ; sCount++)
  {
    disR = readLongRange(sensorR, 0);

    //add the variables into arrays as samples
    Right.add(disR);
  }

}

//seperate from the main function to save time
//get front sensor data only
//offset to increase the acurracy for short distance
void getRMedianFront()
{
  for (int sCount = 0; sCount < 50 ; sCount++)
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
  for (int sCount = 0; sCount < 50 ; sCount++)
  {

    disLF = readSensor(sensorLF, -10);
    disLB = readSensor(sensorLB, -10.1);

    //add the variables into arrays as samples
    LeftF.add(disLF);
    LeftB.add(disLB);
  }
}

//For distance adjustment
void getRMedianDistanceAdjust()
{
  for (int sCount = 0; sCount < 50 ; sCount++)
  {
    disFL = readSensor(sensorFL, 0);
    //add the variables into arrays as samples
    FrontL.add(disFL);

  }
}

void getRMedianStairs()
{
  for (int sCount = 0; sCount < 50 ; sCount++)
  {
    disFL = readSensor(sensorFL, 0);
    disFC = readSensor(sensorFC, 0);
    disLF = readSensor(sensorLF, 0);

    //add the variables into arrays as samples
    FrontL.add(disFL);
    FrontC.add(disFC);
    LeftF.add(disLF);
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

void getSensorsDataActual() {

  getRMedian();
  disFL = FrontL.getMedian() ;
  disFC = FrontC.getMedian() ;
  disFR = FrontR.getMedian() ;
  disLF = LeftF.getMedian() ;
  disLB = LeftB.getMedian() ;
  disR = Right.getMedian();
  clearRMedian();

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

//*gsd
void getSensorsData(char cmd) {

  getRMedian();
  FL = FrontL.getMedian() / 10 + 1;
  FC = FrontC.getMedian() / 10 + 1;
  FR = FrontR.getMedian() / 10 + 1;
  LF = LeftF.getMedian() / 10 + 1;
  LB = LeftB.getMedian() / 10 + 1;
  R = Right.getMedian() / 10 + 1;
  clearRMedian();


  checkForCalibration(cmd);
  delay(100);
  // Message to PC
  Serial.print('P');
  Serial.print(FL);
  Serial.print(",");
  Serial.print(FC);
  Serial.print(",");
  Serial.print(FR);
  Serial.print(",");
  Serial.print(LF);
  Serial.print(",");
  Serial.print(LB);
  Serial.print(",");
  Serial.println(R);
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
  Serial.println(FrontL.getAverage());
  clearRMedian();
}

//Debug function for calibration
void getSensorsDataStairs() {

  getRMedianStairs();
  Serial.print('P');
  Serial.print(FrontL.getAverage());
  Serial.print(",");
  Serial.print(FrontC.getAverage());
  Serial.print(",");
  Serial.println(LeftF.getAverage());
  clearRMedian();
}


int countFAndLWall = 0;
void checkForCalibration(char cmd)
{
  if ((FL  <= 1 && FC  <= 1) || (FL  <= 1 && FR  <= 1) || (FC  <= 1 && FR  <= 1) )
  {
    if (LF  <= 2 && LB  <= 2 && countFAndLWall >= 6 )
    {
      FrontAndLeftWall();
      adjustFailCount = 0;
      adjustFrontFailCount = 0;
      countFAndLWall = 0;
      //      Serial.println("Left and Front Wall");
    }
    else
    {
      FrontWall();
      adjustFrontFailCount = 0;
      adjustFailCount = 0;
      countFAndLWall++;
      //      Serial.println("Front Wall");
    }
  }
  //  else if ((LF  <= 1 && LB  <= 1) && previousLF  <= 1 && adjustFailCount >= 4 && (cmd != 'R' || cmd != 'L'))
  //  {
  //    LeftWall();
  //    adjustFrontFailCount++;
  //    countFAndLWall++;
  //    //    Serial.println("Left Wall");
  //  }
  else
  {
    adjustFrontFailCount++;
    adjustFailCount++;
    countFAndLWall++;
  }

  if ((adjustFrontFailCount >= 6 || adjustFailCount >= 8) && LF  <= 1 && LB <= 1 && (cmd != 'R' || cmd != 'L'))
  {
    turnLeft(90);
    adjustDistance();
    adjustAngleFront();
    delay(100);
    turnRight(90);
    delay(100);
    adjustFailCount = 0;
    adjustFrontFailCount = 0;
    countFAndLWall++;
    //    Serial.println("Turn Left Calibrate");
  }
  else if ((adjustFailCount >= 8 || adjustFrontFailCount >= 6) && (cmd != 'L' && cmd != 'R'))
  {
    if (previousR == 2 && R == 2)
    {
      turnRight(90);
      adjustDistance();
      adjustAngleFront();
      delay(100);
      turnLeft(90);
      delay(100);
      adjustFailCount = 0;
      countFAndLWall++;
      adjustFrontFailCount = 0;
      //      Serial.println("Turn Right Calibrate");
    }
    //    staircase angle calibration
    else if ((FL == 1 && FC == 2 && LF == 1) && adjustFailCount >= 15)
    {
      for (int i = 0; i < 20; i++ )
      {
        adjustAngleStaircase();
        countFAndLWall++;
      }
    }
  }

  previousR = R;
  previousLF = LF;

}

void FrontWall()
{
  for (int i = 0; i < 10; i++)
  {
    adjustDistance();
    adjustAngleFront();
  }
  movementCount = 0;
}

void LeftWall()
{
  for (int i = 0; i < 10; i++)
  {
    adjustAngleLeft();
  }
  movementCount = 0;
}

void FrontAndLeftWall()
{
  turnLeft(90);
  autoCalibrate();
  delay(300);
  turnRight(90);
  autoCalibrate();
  delay(300);
  movementCount = 0;
}

//auto calibration
void autoCalibrate()
{
  for (int i = 0; i < 10; i++)
  {
    adjustDistance();
    adjustAngleLeft();
    adjustAngleFront();
  }
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

    if (curTime - preTime > 3000)
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
          delay(50);
        }
        else
        {
          turnRightabit(1);
          delay(50);
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
          delay(50);
        }
        else
        {
          turnLeftabit(1);
          delay(50);
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
          delay(50);
        }
        else
        {
          turnRightabit(1);
          delay(50);
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

    if (curTime - preTime > 3000)
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

    if ((disLF > 1 && disLF < 18) && (disLB > 1 && disLB < 18))
    {

      if (abs(dErrorDiff) < 0.3)
      {
        break;
      }
      if (dErrorLeftFront < dErrorLeftBack)
      {
        turnRightabit(1);
        delay(50);
      }
      else if (dErrorLeftFront > dErrorLeftBack)
      {
        turnLeftabit(1);
        delay(50);
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

void adjustAngleStaircase()
{
  //error variables
  double calibrateFL = 11;
  double calibrateFC = 19;
  double calibrateLF = 10;

  double dErrorFL = 0;
  double dErrorFC = 0;
  double dErrorLF = 0;

  double dErrorDiffFLFC = 0, dErrorDiffFLLF = 0;

  //timer for break the loop in case stuck in the loop
  unsigned long preTime = millis();
  unsigned long curTime = 0;
  bool disFlag = true;
  while (1)
  {
    curTime = millis();

    if (curTime - preTime > 3000)
    {
      break;
    }

    getRMedianStairs();
    disFL = FrontL.getAverage();
    disFC = FrontC.getAverage();
    disLF = LeftF.getAverage();
    clearRMedian();

    dErrorFL =  disFL - calibrateFL;
    dErrorFC =  disFC - calibrateFC;
    dErrorLF =  disLF - calibrateLF;

    dErrorDiffFLFC = dErrorFL - dErrorFC;
    dErrorDiffFLLF = dErrorFL - dErrorLF;

    if ((disFL > 3 && disFL < 15) && (disFC > 3 && disFC < 25) && (disLF > 3 && disLF < 15) )
    {
      if (disFlag)
      {
        turnLeftFast(90);
        adjustDistanceFR();
        turnRightFast(90);
        disFlag = false;
      }
      if (abs(dErrorDiffFLFC) < 0.3 && abs(dErrorDiffFLLF) < 0.3)
      {
        break;
      }
      if ((dErrorFL < dErrorFC)  && (dErrorFL < dErrorLF))
      {
        turnLeftabit(1);
        delay(50);
      }
      else
      {
        turnRightabit(1);
        delay(50);
      }
    }
    //    else if ((disFL > 3 && disFL < 15) && (disLF > 3 && disLF < 15))
    //    {
    //      if (abs(dErrorDiffFLLF) < 0.3)
    //      {
    //        break;
    //      }
    //      if (dErrorFL < dErrorLF)
    //      {
    //        turnLeftabit(1);
    //        delay(50);
    //      }
    //      else
    //      {
    //        turnRightabit(1);
    //        delay(50);
    //      }
    //    }
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
    if (curTime - preTime > 3000)
    {
      break;
    }
    getRMedianDistanceAdjust();
    disFL = FrontL.getAverage();
    disFC = FrontC.getAverage();
    disFR = FrontR.getAverage();
    clearRMedian();

    //Use Front Left Sensor adjust distance
    if (disFL >= 7.2 && disFL < 9.5)
    {
      moveAdjustB();
      delay(50);      //original is delay(100) reduce to speedup
    }
    else if (disFL > 10.5 && disFL <= 16)
    {
      moveAdjustF();
      delay(50);
    }
    else
    {
      break;
    }
  }
}

void adjustDistanceFR()
{
  unsigned long preTime = millis();
  unsigned long curTime = 0;

  while (1)
  {
    curTime = millis();
    if (curTime - preTime > 3000)
    {
      break;
    }
    getRMedianDistanceAdjust();
    disFL = FrontL.getAverage();
    disFC = FrontC.getAverage();
    disFR = FrontR.getAverage();
    clearRMedian();

    //Use Front Left Sensor adjust distance
    if (disFR >= 7.2 && disFR < 9.5)
    {
      moveAdjustB();
      delay(50);      //original is delay(100) reduce to speedup
    }
    else if (disFR > 10.5 && disFR <= 16)
    {
      moveAdjustF();
      delay(50);
    }
    else
    {
      break;
    }
  }
}

void stairCaseTest()
{
  for (int i = 0; i < 10; i++)
  {
    adjustAngleStaircase();
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


