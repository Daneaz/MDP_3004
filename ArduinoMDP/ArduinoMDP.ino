#include <PinChangeInt.h>
#include <DualVNH5019MotorShield.h>
#include <SharpIR.h>

//motor pins
DualVNH5019MotorShield md(4, 2, 6, A0, 7, 8, 12, A1);

//sensor pins
SharpIR sensorFR(GP2Y0A21YK0F, A0);
SharpIR sensorFL(GP2Y0A21YK0F, A1);
SharpIR sensorFC(GP2Y0A21YK0F, A3);
SharpIR sensorR(GP2Y0A02YK0F, A2);    //Long range
SharpIR sensorLF(GP2Y0A21YK0F, A5);
SharpIR sensorLB(GP2Y0A21YK0F, A4);


volatile int mLTicks = 0;
volatile int mRTicks = 0;

boolean flag = false;

//global string for cmd
String inString = "";

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

  // Read string from serial
  while (Serial.available() > 0)
  {
    inString = Serial.readString();
  }

  // Only print when it is not empty string
  if (inString != "")
  {
    String decodeString;
    Serial.println(inString);
    char cmd = inString.charAt(0);
    Serial.println(cmd);

    for (int i = 1; i < inString.length(); i++)
    {
      decodeString += inString.charAt(i);
    }
    int target = decodeString.toInt();
    Serial.println(target);

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

void runCMD(char cmd, int target)
{
  switch (cmd)
  {
    case 'F':
      moveSpeedup(target);
      getSensorsData();
      break;
    case 'H':
      moveForward();
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
    case 'B':
      turnBack();
      getSensorsData();
      break;
    case 'N':
      turnRight360();
      getSensorsData();
      break;
    case 'S':
      moveBack(target);
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
    case 'A':
      realignment();
      break;
    case 'X':
      fastPath(inString);
      break;
    case 'Q':
      debug(inString);
      break;
    default:
      break;
  }

}

int pidControl(int LeftPosition, int RightPosition) {

  int error;
  int prev_error;
  double integral, derivative, output;
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


void debug(String str)
{
  //Sample string        QC10L355R317T295
  String outputString;
  for (int i = 1; i < str.length(); i++)
  {
    outputString += str[i];
  }
  Serial.print("outputString:");
  Serial.println(outputString);
  int coefficient, leftpwm, righpwm, totaltick;
  
  String value ="";
  
  for (int i = 1; i < 3; i++)
  {
    value += outputString.charAt(i);
  }
  coefficient = value.toInt();
  Serial.print("coefficient:");
  Serial.println(coefficient);

  
  value ="";
  for (int i = 4; i < 7; i++)
  {
    value += outputString.charAt(i);
  }
  leftpwm = value.toInt();
  Serial.print("leftpwm:");
  Serial.println(leftpwm);

  
  value ="";
  for (int i = 8; i < 11; i++)
  {
    value += outputString.charAt(i);
  }
  righpwm = value.toInt();
  Serial.print("righpwm:");
  Serial.println(righpwm);

  
  value ="";
  int index = outputString.indexOf('T');
  for (int i = index+1; i < outputString.length(); i++)
  {
    value += outputString.charAt(i);
  }
  totaltick = value.toInt();
  Serial.print("totaltick:");
  Serial.println(totaltick);
  
  double output;
  double dTotalTicks = totaltick;

  int pwm1 = leftpwm, pwm2 = righpwm;

  while (mLTicks < dTotalTicks)
  {
    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * coefficient, pwm2 + output * coefficient);     //check coiffient for debug
  }
  forwardBrake();
}


void moveSpeedup(int dis)
{
  double dTotalTicks = 0;
  double output;
  int count = 0;
  double avg, total = 0;

  //  int pwm1=355, pwm2=317; //SCSE
  int pwm1 = 355, pwm2 = 340; //SWL3
  //  int pwm1 = 245, pwm2 = 255; //low speed

  //  dTotalTicks = 295 / 10.0 * 10;  // *10 = 10cm
  if (dis <= 1)
  {
    dTotalTicks = 295;  // 1 box
  }
  else if (dis > 1 && dis <= 3 )
  {
    dTotalTicks = 295 * dis;  // 1 to 3 box
  }
  else if (dis > 3 && dis <= 5)
  {
    dTotalTicks = 295 * dis;  // 3 to 5 box
  }
  else if (dis > 5 && dis <= 7)
  {
    dTotalTicks = 295 * dis;  //5 to 7 box
  }
  else if (dis > 7 && dis <= 10)
  {
    dTotalTicks = 295 * dis;  //7 to 10 box
  }
  else if (dis > 10 && dis <= 13)
  {
    dTotalTicks = 295 * dis;  //10 to 13 box
  }

  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 15, pwm2 + output * 15);     //check coiffient for debug

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

  forwardBrake();

}

void moveForward() {

  double dTotalTicks = 0;
  double output;
  int count = 0;
  double avg, total = 0;
  int pwm1 = 245, pwm2 = 225;

  dTotalTicks = 285 / 10.0 * 100;  // *10 = 10cm

  while (mLTicks < dTotalTicks)
  {
    if (mLTicks <= 100)
    {
      pwm1 = 150;
      pwm2 = 115;
    }
    else if (mLTicks <= 200)
    {
      pwm1 = 250;
      pwm2 = 225;
    }
    else
    {
      pwm1 = 245;
      pwm2 = 225;
    }

    output = pidControl(mLTicks, mRTicks);

    md.setSpeeds(pwm1 - output * 3, pwm2 + output * 3);

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

void forwardBrake() {

  for (int i = 0; i < 3; i++)
  {
    md.setBrakes(370, 400);
  }

  delay(10);

  mLTicks = 0;
  mRTicks = 0;
}


void brake() {

  for (int i = 0; i < 3; i++)
  {
    md.setBrakes(400, 400);
  }

  delay(10);

  mLTicks = 0;
  mRTicks = 0;
}

void turnRight(int degree) {

  double dTotalTicks = 0;
  double output;
  int count = 0;
  double avg, total = 0;
  int pwm1 = 345, pwm2 = -335;

  if (degree == 0)
  {
    degree = 90;
  }

  if (degree <= 45)
  {
    dTotalTicks = 4.2222 * degree;  // 45 degree
  }
  else if (degree > 45 && degree <= 90 )
  {
    dTotalTicks = 4.63 * degree;  // 90 degree
  }
  else if (degree > 90 && degree <= 180)
  {
    dTotalTicks = 4.44 * degree;  // 180 degree
  }
  else if (degree > 180 && degree <= 360)
  {
    dTotalTicks = 4.44 * degree;  // 360 degree
  }

  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);

    md.setSpeeds(pwm1 + output * 3, pwm2 - output * 3);
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

void turnRight45() {

  double dTotalTicks = 0;
  double output;
  int count = 0;
  double avg, total = 0;
  int pwm1 = 355, pwm2 = -326;

  dTotalTicks = 191;  // 45 degree

  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);

    md.setSpeeds(pwm1 + output * 3, pwm2 - output * 3);
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


void turnLeft(int degree) {
  double dTotalTicks = 0;
  double output;
  int count = 0;
  double avg, total = 0;
  int pwm1 = -355, pwm2 = 326;

  if (degree == 0)
  {
    degree = 90;
  }

  if (degree <= 45)
  {
    dTotalTicks = 4.2222 * degree;  // 45 degree
  }
  else if (degree > 45 && degree <= 90 )
  {
    dTotalTicks = 4.63 * degree;  // 90 degree
  }
  else if (degree > 90 && degree <= 180)
  {
    dTotalTicks = 4.44 * degree;  // 180 degree
  }
  else if (degree > 180 && degree <= 360)
  {
    dTotalTicks = 4.44 * degree;  // 360 degree
  }

  while (mLTicks < dTotalTicks)
  {
    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 3, pwm2 + output * 3);

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

void turnLeft45() {
  double dTotalTicks = 0;
  double output;
  int count = 0;
  double avg, total = 0;
  int pwm1 = -355, pwm2 = 326;

  dTotalTicks = 191;  // 90 degree

  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);

    md.setSpeeds(pwm1 - output * 3, pwm2 + output * 3);
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

void turnBack() {

  double dTotalTicks = 0;
  double output;
  int count = 0;
  double avg, total = 0;
  int pwm1 = 355, pwm2 = -326;

  dTotalTicks = 800 / 180.0 * 180; // 90 degree

  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);

    md.setSpeeds(pwm1 + output * 3, pwm2 - output * 3);
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

void turnRight360() {

  double dTotalTicks = 0;
  double output;
  int count = 0;
  double avg, total = 0;
  int pwm1 = 355, pwm2 = -326;

  dTotalTicks = 1650 / 180.0 * 180; // 90 degree

  while (mLTicks < dTotalTicks)
  {

    output = pidControl(mLTicks, mRTicks);

    md.setSpeeds(pwm1 + output * 3, pwm2 - output * 3);
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
  int pwm1 = 355, pwm2 = 315;

  dTotalTicks = 28;  // *10 = 10cm

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

  dTotalTicks = 28;

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


double readSensor(SharpIR sensor, double offset)
{
  double distance;
  double avg = 0;
  for (int i = 0; i < 7; i++)
  {
    distance = sensor.getDistance() + offset;
    avg += distance;
  }
  avg = avg / 7;
  return avg;
}



void getSensorsData() {
  double disFL, disFC, disFR, disLF, disLB, disR;

  disFL = readSensor(sensorFL, 0); //Calculate the distance in centimeters and store the value in a variable
  disFR = readSensor(sensorFR, 0);
  disFC = readSensor(sensorFC, 0);
  disR = readSensor(sensorR, 0);
  disLF = readSensor(sensorLF, 0);
  disLB = readSensor(sensorLB, 0);

  //  Serial.print("FL:"); //Print the value to the serial monitor
  //  Serial.println(disFL);
  //  Serial.print("FC:");
  //  Serial.println( disFC);
  //  Serial.print("FR:");
  //  Serial.println( disFR);
  //  Serial.print("R:");
  //  Serial.println(disR);
  //  Serial.print("LF:");
  //  Serial.println(disLF);
  //  Serial.print("LB:");
  //  Serial.println(disLB);
  Serial.print("FL:"); //Print the value to the serial monitor
  Serial.print(disFL);
  Serial.print(",FC:");
  Serial.print( disFC);
  Serial.print(",FR:");
  Serial.print( disFR);
  Serial.print(",R:");
  Serial.print(disR);
  Serial.print(",LF:");
  Serial.print(disLF);
  Serial.print(",LB:");
  Serial.println(disLB);

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
  double disFL, disFC, disFR, disLF, disLB, disR;

  turnLeft(90);

  delay(200);
  //  disFL = readSensor(sensorFL,0); //Calculate the distance in centimeters and store the value in a variable
  disFR = readSensor(sensorFR, 0);
  disFC = readSensor(sensorFC, 0);
  disR = readSensor(sensorR, 0);
  disLF = readSensor(sensorLF, 0);
  disLB = readSensor(sensorLB, 0);

  if ( disFC < 10 || disFR < 10)
  {
    while (1)
    {
      //      disFL = readSensor(sensorFL,0); //Calculate the distance in centimeters and store the value in a variable
      disFR = readSensor(sensorFR, 0);
      disFC = readSensor(sensorFC, 0);
      disR = readSensor(sensorR, 0);
      disLF = readSensor(sensorLF, 0);
      disLB = readSensor(sensorLB, 0);

      if (disFC < 9.5 || disFR < 9.5)
      {
        moveAdjustB();
        delay(100);
      }
      else if (disFC > 10.5 || disFR > 10.5)
      {
        moveAdjustF();
        delay(100);
      }
      else
      {
        brake();
        break;
      }

    }
  }
  delay(100);
  turnRight(90);
}

void moveDigonal()
{
  double dTotalTicks = 0;
  double output;
  double disFL, disFC, disFR, disLF, disLB, disR;

  int pwm1 = 355, pwm2 = 317;

  dTotalTicks = 285 / 10.0 * 100;  // *10 = 10cm

  while (mLTicks < dTotalTicks)
  {
    //    disFL = readSensor(sensorFL,0); //Calculate the distance in centimeters and store the value in a variable
    disFR = readSensor(sensorFR, 0);
    disFC = readSensor(sensorFC, 0);
    disR = readSensor(sensorR, 0);
    disLF = readSensor(sensorLF, 0);
    disLB = readSensor(sensorLB, 0);


    if (disFC < 15 || disFR < 15)
    {
      break;
    }

    output = pidControl(mLTicks, mRTicks);
    md.setSpeeds(pwm1 - output * 15, pwm2 + output * 15);
  }

  brake();
  delay(200);
  while (1)
  {
    //    disFL = readSensor(sensorFL,0); //Calculate the distance in centimeters and store the value in a variable
    disFR = readSensor(sensorFR, 0);
    disFC = readSensor(sensorFC, 0);
    disR = readSensor(sensorR, 0);
    disLF = readSensor(sensorLF, 0);
    disLB = readSensor(sensorLB, 0);

    if (disFC < 13 || disFR < 13)
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
  if (disLF < 15 || disLB < 15)
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
  moveSpeedup(1);
  delay(250);
  moveSpeedup(1);
  delay(250);
  moveSpeedup(1);
  delay(250);
  moveSpeedup(1);
  delay(250);
  turnLeft45();
  delay(250);
  moveSpeedup(1);
  delay(250);

  realignment();
  moveSpeedup(1);
}

void digonalLeft()
{
  turnLeft45();
  delay(250);
  moveSpeedup(1);
  delay(250);
  moveSpeedup(1);
  delay(250);
  moveSpeedup(1);
  delay(250);
  moveSpeedup(1);
  delay(250);
  turnRight45();
  delay(250);
  moveSpeedup(1);
  delay(250);
  
  realignment();
  moveSpeedup(1);
}
void compute_mL_ticks()
{
  mLTicks++;
}

void compute_mR_ticks()
{
  mRTicks++;
}
