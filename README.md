## Rpi Password

Wifi SSID:MDPGrp13

Password:2018Grp13

SSH Command:ssh pi@192.168.13.1

Password: 2018_MDP_Group13


----
## 'Address already in use' issue solution
1. Check the Process_ID(:5182 is the host since we've been running on 192.168.13.1:5182): $ lsof -i :5182

2. Then kill it: $ sudo kill -9 Process_ID
