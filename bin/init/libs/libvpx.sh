#!/bin/bash

source "bin/init/env.sh"

yasm --version || exit 1

cd TMessagesProj/jni || exit 1
git submodule update --init libvpx

cd libvpx
git reset --hard
git clean -fdx
cd ..

./build_libvpx_clang.sh || exit 1
