SET WIRELESS_DEVICE="Wi-Fi 1"

netsh wlan disconnect
netsh interface ipv4 set address %WIRELESS_DEVICE% dhcp
pause
