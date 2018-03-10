#include <PinChangeInt.h>
#include <DualVNH5019MotorShield.h>
#include <RunningMedian.h>
#include <SharpIR.h>

//
//#define sensorFR A0
//#define sensorFL A1
//#define sensorFC A3
//#define sensorR A2
//#define sensorLF A4
//#define sensorLB A5

//Sharp IR sensor define
SharpIR sensorFR(GP2Y0A21YK0F, A0);
SharpIR sensorFL(GP2Y0A21YK0F, A1);
SharpIR sensorFC(GP2Y0A21YK0F, A3);
SharpIR sensorR(GP2Y0A02YK0F, A2);
SharpIR sensorLF(GP2Y0A21YK0F, A5);
SharpIR sensorLB(GP2Y0A21YK0F, A4);
//motor pins
DualVNH5019MotorShield md(4, 2, 6, A0, 7, 8, 12, A1);

//Median variables
static RunningMedian FrontR = RunningMedian(300);
static RunningMedian FrontL = RunningMedian(300);
static RunningMedian FrontC = RunningMedian(300);
static RunningMedian Right = RunningMedian(300);
static RunningMedian LeftF = RunningMedian(300);
static RunningMedian LeftB = RunningMedian(300);

volatile int mLTicks = 0;
volatile int mRTicks = 0;

boolean flag = false;

//global string for cmd
String inString = "";


//global var for sensors
double disFL, disFC, disFR, disLF, disLB, disR;

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
  //  getSensorsData();
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

    //    if (cmd == 'M')
    //    {
    //      moveCount++;
    //    }
    //    else
    //    {
    //      moveCount = 0;
    //    }
    //
    //    if (moveCount => 3)
    //    {
    //      moveSpeedup(1);
    //      threeContMove();
    //      getSensorsData();
    //      moveCount = 0;
    //      Serial.println("Auto Calibrating Done! ");
    //    }
    //    else
    //    {
    runCMD(cmd, target);
    //    }

  }

  // Clear the string
  inString = "";
}

void threeContMove()
{
  getRMedian2();
  disFL = FrontL.getAverage();
  disFC = FrontC.getAverage();
  disFR = FrontR.getAverage();
  disLF =  LeftF.getAverage();
  disLB =  LeftB.getAverage();
  clearRMedian();

  if ((disLF > 5 && disLF < 14) && (disLB > 5 && disLB < 14))
  {

    if (((disFL > 5 && disFL < 14) && (disFC > 5 && disFC < 14)) || ((disFL > 5 && disFL < 14) && (disFR > 5 && disFR < 14)) || ((disFC > 5 && disFC < 14) && (disFR > 5 && disFR < 14)))
    {
      turnLeft(90);
      autoCalibrate();
      turnRight(90);
      autoCalibrate();
      Serial.println("Left and Front Wall condition!");
    }
    else
    {
      turnLeft(90);
      autoCalibrate();
      turnRight(90);
      Serial.println("Left Wall condition!");
    }

  }
  else if ( ((disFL > 5 && disFL < 14) && (disFC > 5 && disFC < 14)) || ((disFL > 5 && disFL < 14) && (disFR > 5 && disFR < 14)) || ((disFC > 5 && disFC < 14) && (disFR > 5 && disFR < 14)))
  {
    autoCalibrate();
    Serial.println("Front Wall condition");
  }
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
        moveSpeedup(dis);
        //        Serial.println("Fast F");
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
        turnLeft(180);
        //        Serial.println("Fast L");
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
      turnRight(90);
      autoCalibrate();
      //      getSensorsData();
      Serial.println("POK");
      break;
    case 'Q':
      turnLeft(90);
      autoCalibrate();
      turnRight(90);
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
    //check list function below
    case 'Z':
      moveDigonal();
      break;
    //debug function below
    case 'D':
      getSensorsData();
      break;
    case 'F':
      getSensorsData1();
      break;
    case 'G':
      getSensorsData2();
      break;
    case 'H':
      getSensorsData3();
      break;
    case 'N':
      adjustDistance();
      break;
    case '+':
      moveAdjustF();
      break;
    case '-':
      moveAdjustB();
      break;
    case 'E':
      debug(inString);
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
  double Ki = 0;

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

  //  int pwm1=355, pwm2=317; //SCSE
  //  int pwm1 = 331, pwm2 = 322; //SWL3
  //  int pwm1 = 245, pwm2 = 255; //low speed
  //  int pwm1 = 318, pwm2 = 353;
  int pwm1 = 254, pwm2 = 330;
  //  int pwm1 = 335, pwm2 = 318;
  //  dTotalTicks = 295 / 10.0 * 10;  // *10 = 10cm
  if (dis <= 1)
  {
    //    dTotalTicks = 310;  // 1 box
    dTotalTicks = 275;   //B2
  }
  else if (dis > 1 && dis <= 3 )
  {
    dTotalTicks = 300 * dis;  // 1 to 3 box
  }
  else if (dis > 3 && dis <= 5)
  {
    dTotalTicks = 295 * dis;  // 3 to 5 box
  }
  else if (dis > 5 && dis <= 7)
  {
    dTotalTicks = 300 * dis;  //5 to 7 box
  }
  else if (dis > 7 && dis <= 10)
  {
    dTotalTicks = 295 * dis;  //7 to 10 box
  }


  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 3, pwm2 + output * 3);    //check coiffient for debug

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
    dTotalTicks = 365;        //Battery 2
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
    dTotalTicks = 363;      //Battery 2
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

  dTotalTicks = 2;

  while (mLTicks < dTotalTicks)
  {
    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 , pwm2 );

  }
  brakeabit();
}


//double readSensor(uint8_t sensor, double offset)
//{
//  double data;
//  int distance;
//  //Reading analog voltage of sensor
//  data = analogRead(sensor);
//
//  if (sensor == sensorFR)
//    distance = (-0.000002519959460 * pow(data, 3) + 0.002437176903164 * pow(data, 2) - 0.833608599855960 * data + 114.319657177832000) - offset;
//  else if (sensor == sensorFL)
//    distance = (-0.000002664149856 * pow(data, 3) + 0.002448843508527 * pow(data, 2) - 0.792515291235626 * data + 104.390939725150000) - offset;
//  else if (sensor == sensorFC)
//    distance = (-0.000002017356684 * pow(data, 3) + 0.001994666767648 * pow(data, 2) - 0.709538365557925 * data + 102.098598425728000) - offset;
//  else if (sensor == sensorLF)
//    distance = (-0.000002412782701 * pow(data, 3) + 0.002504148776671 * pow(data, 2) - 0.887601420892029 * data + 123.895713653069000) - offset;
//  else if (sensor == sensorLB)
//    distance = (-0.000002327809079 * pow(data, 3) + 0.002391620795912 * pow(data, 2) - 0.846619727510112 * data + 120.305243788885000) - offset;
//  else // A2 long sensor
//  {
//    distance = (-0.000004468997192 * pow(data, 3) + 0.004620742694645 * pow(data, 2) - 1.683064107825460 * data + 244.115436152919000) - offset;
//    if (distance > 37 && distance < 70)
//      distance += 1;
//  }
//  return distance;
//}
//
double readSensor(SharpIR sensor, double offset)
{
  double dis;

  dis = sensor.getDistance() + offset;

  return dis;
}

//*grm
void getRMedian()
{
  for (int sCount = 0; sCount < 300 ; sCount++)
  {
    disFL = readSensor(sensorFL, -6); //Calculate the distance in centimeters and store the value in a variable

    disFC = readSensor(sensorFC, -5);
    disFR = readSensor(sensorFR, -7);


    disR = readSensor(sensorR, 1);
    if (disR <= 20)
    {
      disR -= 8;
    }
    else if (disR > 20 && disR < 25)
    {
      disR -= 2;
    }
    else if (disR > 40 && disR < 47)
    {
      disR += 3;
    }
    else if (disR > 50 && disR < 57)
    {
      disR += 3;
    }
    else if (disR > 60 && disR < 67)
    {
      disR += 5;
    }
    //    else if (disR >= 40 && disR <= 50)
    //    {
    //      disR += 8;
    //    }
    //
    //    if (disR > 44)
    //      disR += 5;
    //    else if (disR > 55)
    //      disR += 4;
    disLF = readSensor(sensorLF, -7);
    disLB = readSensor(sensorLB, -7);


    //add the variables into arrays as samples
    FrontR.add(disFR);
    FrontL.add(disFL);
    FrontC.add(disFC);
    Right.add(disR);
    LeftF.add(disLF);
    LeftB.add(disLB);
  }
}

void getRMedianFront()
{
  for (int sCount = 0; sCount < 20 ; sCount++)
  {
    disFL = readSensor(sensorFL, -5.8); //Calculate the distance in centimeters and store the value in a variable

    disFC = readSensor(sensorFC, -4.8);
    disFR = readSensor(sensorFR, -5.8);

    //add the variables into arrays as samples
    FrontR.add(disFR);
    FrontL.add(disFL);
    FrontC.add(disFC);
  }
}

void getRMedianLeft()
{
  for (int sCount = 0; sCount < 20 ; sCount++)
  {

    disLF = readSensor(sensorLF, -10);
    disLB = readSensor(sensorLB, -10);

    //add the variables into arrays as samples
    LeftF.add(disLF);
    LeftB.add(disLB);
  }
}


void getRMedian2()
{
  for (int sCount = 0; sCount < 20 ; sCount++)
  {
    disFL = readSensor(sensorFL, 0); //Calculate the distance in centimeters and store the value in a variable

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

  disFL = FrontL.getMedian();
  disFC = FrontC.getMedian();
  disFR = FrontR.getMedian();
  disLF = LeftF.getMedian();
  disLB = LeftB.getMedian();
  disR = Right.getMedian();


  //  if ((disFC > 5 && disFC < 15) || (disFL > 5 && disFL < 15) || (disFR > 5 && disFR < 15))
  //  {
  //    if((disFL > 5 && disFL < 15) && (disFC > 5 && disFC < 15))
  //    {
  //      autoCalibrate();
  //    }
  //    else if ((disFC > 5 && disFC < 15) && (disFR > 5 && disFR < 15))
  //    {
  //      autoCalibrate();
  //    }
  //    else if((disFL > 5 && disFL < 15) && (disFR > 5 && disFR < 15))
  //    {
  //      autoCalibrate();
  //    }
  //  }
  //  else if ((disLF >5 && disLF <15) && (disLB >5 && disLB <15))
  //  {
  //    turnLeft(90);
  //    autoCalibrate();
  //    turnRight(90);
  //  }

  //    if ((disLF >5 && disLF <15) && (disLB >5 && disLB <15))
  //    {
  //      turnLeft(90);
  //      autoCalibrate();
  //      turnRight(90);
  //    }



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

  clearRMedian();
}

void getSensorsData1() {

  getRMedianFront();
  Serial.print('P');
  Serial.print(FrontL.getAverage());
  Serial.print(",");
  Serial.print(FrontC.getAverage());
  Serial.print(",");
  Serial.println(FrontR.getAverage());

  clearRMedian();
}

void getSensorsData2() {

  getRMedianLeft();
  Serial.print('P');
  Serial.print(FrontL.getAverage());
  Serial.print(",");
  Serial.print(FrontC.getAverage());
  Serial.print(",");
  Serial.print(FrontR.getAverage());
  Serial.print(",");
  Serial.print(LeftF.getAverage());
  Serial.print(",");
  Serial.println(LeftB.getAverage());

  clearRMedian();
}

void getSensorsData3() {

  getRMedian2();
  Serial.print('P');
  Serial.print(FrontL.getAverage());
  Serial.print(",");
  Serial.print(FrontC.getAverage());
  Serial.print(",");
  Serial.print(FrontR.getAverage());
  Serial.print(",");
  Serial.print(LeftF.getAverage());
  Serial.print(",");
  Serial.println(LeftB.getAverage());

  clearRMedian();
}



void autoCalibrate()
{
  adjustDistance();
  adjustAngle();
}


void adjustAngle()
{

  double calibrateFront = 9;
  double calibrateFrontLeft = 9;
  double calibrateFrontRight = 9;

  double calibrateLeftFront = 9;
  double calibrateLeftBack = 9;

  double dErrorFront = 0;
  double dErrorFrontLeft = 0;
  double dErrorFrontRight = 0;

  double dErrorLeftFront = 0;
  double dErrorLeftBack = 0;

  double dErrorDiff_1 = 0, dErrorDiff_2 = 0, dErrorDiff_3 = 0, dErrorDiff_4 = 0;


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



    if ((disFC > 3 && disFC < 11) || (disFL > 3 && disFL < 11) || (disFR > 3 && disFR < 11)) // Front One Grid Angle Alignment
    {
      if ((disFL > 3 && disFL < 11) && (disFR > 3 && disFR < 11))
      {
        if (abs(dErrorDiff_1) < 0.5)
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
        if (abs(dErrorDiff_2) < 0.5)
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
        if (abs(dErrorDiff_3) < 0.5)
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

      //      Serial.println("Am here");
    }
    else
    {
      break;
    }
    //    Serial.println("Am NOT here");
  }


  preTime = millis();

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


    dErrorDiff_4 = dErrorLeftFront - dErrorLeftBack;

    if ((disLF > 3 && disLF < 11) && (disLB > 3 && disLB < 11))
    {

      if (abs(dErrorDiff_4) < 0.5)
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
    else
    {
      break;
    }
  }
  //  delay(200);
  //  Serial.println("am Wout");
}

//*ra
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
    getRMedian2();

    disFL = FrontL.getAverage();
    disFC = FrontC.getAverage();
    disFR = FrontR.getAverage();

    clearRMedian();

    //    if ((disFC >= 2 && disFC <= 4) || (disFL >= 4 && disFL <= 6) || (disFR >= 4 && disFR <= 6))
    //    if ((disFC > 2 && disFC <= 6) || (disFL > 4 && disFL <= 6) || (disFR > 4 && disFR <= 6))
    if (disFL >=9 && disFL < 10)
    {
      moveAdjustB();
      delay(100);
    }
    //    else if ((disFC > 8 && disFC <= 13) || (disFL > 8 && disFL <= 13) || (disFR > 8 && disFR <= 13))
    else if (disFL > 10 && disFL <= 13)
    {
      moveAdjustF();
      delay(100);
    }
    else
    {
      break;
    }
  }
}


//*md
void moveDigonal()
{
  double dTotalTicks = 0;
  double output;
  double disFL, disFC, disFR, disLF, disLB, disR;

  int pwm1 = 331, pwm2 = 322;

  dTotalTicks = 310 / 10 * 100;  // *10 = 10cm

  while (mLTicks < dTotalTicks)
  {
    disFL = readSensor(sensorFL, 0); //Calculate the distance in centimeters and store the value in a variable
    disFR = readSensor(sensorFR, 0);
    disFC = readSensor(sensorFC, 0);
    disR = readSensor(sensorR, 0);
    disLF = readSensor(sensorLF, 0);
    disLB = readSensor(sensorLB, 0);

    //add the variables into arrays as samples
    FrontR.add(disFR);
    FrontL.add(disFL);
    FrontC.add(disFC);
    Right.add(disR);
    LeftF.add(disLF);
    LeftB.add(disLB);

    if ((FrontC.getMedian()) < 20 || (FrontR.getMedian()) < 20 || (FrontL.getMedian()) < 20)
    {
      break;
    }

    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 5, pwm2 + output * 5);
  }

  brake();

  delay(200);

  while (1)
  {
    disFL = readSensor(sensorFL, 0); //Calculate the distance in centimeters and store the value in a variable
    disFR = readSensor(sensorFR, 0);
    disFC = readSensor(sensorFC, 0);
    disR = readSensor(sensorR, 0);
    disLF = readSensor(sensorLF, 0);
    disLB = readSensor(sensorLB, 0);

    //add the variables into arrays as samples
    FrontR.add(disFR);
    FrontL.add(disFL);
    FrontC.add(disFC);
    Right.add(disR);
    LeftF.add(disLF);
    LeftB.add(disLB);


    if ((FrontC.getMedian()) < 13 || (FrontR.getMedian()) < 13 || (FrontL.getMedian()) < 13)
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

  if ((LeftF.getMedian() > 30 || LeftB.getMedian() > 30) && FrontR.getMedian() < 20 )
  {
    digonalLeft();

  }
  else if ((Right.getMedian()) > 30 && (FrontL.getMedian()) < 20 )
  {
    digonalRight();
  }
  else if (FrontC.getMedian() < 20)
  {
    digonalLeft();
  }
  else {
    brake();
  }

}

//*a
void avoid()
{
  double dTotalTicks = 0;
  double output;
  double disFL, disFC, disFR, disLF, disLB, disR;

  int pwm1 = 331, pwm2 = 322;

  dTotalTicks = 310 / 10 * 100;  // *10 = 10cm

  while (mLTicks < dTotalTicks)
  {
    disFL = readSensor(sensorFL, 0); //Calculate the distance in centimeters and store the value in a variable
    disFR = readSensor(sensorFR, 0);
    disFC = readSensor(sensorFC, 0);
    disR = readSensor(sensorR, 0);
    disLF = readSensor(sensorLF, 0);
    disLB = readSensor(sensorLB, 0);

    //add the variables into arrays as samples
    FrontR.add(disFR);
    FrontL.add(disFL);
    FrontC.add(disFC);
    Right.add(disR);
    LeftF.add(disLF);
    LeftB.add(disLB);

    if ((FrontC.getMedian()) < 20 || (FrontR.getMedian()) < 20 || (FrontL.getMedian()) < 20)
    {
      break;
    }

    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 5, pwm2 + output * 5);
  }

  brake();
  delay(200);

  if (LeftF.getMedian() > 30 || LeftB.getMedian() > 30)
  {
    turnLeft(90);
    delay(200);
    moveSpeedup(2);
    delay(200);
    turnRight(90);
    delay(200);
    moveSpeedup(5);
    delay(200);
    turnRight(90);
    delay(200);
    moveSpeedup(2);
    delay(200);
    turnLeft(90);
    delay(200);
    moveSpeedup(2);
    delay(200);
  }
  else if (Right.getMedian() > 30)
  {
    turnRight(90);
    delay(200);
    moveSpeedup(2);
    delay(200);
    turnLeft(90);
    delay(200);
    moveSpeedup(5);
    delay(200);
    turnLeft(90);
    delay(200);
    moveSpeedup(2);
    delay(200);
    turnRight(90);
    delay(200);
    moveSpeedup(2);
    delay(200);
  }
  else
  {
    brake();
  }
}


//*dr
void digonalRight()
{
  turnRight(45);
  delay(250);
  moveSpeedup(4);
  delay(250);
  turnLeft(45);
  delay(250);
  moveSpeedup(2);
  delay(250);
  turnLeft(90);
  delay(250);
  moveSpeedup(3);
  delay(250);
  turnRight(90);
  delay(250);
  moveSpeedup(2);
}

//*dl
void digonalLeft()
{
  turnLeft(45);
  delay(250);
  moveSpeedup(4);
  delay(250);
  turnRight(45);
  delay(250);
  moveSpeedup(2);
  delay(250);
  turnRight(90);
  delay(250);
  moveSpeedup(3);
  delay(250);
  turnLeft(90);
  delay(250);
  moveSpeedup(2);
}


//*debug
void debug(String str)
{
  //Sample string        QC10L355R317T295
  String outputString;
  for (int i = 1; i < str.length(); i++)
  {
    outputString += str[i];
  }
  //  Serial.print("outputString:");
  //  Serial.println(outputString);
  int coefficient, leftpwm, righpwm, totaltick;

  String value = "";

  for (int i = 1; i < 3; i++)
  {
    value += outputString.charAt(i);
  }
  coefficient = value.toInt();
  //  Serial.print("coefficient:");
  //  Serial.println(coefficient);


  value = "";
  for (int i = 4; i < 7; i++)
  {
    value += outputString.charAt(i);
  }
  leftpwm = value.toInt();
  //  Serial.print("leftpwm:");
  //  Serial.println(leftpwm);


  value = "";
  for (int i = 8; i < 11; i++)
  {
    value += outputString.charAt(i);
  }
  righpwm = value.toInt();
  //  Serial.print("righpwm:");
  //  Serial.println(righpwm);


  value = "";
  int index = outputString.indexOf('T');
  for (int i = index + 1; i < outputString.length(); i++)
  {
    value += outputString.charAt(i);
  }
  totaltick = value.toInt();
  //  Serial.print("totaltick:");
  //  Serial.println(totaltick);

  double output;
  double dTotalTicks = totaltick;

  int pwm1 = -leftpwm, pwm2 = righpwm;

  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * coefficient, pwm2 + output * coefficient);     //check coiffient for debug

  }

  brake();

}


void compute_mL_ticks()
{
  mLTicks++;
}

void compute_mR_ticks()
{
  mRTicks++;
}
