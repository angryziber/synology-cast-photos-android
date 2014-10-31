# HelloText-Android

This Google Cast demo application shows how to send messages from an Android device to a receiver using a custom namespace. 

## Dependencies
* The Android SDK android-support-v7-appcompat and android-support-v7-mediarouter support libraries (at least revision 19).
* The Android SDK google-play-services_lib library (at least version 4.2)

## Setup Instructions
* Just check out the code from GitHub and run the app on your mobile device
* If you don't want to use the sample App ID, you need to do the following steps
* Get a Chromecast device and get it set up for development: https://developers.google.com/cast/docs/developers#Get_started
* Register an application on the Developers Console (http://cast.google.com/publish). Select the Custom Receiver option and specify the URL to where you are hosting the receiver/receiver.html file (You can use Google Drive to host your files: https://support.google.com/drive/answer/2881970?hl=en). You will get an App ID when you finish registering your application.
* Setup the project dependencies
* Insert your App ID in the strings.xml in the res directory of the project (look for app_id in that file)
* Compile and deploy to your Android device.

## References and How to report bugs
* Cast APIs: http://developers.google.com/cast/
* Design Checklist (http://developers.google.com/cast/docs/design_checklist)
* If you find any issues, please open a bug here on GitHub

## How to make contributions?
Please read and follow the steps in the CONTRIBUTING.md

## License
See LICENSE

## Google+
Google Cast Developers Community on Google+ [http://goo.gl/TPLDxj](http://goo.gl/TPLDxj)
