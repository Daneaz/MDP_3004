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

SharpIR sensorFR(GP2Y0A21YK0F, A0);
SharpIR sensorFL(GP2Y0A21YK0F, A1);
SharpIR sensorFC(GP2Y0A21YK0F, A3);
SharpIR sensorR(GP2Y0A02YK0F, A2);
SharpIR sensorLF(GP2Y0A21YK0F, A4);
SharpIR sensorLB(GP2Y0A21YK0F, A5);
//motor pins
DualVNH5019MotorShield md(4, 2, 6, A0, 7, 8, 12, A1);

//Median variables
static RunningMedian FrontR = RunningMedian(100);
static RunningMedian FrontL = RunningMedian(100);
static RunningMedian FrontC = RunningMedian(100);
static RunningMedian Right = RunningMedian(100);
static RunningMedian LeftF = RunningMedian(100);
static RunningMedian LeftB = RunningMedian(100);

volatile int mLTicks = 0;
volatile int mRTicks = 0;

boolean flag = false;

//global string for cmd
String inString = "";
static double disFL, disFC, disFR, disLF, disLB, disR;
void setup() {
  // put your setup code here, to run once:
  pinMode(4, INPUT);  //Interrupt Pin 4
  pinMode(13, INPUT); //Interrupt Pin 13

  md.init();

  PCintPort::attachInterrupt(11, &compute_mL_ticks, RISING);  //Attached to Pin 11
  PCintPort::attachInterrupt(3, &compute_mR_ticks, RISING); //Attached to Pin 3

  Serial.begin(115200);
  Serial.setTimeout(10);
  //  Serial.println("Waiting for data: ");
}



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

    runCMD(cmd, target);

  }

  // Clear the string
  inString = "";
}

void fastPath(String str)
{

  for (int i = 1; i < str.length(); i++)
  {
    switch (str[i])
    {
      case 'F':
        //        moveSpeedup();
        //        getSensorsData();
        Serial.println("Fast F");
        break;
      case 'R':
        //        turnRight(90);
        //        getSensorsData();
        Serial.println("Fast R");
        break;
      case 'L':
        //        turnLeft(90);
        //        getSensorsData();
        Serial.println("Fast L");
        break;
    }
  }
}

//*cmd
void runCMD(char cmd, int target)
{
  switch (cmd)
  {
    case 'M':
      wall();
      moveSpeedup(target);

      break;
    case 'R':
      wall();
      turnRight(target);

      break;
    case 'L':
      wall();
      turnLeft(target);

      break;
    case 'U':
      wall();
      turnLeft(180);

      break;
    case 'N':
      turnLeft(360);

      break;
    case 'S':
      moveBack(target);

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
    case '[':
      turnLeft(45);
      break;
    case ']':
      turnRight(45);
      break;
    case 'Z':
      moveDigonal();
      break;
    case 'A':
      wall();
      break;
    case 'X':
      fastPath(inString);
      break;
    case 'Q':
      debug(inString);
      break;
    case 'E':
      debug1(inString);
      break;
    case 'W':
      debug2(inString);
      break;
    case 'F':
      debug3(inString);
      break;
    case 'C':
      wall();
      delay(2000);
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
  double Kp = 5.5;                  //prefix Kp Ki, Kd dont changed if want to changed pls re declared
  double Kd = 0.4;
  double Ki = 0.1;

  error = LeftPosition - RightPosition;
  integral += error;
  derivative = (error - prev_error);

  output = Kp * error + Ki * integral + Kd * derivative;
  prev_error = error;
  return output;
}


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

  int pwm1 = leftpwm, pwm2 = righpwm;
    int count = 0;
  double avg, total = 0;

  while (mLTicks < dTotalTicks)
  {

    count++;
    total += abs(mLTicks - mRTicks);
    Serial.println(mLTicks - mRTicks);
    Serial.print(mLTicks);
    Serial.print("/");
    Serial.println(mRTicks);
  }
  avg = total / count;
  Serial.print("Avg:");
  Serial.println(avg);

  brake();

}

//*debug
void debug1(String str)
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

  int pwm1 = leftpwm, pwm2 = -righpwm;

  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * coefficient, pwm2 + output * coefficient);     //check coiffient for debug

  }

  brake();

}

void debug2(String str)
{
  //Sample string        WC10L355R317T295
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

  int pwm1 = -leftpwm, pwm2 = -righpwm;

  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * coefficient, pwm2 + output * coefficient);     //check coiffient for debug

  }

  brake();

}

void debug3(String str)
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

  int pwm1 = leftpwm, pwm2 = righpwm;

  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * coefficient, pwm2 + output * coefficient);     //check coiffient for debug

  }

  brake();

}

//*msu
void moveSpeedup(int dis)
{

  double dTotalTicks = 0;
  double output;
  int count = 0;
  double avg, total = 0;

  //  int pwm1=355, pwm2=317; //SCSE
  //  int pwm1 = 331, pwm2 = 322; //SWL3
  //  int pwm1 = 245, pwm2 = 255; //low speed
//  int pwm1 = 318, pwm2 = 353;
int pwm1 = 355, pwm2 = 320;
  //  dTotalTicks = 295 / 10.0 * 10;  // *10 = 10cm
  if (dis <= 1)
  {
    //    dTotalTicks = 310;  // 1 box
    dTotalTicks = 310;   //B2
  }
  else if (dis > 1 && dis <= 3 )
  {
    dTotalTicks = 310 * dis;  // 1 to 3 box
  }
  else if (dis > 3 && dis <= 5)
  {
    dTotalTicks = 310 * dis;  // 3 to 5 box
  }
  else if (dis > 5 && dis <= 7)
  {
    dTotalTicks = 310 * dis;  //5 to 7 box
  }
  else if (dis > 7 && dis <= 10)
  {
    dTotalTicks = 310 * dis;  //7 to 10 box
  }
  else if (dis > 10 && dis <= 13)
  {
    dTotalTicks = 310 * dis;  //10 to 13 box
  }

  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 3, pwm2 + output * 3);     //check coiffient for debug

    count++;
    total += abs(mLTicks - mRTicks);
    Serial.println(mLTicks - mRTicks);
    Serial.print(mLTicks);
    Serial.print("/");
    Serial.println(mRTicks);
  }
  avg = total / count;
  Serial.print("Avg:");
  Serial.println(avg);

  brake();


}

//*b
void brake() {

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
  int count = 0;
  double avg, total = 0;
  int pwm1 = 355, pwm2 = -326;        //Battery 1
  //  int pwm1 = 355, pwm2 = -326;          //Battery 2

  if (degree == 0)
  {
    //    dTotalTicks = 391;
    //    dTotalTicks = 379 / 90 * 90;     //Battery 1
    dTotalTicks = 390 / 90 * 90;        //Battery 2
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
    count++;
    total += abs(mLTicks - mRTicks);
    Serial.println(mLTicks - mRTicks);
    Serial.print(mLTicks);
    Serial.print("/");
    Serial.print(mRTicks);
    Serial.print("/");
    Serial.println(dTotalTicks);
  }
  avg = total / count;
  Serial.print("Avg:");
  Serial.println(avg);

  brake();
}


void turnLeft(int degree) {
  double dTotalTicks = 0;
  double output;
    int count = 0;
    double avg, total = 0;
  int pwm1 = -355, pwm2 = 326;    //Battery 1
  //    int pwm1 = -355, pwm2 = 326;        //Battery 2
  if (degree == 0)
  {
    //    dTotalTicks = 405 / 90 * 90;    //Battery 1
    dTotalTicks = 418;      //Battery 2
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

        count++;
        total += abs(mLTicks-mRTicks);
        Serial.println(mLTicks-mRTicks);
        Serial.print(mLTicks);
        Serial.print("/");
        Serial.print(mRTicks);
        Serial.print("/");
        Serial.println(dTotalTicks);
  }
    avg =total/count;
    Serial.print("Avg:");
    Serial.println(avg);

  brake();
}


void moveBack(int dis)
{
  double dTotalTicks = 0;
  double output;
  int count = 0;
  double avg, total = 0;
  int pwm1 = -355, pwm2 = -315;

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
  int count = 0;
  double avg, total = 0;
  int pwm1 = 333, pwm2 = 353;

  dTotalTicks = 10;  // *10 = 10cm

  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 5, pwm2 + output * 5);

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
  double dTotalTicks = 0;
  double output;
  int count = 0;
  double avg, total = 0;
  int pwm1 = -355, pwm2 = -315;
  //    int pwm1 = -150, pwm2 = -150;

  dTotalTicks = 10;

  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 5, pwm2 + output * 5);

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
  double data;
  //  int distance;
  //  double avg=0;
  //Reading analog voltage of sensor
  data = sensor.getDistance() + offset;

  return data;
}

//*grm
void getRMedian()
{
  for (int sCount = 0; sCount < 100 ; sCount++)
  {
    disFL = readSensor(sensorFL, -2); //Calculate the distance in centimeters and store the value in a variable
    disFR = readSensor(sensorFR, -3);
    disFC = readSensor(sensorFC, -3);
    disR = readSensor(sensorR, 8);
    if (disR > 44)
      disR += 5;
    else if (disR > 55)
      disR += 4;
    disLF = readSensor(sensorLF, 0);
    disLB = readSensor(sensorLB, 0);


    //add the variables into arrays as samples
    FrontR.add(disFR);
    FrontL.add(disFL);
    FrontC.add(disFC);
    Right.add(disR);
    LeftF.add(disLF);
    LeftB.add(disLB);


    //    Serial.print("FL:"); //Print the value to the serial monitor
    //    Serial.println(disFL);

  }
}

void getRMedian1()
{
  for (int sCount = 0; sCount < 10 ; sCount++)
  {
    disFL = readSensor(sensorFL, -2); //Calculate the distance in centimeters and store the value in a variable
    disFR = readSensor(sensorFR, -1);
    disFC = readSensor(sensorFC, -3);
    disR = readSensor(sensorR, 8);
    if (disR > 44)
      disR += 5;
    else if (disR > 55)
      disR += 4;
    disLF = readSensor(sensorLF, 0);
    disLB = readSensor(sensorLB, 0);


    //add the variables into arrays as samples
    FrontR.add(disFR);
    FrontL.add(disFL);
    FrontC.add(disFC);
    Right.add(disR);
    LeftF.add(disLF);
    LeftB.add(disLB);


    //    Serial.print("FL:"); //Print the value to the serial monitor
    //    Serial.println(disFL);

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
  Serial.print('P');
  Serial.print(FrontL.getMedian());
  Serial.print(",");
  Serial.print(FrontC.getMedian());
  Serial.print(",");
  Serial.print(FrontR.getMedian());
  Serial.print(",");
  Serial.print(LeftF.getMedian());
  Serial.print(",");
  Serial.print(LeftB.getMedian());
  Serial.print(",");
  Serial.println(Right.getMedian());
  clearRMedian();


  //  getRMedian();
  //  Serial.print("FL:"); //Print the value to the serial monitor
  //  Serial.print(FrontL.getMedian());
  //  Serial.print(",FC:");
  //  Serial.print(FrontC.getMedian());
  //  Serial.print(",FR:");
  //  Serial.print( FrontR.getMedian());
  //  Serial.print(",R:");
  //  Serial.print(Right.getMedian());
  //  Serial.print(",LF:");
  //  Serial.print(LeftF.getMedian());
  //  Serial.print(",LB:");
  //  Serial.println(LeftB.getMedian());
  //  clearRMedian();


}

void wall()
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

    if (curTime - preTime > 3000)
    {
      break;
    }

    getRMedian1();

    disLF = LeftF.getMedian();
    disLB = LeftB.getMedian();

    clearRMedian();

    dErrorLeftFront =  disLF - calibrateLeftFront;
    dErrorLeftBack =  disLB - calibrateLeftBack;


    dErrorDiff_4 = dErrorLeftFront - dErrorLeftBack;

    if ((disLF > 5 && disLF < 20) && (disLB > 5 && disLB < 20))
    {

      if (abs(dErrorDiff_4) < 0.5)
      {
        break;
      }
      if (dErrorLeftFront < dErrorLeftBack)
      {

        turnLeft(1);
      }
      else if (dErrorLeftFront > dErrorLeftBack)
      {

        turnRight(1);
      }

    }
    else
    {
      break;
    }

  }


  preTime = millis();

  while (1)
  {
    curTime = millis();

    if (curTime - preTime > 3000)
    {
      break;
    }

    getRMedian1();

    disFL = FrontL.getMedian();
    disFC = FrontC.getMedian();
    disFR = FrontR.getMedian();

    clearRMedian();

    dErrorFront =  disFC - calibrateFront;
    dErrorFrontLeft =  disFL - calibrateFrontLeft;
    dErrorFrontRight =  disFR - calibrateFrontRight;


    dErrorDiff_1 = dErrorFrontLeft - dErrorFrontRight;
    dErrorDiff_2 = dErrorFront - dErrorFrontLeft;
    dErrorDiff_3 = dErrorFront - dErrorFrontRight;



    if ((disFC > 5 && disFC < 20) && (disFL > 5 && disFL < 20) && (disFR > 5 && disFR < 20)) // Front One Grid Angle Alignment
    {
      if ((disFL > 5 && disFL < 20) && (disFR > 5 && disFR < 20))
      {
        if (abs(dErrorDiff_1) < 0.5)
        {
          break;
        }

        if (dErrorFrontLeft < dErrorFrontRight)
        {
          turnLeft(1);
        }
        else
        {
          turnRight(1);
        }
      }
      else if ((disFC > 5 && disFC < 20) && (disFL > 5 && disFL < 20))
      {
        if (abs(dErrorDiff_2) < 0.5)
        {
          break;
        }

        if (dErrorFrontLeft > dErrorFront)
        {
          turnRight(1);
        }
        else
        {
          turnLeft(1);
        }
      }
      else if ((disFC > 5 && disFC < 20) && (disFR > 5 && disFR < 20))
      {
        if (abs(dErrorDiff_3) < 0.5)
        {
          break;
        }

        if (dErrorFrontRight > dErrorFront)
        {
          turnLeft(1);
        }
        else
        {
          turnRight(1);
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
  //  Serial.println("am Wout");
}

//*ra
void realignment()
{
  //  turnLeft(90);
  //  delay(200);
  getRMedian();
  unsigned long preTime = millis();
  unsigned long curTime = 0;

  if ( FrontC.getMedian() < 10 || FrontR.getMedian() < 10 || FrontL.getMedian() < 10)
  {

    while (1)
    {
      clearRMedian();
      curTime = millis();

      if (curTime - preTime > 5000)
      {
        break;
      }
      getRMedian();

      if ((FrontC.getMedian()) < 10 || (FrontR.getMedian()) < 10 || (FrontL.getMedian()) < 10)
      {
        moveAdjustB();
        delay(50);
      }
      else if ((FrontR.getMedian()) > 13 || (FrontC.getMedian()) > 13 || (FrontL.getMedian()) > 13)
      {
        moveAdjustF();
        delay(50);
      }
      else
      {
        brake();
        break;
      }
      delay(50);

      //debug
      //      Serial.print("FL:"); //Print the value to the serial monitor
      //      Serial.print(FrontL.getMedian());
      //      Serial.print(",FC:");
      //      Serial.print(FrontC.getMedian());
      //      Serial.print(",FR:");
      //      Serial.print( FrontR.getMedian());
      //      Serial.print(",R:");
      //      Serial.print(Right.getMedian());
      //      Serial.print(",LF:");
      //      Serial.print(LeftF.getMedian());
      //      Serial.print(",LB:");
      //      Serial.println(LeftB.getMedian());
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
void compute_mL_ticks()
{
  mLTicks++;
}

void compute_mR_ticks()
{
  mRTicks++;
}
