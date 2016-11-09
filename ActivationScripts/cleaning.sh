#!/bin/bash
#Copyright 2016, University of Messina.
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#
#   @author Giuseppe Tricomi <giu.tricomi@gmail.com>


DIRPATH=$1
SCRIPTNAME=$DIRPATH"/"$2
VARIABLE_ID=`wc -l $SCRIPTNAME| awk '{printf $1}'`
i="2"
echo "cleaning starts"
echo "#!/bin/bash" >$DIRPATH/tmpfile.sh
rm $DIRPATH/tmpfile2.txt
touch $DIRPATH/tmpfile2.txt
neutron floatingip-list -F fixed_ip_address -F floating_ip_address |awk '(NR>3) ' > $DIRPATH/neutronanswer.txt
while [ "$i" -le "$VARIABLE_ID" ]
do
    echo "var"$VARIABLE_ID" i"$i
    UUID=`cat $SCRIPTNAME | awk -v exind="$i" '(NR == exind){ print $4 }'`
    TESTP=`cat $SCRIPTNAME | awk -v exind="$i" '(NR == exind){if($5 == "|") printf "false"; else printf "$4";}'`
    if [[ $TESTP == "false" ]]
    then
    	cat TMPtest.txt | awk -v uuid="$UUID" '{if(uuid == $2) printf $0"\n"; else printf "";}' > $DIRPATH/tmpfile2.txt
    	VARIABLE_ID_A=`wc -l $DIRPATH/tmpfile2.txt| awk '{printf $1}'`
    	k="1"
    	while [ "$k" -le "$VARIABLE_ID_A" ]
    	do
    	echo "VARIABLE_ID_A:" $VARIABLE_ID_A"k"$k 
    		echo $uuid
            	A=`cat $DIRPATH/tmpfile2.txt | awk -v exind="$k" '(NR == exind){printf $5}'`
            	PRIVATEIP=${A:12:11}
            	echo $PRIVATEIP
            	VARIABLE_ID2=`wc -l $DIRPATH/neutronanswer.txt| awk '{printf $1}'`
            	j="1"
            	while [ "$j" -le "$VARIABLE_ID2" ]
            	do
                   	FLOATIP=`cat $DIRPATH/neutronanswer.txt | awk -v uuid="$PRIVATEIP" -v exind="$j" '(NR == exind){if(uuid == $2) print $4 ; else printf "mod";}'`
            	    echo "VARIABLE_ID2" $VARIABLE_ID2"j"$j
                   	if [[ $FLOATIP != "mod" ]]
                   	then
                        echo "FLOAT " $FLOATIP
    			        sed -i "s/$uuid | /$uuid $FLOATIP /" $SCRIPTNAME
                   	fi
                    	j=$[$j+1]
           		done
    		k=$[$k+1]
    	done
    fi
    echo "SED controls are sto per eseguire il controllo con il sed"
    TESTP=`cat $SCRIPTNAME | awk -v exind="$i" -v scrname=$SCRIPTNAME '(NR == exind){if($5 == "|") printf "sed -i "exind"d "scrname; else printf "echo ok";}'`
    echo $TESTP >>$DIRPATH/tmpfile.sh
    i=$[$i+1]
done
chmod +x $DIRPATH/tmpfile.sh
./$DIRPATH/tmpfile.sh
rm $DIRPATH/tmpfile.sh
