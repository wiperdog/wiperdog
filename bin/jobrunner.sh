#!/bin/sh
#############################################################################
#   Copyright 2013 Insight technology,inc. All rights reserved.
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
#############################################################################

self="$0"
dir=`dirname "$self"`
# 2013-03-06 Luvina insert start
path=`pwd $0`
if [ "$dir" == "." ]  
then
  export dirname=`pwd $0`
else
  export dirname=$path"/"$dir	
fi
# 2013-03-06 Luvina insert end
if [ "$#" -eq 2 ]; then 
"$dir/groovy" "$dir/jobrunner.groovy" "$1" "$2"
elif [ "$#" -eq 4 ]; then
  "$dir/groovy" "$dir/jobrunner.groovy" "$1" "$2" "$3" "$4" 
else
 echo           Incorrect parameters!
 echo			Example:
 echo			jobrunner -f var/job/testjob.job  :  Run job now and one time only
 echo			jobrunner -f var/job/testjob.job -s "<crontab>"  :  Run scheduled job with crontab format
fi
