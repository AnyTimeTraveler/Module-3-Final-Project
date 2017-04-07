SET GROUP="groep1"
SET WIRELESS_DEVICE="Wi-Fi 1"
SET COMPUTER_NUMBER=1

netsh wlan set profileparameter %GROUP% connectiontype=ibss connectionmode=manual
netsh wlan connect %GROUP%
netsh interface ipv4 set address %WIRELESS_DEVICE% static 192.168.5.%COMPUTER_NUMBER% 255.255.255.0
pause
