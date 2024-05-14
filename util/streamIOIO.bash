#!//usr/bin/env bash
windowLen=1000
updateTime=0.001
{
adb logcat -c
while true; do
	sleep 1; 
	adb logcat -v raw pwmMsg:I *:S; 
done
} | feedgnuplot --lines --stream $updateTime --exit --title 'Pulse Width Modulation Values' --xlen $windowLen --ylabel 'Pulse Width (microseconds) - 1000 being always high' --xlabel 'sample' --legend 0 'pulseWidthRightWheelCcurrent' --legend 1 'pulseWidthLeftWheelCurrent' --legend 2 'pulseWidthRightWheelNew' --legend 3 'pulseWidthLeftWheelNew' &

{
adb logcat -c
while true; do
	sleep 1; 
	adb logcat -v raw encoderStateMsg:I *:S; 
done
} | feedgnuplot --lines --stream $updateTime --exit --title 'Wheel Encoder States' --xlen 100 --ylabel 'Boolean True/False' --xlabel 'sample' --legend 0 'encoderARightWheel' --legend 1 'encoderBRightWheel' --legend 2 'encoderALeftWheel' --legend 3 'encoderBLeftWheel' &

{
adb logcat -c
while true; do
	sleep 1; 
	adb logcat -v raw encoderCountMsg:I *:S; 
done
} | feedgnuplot --lines --stream $updateTime --exit --title 'Wheel Encoder Counts' --xlen $windowLen --ylabel 'Encoder Counts' --xlabel 'sample' --legend 0 'encoderCountRightWheelNew' --legend 1 'encoderCountRightWheelCurrent' --legend 2 'encoderCountLeftWheelNew' --legend 3 'encoderCountLeftWheelCurrent'  
