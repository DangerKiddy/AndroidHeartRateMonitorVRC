# HeartRateMonitorVRC Android App
An androind application for streaming heart rate data from a pulse oximeter to a PC with [HeartRateMonitorVRC](https://github.com/DangerKiddy/HeartRateMonitorVRC) installed, to send data to VRChat. Used as a helper app in case if your PC don't have Bluetooth adapter/built-in Bluetooth.

## Requirements
- Phone with Android 13 or newer
- Pulseoximeter with BluetoothLE (Low energy) support, such as CooSpo H808S (Tested only with that device)
- PC connected to the ***same*** router as the phone

## Connecting
1. Open [HeartRateMonitorVRC](https://github.com/DangerKiddy/HeartRateMonitorVRC) app on your PC
2. Open Android app on your phone and press the Scan button\
![1](https://github.com/user-attachments/assets/9b12baa5-0513-4764-9bdd-f3361807f485)
3. Select your pulse oximeter (Mine is CooSpoo H808S, so it's named as `808S ..`)\
![2](https://github.com/user-attachments/assets/3761acf6-a737-4d03-83e4-6f796132024e)
4. Wait until it connects to the pulse oximeter and to your PC\
![3](https://github.com/user-attachments/assets/5bf0ca4b-2ebc-41df-8158-31aa04e8d38e)\
- If it fails to connect to your pulse oximeter (the device is occupied by another device or any other reasons), then restart the app and try again
- If it fails to connect to your PC/find your PC, it will restart the process in 5 seconds. Make sure you have desktop app running and both PC and your phone are connected to the ***same router***.
5. Done, your pulse oximeter now connected to your PC and is sending BPM info\
![4](https://github.com/user-attachments/assets/ccb59e89-1635-4dbc-9c41-03d76867f1dc)\
![Скриншот 27-03-2025 022522](https://github.com/user-attachments/assets/0ef246da-a10b-4353-a627-ac5719486ce9)
