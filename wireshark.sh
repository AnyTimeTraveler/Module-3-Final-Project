#!/bin/bash

wireshark -X lua_script:debug.lua -Y "hrp4" -i any -k