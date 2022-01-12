## Manjaro XFCE First setup

- software: update, reboot
- Disable beep
```bash
sudo su
echo "blacklist pcspkr" | tee /etc/modprobe.d/nobeep.conf
rmmod pcspkr
(https://wiki.archlinux.org/index.php/PC_speaker)
```
- turn on snap in Software, and `ln -s /var/lib/snapd/snap /snap`
- chromium (setup no password, On startup: 
Continue where you left off, default browser)
- manjaro settings, kernel
- On Lenovo-Flex (or amd cpu graphics)
```bash
#https://www.linux.org/threads/failed-to-start-load-save-screen-backlight-brightness-of-amdgpu_bl1.31998/
sudo nano /etc/mkinitcpio.conf
#modify: MODULES=(amdgpu)
sudo mkinitcpio -P
```
- idea community
- sudo archlinux-java set java-11-openjdk  
- Mouse and Touchpad -> Select Device Touchpad, checkbox Reverse scroll direction
- Mouse and Touchpad-> Select Device Touchpad, and tab Touchpad. checkbox "Tab touchpad to click" true
- Appearance  
![img.png](appearance.png)
- Window Manager  
![img.png](window_manager.png)  
![img.png](windows_manager_2.png)  
- Keyboard  
xfce4-screenshooter  [Print]
xfce4-popup-whiskermenu [Win + S]  
![img.png](keyboard1.png)  
![img.png](keyboard2.png)  
- Panel  
![img.png](panel.png)
- In Window Buttons submenu  
![img.png](window_buttons.png)
- Window Manager Tweaks
![window_manager_tweaks.png](window_manager_tweaks.png)
- Power Manager   
- Docker  
  https://manjaro.site/how-to-install-docker-on-manjaro-18-0/  
- download and run android_studio  
https://developer.android.com/studio/archive
```bash
archlinux-java status
sudo archlinux-java set java-8-openjdk
export JAVA_HOME="/usr/lib/jvm/java-8-openjdk"
yes | ~/Android/Sdk/tools/bin/sdkmanager --licenses
sudo archlinux-java set liberica-jdk-11-full 
#sudo archlinux-java set java-11-openjdk
```
- [Optional] increase swapfile (https://www.linuxsecrets.com/manjaro-wiki/index.php%3Ftitle=Add_a_%252Fswapfile.html)
```bash
sudo swapoff /swapfile
sudo rm -rf /swapfile
sudo fallocate -l 35G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
# not need if change exists
#Add the following line to your /etc/fstab
#/swapfile none swap defaults 0 0
```
- add to bashrc  
```bash
source "/home/dim/Desktop/github/avdim/save/linux_install/_bashrc.sh"
```
