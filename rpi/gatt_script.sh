#!/bin/bash
date
gatttool -b D6:67:5C:C8:C4:71 --char-read -a 0x000c
sleep 1
gatttool -b D6:67:5C:C8:C4:71 --char-read -a 0x000f
sleep 1
gatttool -b D6:67:5C:C8:C4:71 --char-read -a 0x0012
sleep 1
gatttool -b D6:67:5C:C8:C4:71 --char-read -a 0x0015
sleep 1
gatttool -b D6:67:5C:C8:C4:71 --char-read -a 0x0018

