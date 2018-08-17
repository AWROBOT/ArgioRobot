#include <SoftwareSerial.h>
#include "Otto.h"

SoftwareSerial BTSerial(12, 13); // RX | TX

#define PIN_YL 2 //servo[0]
#define PIN_YR 3 //servo[1]
#define PIN_RL 4 //servo[2]
#define PIN_RR 5 //servo[3]

Otto Otto;

//-- Movement parameters
int T=1000;              //Initial duration of movement
int moveId=0;            //Number of movement
int modeId=0;            //Number of mode
int moveSize=15;         //Asociated with the height of some movements

volatile int MODE=0;

unsigned long previousMillis=0;

int randomDance=0;
int randomSteps=0;
bool obstacleDetected = false;

bool running = 0;
bool inited = 0;

char last_data;

void setup() 
{
  Serial.begin(9600);

  BTSerial.begin(38400);
}

void loop() 
{
  if(BTSerial.available() > 0)
  {
    char data =  BTSerial.read(); 

    //Serial.write(data);

    parseData(data);
  }

  switch (MODE) 
  {
      //-- MODE 0 - Awaiting
      //---------------------------------------------------------
      case 0:

        /*
        //Every 80 seconds in this mode, Otto falls asleep 
        if (millis()-previousMillis>=80000)
        {
            OttoSleeping_withInterrupts(); //ZZzzzzz...
            previousMillis=millis();         
        }
        */

        break;
      
      //-- MODE 1 - Dance Mode!
      //---------------------------------------------------------
      case 1:
        
        randomDance=random(5,21); //5,20
        if((randomDance>14)&&(randomDance<19))
        {
            randomSteps=1;
            T=1600;
        }
        else
        {
            randomSteps=random(3,6); //3,5
            T=1000;
        }

        for (int i=0;i<randomSteps;i++)
        {
            move(randomDance);
            
        }
        break;

      //-- MODE 2 - Obstacle detector mode
      //---------------------------------------------------------
      case 2:

        if(obstacleDetected)
        {
              Otto.sing(S_surprise);
              Otto.jump(5, 500);
              Otto.sing(S_cuddly);
            //Otto takes two steps back
            for(int i=0;i<3;i++)
            { 
              Otto.walk(1,1300,-1);
            }

            delay(100);
            obstacleDetector();
            delay(100);
           //If there are no obstacles and no button is pressed, Otto shows a smile
           if(obstacleDetected==true)
           {
            break;
           }            
           else
           {
              delay(50);
              obstacleDetector();
           } 
           
           //If there are no obstacles and no button is pressed, Otto shows turns left
           for(int i=0; i<3; i++)
           {
              if(obstacleDetected==true){break;}            
              else{ 
                  Otto.turn(1,1000,1); 
                  obstacleDetector();
              } 
           }
            
            //If there are no obstacles and no button is pressed, Otto is happy
            if(obstacleDetected==true){break;}           
            else{
                Otto.home();
                Otto.sing(S_happy_short);
                delay(200);
            }     
        
        }else{

            Otto.walk(1,1000,1); //Otto walk straight
            obstacleDetector();
        }   

        break;
        
      //-- MODE 3 - Teleoperation mode (listening SerialPort) 
      //---------------------------------------------------------
      case 3:

/*
        if(inited)
        {
          if (running) //Keep moving
          {
            parseData(last_data);
          }
          else
          {
            Otto.home();
          }
        }
        */

        Otto.home();
      
        break;      

      default:
          MODE=0;
          break;
    }
}

void parseData(char data)
{
    switch (data)
    {
        case 'I':
            Serial.println("init");
            if(!inited)
            {
              Otto.init(PIN_YL,PIN_YR,PIN_RL,PIN_RR,true);
              Otto.sing(S_connection);
              Otto.home();
              MODE = 0;
            }
            inited = 1;
            break;
      
        case 'W':
            Serial.println("walk");
            Otto.walk(1,T,FORWARD);
            running = 1;
            last_data = data;
            MODE = 3;
            break;

        case 'B':
            Serial.println("back");
            Otto.walk(1,T,BACKWARD);
            running = 1;
            last_data = data;
            MODE = 3;
            break;

        case 'L':
            Serial.println("left");
            Otto.turn(1,T,LEFT);
            running = 1;
            last_data = data;
            MODE = 3;
            break;

        case 'R':
            Serial.println("right");
            Otto.turn(1,T,RIGHT);
            running = 1;
            last_data = data;
            MODE = 3;
            break;

        case 'S':
            Serial.println("stop");
            running = 0;
            MODE = 0;
            break;

        case 'U':
            Serial.println("updown");
            //Otto.updown(1,T,moveSize);
            Otto.sing(S_surprise);
            Otto.jump(1,T);
            running = 0;
            MODE = 3;
            break;

        case 'M':
            Serial.println("moonwalk");
            if(random(0, 2) == 0)
            {
              Otto.sing(S_OhOoh);
              Otto.moonwalker(1,T,moveSize,LEFT);
            }
            else
            {
              Otto.sing(S_OhOoh2);
              Otto.moonwalker(1,T,moveSize,RIGHT);
            }
            running = 0;
            MODE = 3;
            break;

        case 'D':
            Serial.println("dance");
            MODE = 1;
            running = 0;
            break;

        case 'P':
            Serial.println("patrol");
            MODE = 2;
            running = 0;
            obstacleDetected = false;
            break;

        case 'T':
            Serial.println("talk");
            sing(random(1, 20));
            running = 0;
            MODE = 3;
            break;

        case 'X':
            Serial.println("random");
            move(random(8, 17));
            running = 0;
            MODE = 3;
            break;
    }
}

//-- Function to read distance sensor & to actualize obstacleDetected variable
void obstacleDetector()
{
   int distance = Otto.getDistance();

        if(distance<15)
        {
          obstacleDetected = true;
        }
        else
        {
          obstacleDetected = false;
        }
}

//-- Function to execute the right movement according the movement command received.
void move(int moveId)
{
  switch (moveId) {
    case 0:
      Otto.home();
      break;
    case 1: //M 1 1000 
      Otto.walk(1,T,1);
      break;
    case 2: //M 2 1000 
      Otto.walk(1,T,-1);
      break;
    case 3: //M 3 1000 
      Otto.turn(1,T,1);
      break;
    case 4: //M 4 1000 
      Otto.turn(1,T,-1);
      break;
    case 5: //M 5 1000 30 
      Otto.updown(1,T,moveSize);
      break;
    case 6: //M 6 1000 30
      Otto.moonwalker(1,T,moveSize,1);
      break;
    case 7: //M 7 1000 30
      Otto.moonwalker(1,T,moveSize,-1);
      break;
    case 8: //M 8 1000 30
      Otto.swing(1,T,moveSize);
      break;
    case 9: //M 9 1000 30 
      Otto.crusaito(1,T,moveSize,1);
      break;
    case 10: //M 10 1000 30 
      Otto.crusaito(1,T,moveSize,-1);
      break;
    case 11: //M 11 1000 
      Otto.jump(1,T);
      break;
    case 12: //M 12 1000 30 
      Otto.flapping(1,T,moveSize,1);
      break;
    case 13: //M 13 1000 30
      Otto.flapping(1,T,moveSize,-1);
      break;
    case 14: //M 14 1000 20
      Otto.tiptoeSwing(1,T,moveSize);
      break;
    /*
    case 15: //M 15 500 
      Otto.bend(1,T,1);
      break;
    case 16: //M 16 500 
      Otto.bend(1,T,-1);
      break;
    case 16: //M 17 500 
      Otto.shakeLeg(1,T,1);
      break;
    case 17: //M 18 500 
      Otto.shakeLeg(1,T,-1);
      break;
    */
    case 15: //M 19 500 20
      Otto.jitter(1,T,moveSize);
      break;
    case 16: //M 20 500 15
      Otto.ascendingTurn(1,T,moveSize);
      break;
    default:
      break;
  }    
}

void sing(int singId)
{
    switch (singId) 
    {
      case 1: //K 1 
        Otto.sing(S_connection);
        break;
      case 2: //K 2 
        Otto.sing(S_disconnection);
        break;
      case 3: //K 3 
        Otto.sing(S_surprise);
        break;
      case 4: //K 4 
        Otto.sing(S_OhOoh);
        break;
      case 5: //K 5  
        Otto.sing(S_OhOoh2);
        break;
      case 6: //K 6 
        Otto.sing(S_cuddly);
        break;
      case 7: //K 7 
        Otto.sing(S_sleeping);
        break;
      case 8: //K 8 
        Otto.sing(S_happy);
        break;
      case 9: //K 9  
        Otto.sing(S_superHappy);
        break;
      case 10: //K 10
        Otto.sing(S_happy_short);
        break;  
      case 11: //K 11
        Otto.sing(S_sad);
        break;   
      case 12: //K 12
        Otto.sing(S_confused);
        break; 
      case 13: //K 13
        Otto.sing(S_fart1);
        break;
      case 14: //K 14
        Otto.sing(S_fart2);
        break;
      case 15: //K 15
        Otto.sing(S_fart3);
        break;    
      case 16: //K 16
        Otto.sing(S_mode1);
        break; 
      case 17: //K 17
        Otto.sing(S_mode2);
        break; 
      case 18: //K 18
        Otto.sing(S_mode3);
        break;   
      case 19: //K 19
        Otto.sing(S_buttonPushed);
        break;                      
      default:
        break;
    }
}

void OttoSleeping_withInterrupts()
{
  int bedPos_0[4]={100, 80, 60, 120}; 

  
    Otto._moveServos(700, bedPos_0);  
  

  for(int i=0; i<4;i++)
  {
      Otto.bendTones (100, 200, 1.04, 10, 10);
    
      Otto.bendTones (200, 300, 1.04, 10, 10);  

      Otto.bendTones (300, 500, 1.04, 10, 10);   

    delay(500);
    
      Otto.bendTones (400, 250, 1.04, 10, 1); 

      Otto.bendTones (250, 100, 1.04, 10, 1); 
    
    delay(500);
  } 

    Otto.sing(S_cuddly);
 
  Otto.home();
}
