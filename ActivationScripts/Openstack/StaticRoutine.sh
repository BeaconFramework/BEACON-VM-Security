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

DIRPATH=$1                          #DIRECTORY PATH OF SCANNER ELEMENT
STR1="java -jar "$1"/"$2            #$2 represent FCOExecutableOpenStack.jar
STR2=$3                             #Destination Email 
TMPFILE=$DIRPATH"/TMPtest.txt"
CLEANSCRIPT=$DIRPATH"/cleaningScript.sh"


REPONAME=$DIRPATH"/repoVMsChecked/UUID_REPO_Flexiant"
SCRIPTNAME=$DIRPATH"/final_script_Flexiant.sh"
##
## System parameters loads
##
SYS_PARAM=$DIRPATH"/rcfiles/rcflextest"
. $SYS_PARAM

echo '#!/bin/bash' > $SCRIPTNAME
###
### VMs LIST RETRIEVING
###
nova list --fields ID,Networks |awk '(NR>3) ' > $TMPFILE
VARIABLE_ID=`wc -l $TMPFILE| awk '{printf $1}'`
i="1"
while [ "$i" -lt "$VARIABLE_ID" ]
do
	echo "i vale:"$i
	UUID_UNCHECK=`cat $TMPFILE | awk -v exind="$i" '(NR == exind){printf $2}'`
####
#### Verify if VM is already analized
####
        PRESENT=`cat $REPONAME | awk -v uuid="$UUID_UNCHECK" '{if(uuid == $1) printf "true"; else printf "false";}'`
        echo $PRESENT
        if [[ $PRESENT != *"true"* ]]
	then
	        cat $TMPFILE | awk -v str1="$STR1" -v str2="$STR2" -v exind="$i" '(NR == exind){ print str1, $2, $6, str2 }' >> $SCRIPTNAME
		echo "$UUID_UNCHECK" >>$REPONAME
	fi
	i=$[$i+1]
done

chmod +x $SCRIPTNAME
bash $CLEANSCRIPT $DIRPATH "final_script_Flexiant.sh"
##
## FILE creation Ending
##

bash $SCRIPTNAME
rm $TMPFILE
