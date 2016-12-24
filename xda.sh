#/bin/bash

if [ -f ~/.xda ]
then source ~/.xda
else
read -p "XDA Username:" xdaname
read -p "XDA Password:" xdapass
echo -e "xdaname=$xdaname\nxdapass=$xdapass" > ~/.xda
fi

cd xda
nano op.txt
python update.py -u $xdaname -p $xdapass -v 7
cd ..
