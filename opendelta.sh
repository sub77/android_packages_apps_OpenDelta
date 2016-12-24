#!/bin/bash

# Script to generate delta files for OpenDelta - by Jorrit 'Chainfire' Jongma

# Get device either from $DEVICE set by calling script, or first parameter

if [ "$DEVICE" == "" ]; then
    if [ "$1" != "" ]; then
        DEVICE=$1
    fi
fi

if [ "$DEVICE" == "" ]; then
    echo "Abort: no device set" >&2
    exit 1
fi

# ------ CONFIGURATION ------

HOME=/4and
ROMBASE=OmniROM
OPENDELTA=opendelta
FILEMASK="omni-"
USERNAME="sub77.s"
PASSWORD="sx1r0x"
SERVER="ftp.basketbuild.com"
ROM="OmniRom"

# FTP Directory where file is located
DIR_DELTA="$ROM/.delta/$DEVICE/"
DIR_FULL="/$ROM/$DEVICE/"
#VERBOSE="-v"
TESTMODE=

BIN_JAVA=java
BIN_MINSIGNAPK=$HOME/$ROMBASE/$OPENDELTA/delta/minsignapk.jar
BIN_XDELTA=$HOME/$ROMBASE/$OPENDELTA/delta/xdelta3
BIN_ZIPADJUST=$HOME/$ROMBASE/$OPENDELTA/delta/zipadjust

FILE_MATCH=$FILEMASK*.zip
FILE_MATCH2=$FILEMASK*.md5sum
PATH_CURRENT=$HOME/$ROMBASE/out/target/product/$DEVICE
PATH_LAST=$HOME/$ROMBASE/$OPENDELTA/last/$DEVICE

KEY_X509=$HOME/$ROMBASE/$OPENDELTA/certs/platform.x509.pem
KEY_PK8=$HOME/$ROMBASE/$OPENDELTA/certs/platform.pk8

# ------ PROCESS ------

getFileName() {
    echo ${1##*/}
}

getFileName2() {
    echo ${1##*/}
}

getFileNameNoExt() {
    echo ${1%.*}
}

getFileName2NoExt() {
    echo ${1%.*}
}

getFileMD5() {
    TEMP=$(md5sum -b $1)
    for T in $TEMP; do echo $T; break; done
}

getFileSize() {
    echo $(stat --print "%s" $1)
}

nextPowerOf2() {
    local v=$1;
    ((v -= 1));
    ((v |= $v >> 1));
    ((v |= $v >> 2));
    ((v |= $v >> 4));
    ((v |= $v >> 8));
    ((v |= $v >> 16));
    ((v += 1));
    echo $v;
}

FILE_CURRENT=$(getFileName $(ls -1 $PATH_CURRENT/$FILE_MATCH))
FILE_CURRENT2=$(getFileName2 $(ls -1 $PATH_CURRENT/$FILE_MATCH2))

FILE_LAST=$(getFileName $(ls -1 $PATH_LAST/$FILE_MATCH))
FILE_LAST2=$(getFileName2 $(ls -1 $PATH_LAST/$FILE_MATCH2))

FILE_LAST_BASE=$(getFileNameNoExt $FILE_LAST)
FILE_LAST_BASE2=$(getFileName2NoExt $FILE_LAST2)

if [ -z $TESTMODE ]; then

if [ "$FILE_CURRENT" == "" ]; then
    echo "Abort: CURRENT zip not found" >&2
    exit 1
fi

if [ "$FILE_LAST" == "" ]; then
    echo "Abort: LAST zip not found" >&2
    mkdir -p $PATH_LAST
    cp $PATH_CURRENT/$FILE_CURRENT $PATH_LAST/$FILE_CURRENT
    cp $PATH_CURRENT/$FILE_CURRENT2 $PATH_LAST/$FILE_CURRENT2
    exit 0
fi

if [ "$FILE_LAST" == "$FILE_CURRENT" ]; then
    echo "Abort: CURRENT and LAST zip have the same name" >&2
    exit 1
fi

rm -rf work
mkdir work
rm -rf out
mkdir out

$BIN_ZIPADJUST --decompress $PATH_CURRENT/$FILE_CURRENT work/current.zip
$BIN_ZIPADJUST --decompress $PATH_LAST/$FILE_LAST work/last.zip
$BIN_JAVA -Xmx1024m -jar $BIN_MINSIGNAPK $KEY_X509 $KEY_PK8 work/current.zip work/current_signed.zip
$BIN_JAVA -Xmx1024m -jar $BIN_MINSIGNAPK $KEY_X509 $KEY_PK8 work/last.zip work/last_signed.zip
SRC_BUFF=$(nextPowerOf2 $(getFileSize work/current.zip));
$BIN_XDELTA -B ${SRC_BUFF} -9evfS none -s work/last.zip work/current.zip out/$FILE_LAST_BASE.update
SRC_BUFF=$(nextPowerOf2 $(getFileSize work/current_signed.zip));
$BIN_XDELTA -B ${SRC_BUFF} -9evfS none -s work/current.zip work/current_signed.zip out/$FILE_LAST_BASE.sign

MD5_CURRENT=$(getFileMD5 $PATH_CURRENT/$FILE_CURRENT)
MD5_CURRENT_STORE=$(getFileMD5 work/current.zip)
# Check if value is empty, and if it is empty, use dummy value instead.
# That is to prevent JSON errors and make delta work flawlessly.
if [ -z $MD5_CURRENT_STORE ]; then MD5_CURRENT_STORE=0; fi
MD5_CURRENT_STORE_SIGNED=$(getFileMD5 work/current_signed.zip)
if [ -z $MD5_CURRENT_STORE_SIGNED ]; then MD5_CURRENT_STORE_SIGNED=0; fi
MD5_LAST=$(getFileMD5 $PATH_LAST/$FILE_LAST)
MD5_LAST_STORE=$(getFileMD5 work/last.zip)
MD5_LAST_STORE_SIGNED=$(getFileMD5 work/last_signed.zip)
if [ -z $MD5_LAST_STORE_SIGNED ]; then MD5_LAST_STORE_SIGNED=0; fi
MD5_UPDATE=$(getFileMD5 out/$FILE_LAST_BASE.update)
MD5_SIGN=$(getFileMD5 out/$FILE_LAST_BASE.sign)
if [ -z $MD5_SIGN ]; then MD5_SIGN=0; fi;

SIZE_CURRENT=$(getFileSize $PATH_CURRENT/$FILE_CURRENT)
SIZE_CURRENT_STORE=$(getFileSize work/current.zip)
SIZE_CURRENT_STORE_SIGNED=$(getFileSize work/current_signed.zip)
if [ -z $SIZE_CURRENT_STORE_SIGNED ]; then SIZE_CURRENT_STORE_SIGNED=0; fi
SIZE_LAST=$(getFileSize $PATH_LAST/$FILE_LAST)
SIZE_LAST_STORE=$(getFileSize work/last.zip)
SIZE_LAST_STORE_SIGNED=$(getFileSize work/last_signed.zip)
if [ -z $SIZE_LAST_STORE_SIGNED ]; then SIZE_LAST_STORE_SIGNED=0; fi
SIZE_UPDATE=$(getFileSize out/$FILE_LAST_BASE.update)
SIZE_SIGN=$(getFileSize out/$FILE_LAST_BASE.sign)

DELTA=out/$FILE_LAST_BASE.delta

echo "{" > $DELTA
echo "  \"version\": 1," >> $DELTA
echo "  \"in\": {" >> $DELTA
echo "      \"name\": \"$FILE_LAST\"," >> $DELTA
echo "      \"size_store\": $SIZE_LAST_STORE," >> $DELTA
echo "      \"size_store_signed\": $SIZE_LAST_STORE_SIGNED," >> $DELTA
echo "      \"size_official\": $SIZE_LAST," >> $DELTA
echo "      \"md5_store\": \"$MD5_LAST_STORE\"," >> $DELTA
echo "      \"md5_store_signed\": \"$MD5_LAST_STORE_SIGNED\"," >> $DELTA
echo "      \"md5_official\": \"$MD5_LAST\"" >> $DELTA
echo "  }," >> $DELTA
echo "  \"update\": {" >> $DELTA
echo "      \"name\": \"$FILE_LAST_BASE.update\"," >> $DELTA
echo "      \"size\": $SIZE_UPDATE," >> $DELTA
echo "      \"size_applied\": $SIZE_CURRENT_STORE," >> $DELTA
echo "      \"md5\": \"$MD5_UPDATE\"," >> $DELTA
echo "      \"md5_applied\": \"$MD5_CURRENT_STORE\"" >> $DELTA
echo "  }," >> $DELTA
echo "  \"signature\": {" >> $DELTA
echo "      \"name\": \"$FILE_LAST_BASE.sign\"," >> $DELTA
echo "      \"size\": $SIZE_SIGN," >> $DELTA
echo "      \"size_applied\": $SIZE_CURRENT_STORE_SIGNED," >> $DELTA
echo "      \"md5\": \"$MD5_SIGN\"," >> $DELTA
echo "      \"md5_applied\": \"$MD5_CURRENT_STORE_SIGNED\"" >> $DELTA
echo "  }," >> $DELTA
echo "  \"out\": {" >> $DELTA
echo "      \"name\": \"$FILE_CURRENT\"," >> $DELTA
echo "      \"size_store\": $SIZE_CURRENT_STORE," >> $DELTA
echo "      \"size_store_signed\": $SIZE_CURRENT_STORE_SIGNED," >> $DELTA
echo "      \"size_official\": $SIZE_CURRENT," >> $DELTA
echo "      \"md5_store\": \"$MD5_CURRENT_STORE\"," >> $DELTA
echo "      \"md5_store_signed\": \"$MD5_CURRENT_STORE_SIGNED\"," >> $DELTA
echo "      \"md5_official\": \"$MD5_CURRENT\"" >> $DELTA
echo "  }" >> $DELTA
echo "}" >> $DELTA

mkdir publish >/dev/null 2>/dev/null
mkdir publish/$DEVICE >/dev/null 2>/dev/null
cp out/* publish/$DEVICE/.

rm -rf $PATH_LAST/*
mkdir -p $PATH_LAST

cp $PATH_CURRENT/$FILE_CURRENT $PATH_LAST/$FILE_CURRENT
cp $PATH_CURRENT/$FILE_CURRENT2 $PATH_LAST/$FILE_CURRENT2

fi

FILE_DELTA1="$HOME/$ROMBASE/$OPENDELTA/publish/$DEVICE/*.delta"
FILE_DELTA2="$HOME/$ROMBASE/$OPENDELTA/publish/$DEVICE/*.sign"
FILE_DELTA3="$HOME/$ROMBASE/$OPENDELTA/publish/$DEVICE/*.update"


FILE_FULL_MD5="$PATH_LAST/$FILE_MATCH2"
FILE_FULL_ZIP="$PATH_LAST/$FILE_MATCH"

curl $VERBOSE -T $FILE_DELTA1 -u $USERNAME:$PASSWORD $SERVER/$DIR_DELTA
curl $VERBOSE -T $FILE_DELTA2 -u $USERNAME:$PASSWORD $SERVER/$DIR_DELTA
curl $VERBOSE -T $FILE_DELTA3 -u $USERNAME:$PASSWORD $SERVER/$DIR_DELTA
curl $VERBOSE -T $FILE_FULL_MD5 -u $USERNAME:$PASSWORD $SERVER/$DIR_FULL
curl $VERBOSE -T $FILE_FULL_ZIP -u $USERNAME:$PASSWORD $SERVER/$DIR_FULL

rm $FILE_DELTA1
rm $FILE_DELTA2
rm $FILE_DELTA3

rm -rf work
rm -rf out

exit 0
