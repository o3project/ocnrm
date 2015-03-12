README
==========================

 Software Installation
--------------------------
[Install jdk1.7, maven, and set proxy for maven.]

 Build OCNRM
--------------------------

    $ bash ./buld.sh
    $ cd ./target
    $ cp -p ./ocnrm-1.0.0-bin.tar.gz PATH/TO/INSTALL/DIR
    $ cd PATH/TO/INSTALL/DIR
    $ tar xvfz ./ocnrm-1.0.0-bin.tar.gz
    $ cd ocnrm-1.0.0-bin
    $ vi ./PseudoMf.properties

check DISPATCHER_HOST, DISPATCHER_PORT

     > DISPATCHER_HOST=127.0.0.1
     > DISPATCHER_PORT=6379

ex.  
if Redis Server( for ODENOS ) Address:Port is 192.168.1.100:6379 then

     DISPATCHER_HOST=192.168.1.100
     DISPATCHER_PORT=6379

check REQUEST_ODU_FLOW_URL, REQUEST_OCH_REPLACEMENT_PIECE_URL, 
REQUEST_OCH_REPLACEMENT_PIECE_URL,REQUEST_OCH_REPLACEMENT_PIECE_URL

     > REQUEST_ODU_FLOW_URL=http://127.0.0.1/DEMO/Generate/L1Path
     > REQUEST_OCH_REPLACEMENT_PIECE_URL=http://127.0.0.1/DEMO/ID/L0Request
     > REQUEST_ODU_REPLACEMENT_PIECE_URL=http://127.0.0.1/DEMO/ID/L1Request
     > DELETE_ODU_FLOW_URL=http://127.0.0.1/DEMO/Delete/L1Path

ex.  
if PsudeMf Server Address:Port is 192.168.1.120:8081 then

     REQUEST_ODU_FLOW_URL=http://192.168.1.120:8081/DEMO/Generate/L1Path
     REQUEST_OCH_REPLACEMENT_PIECE_URL=http://192.168.1.120:8081/DEMO/ID/L0Request
     REQUEST_ODU_REPLACEMENT_PIECE_URL=http://192.168.1.120:8081/DEMO/ID/L1Request
     DELETE_ODU_FLOW_URL=http://192.168.1.120:8081/DEMO/Delete/L1Path

check OFCTL_SEND_URL

     > OFCTL_SEND_URL=http://127.0.0.1:8080/stats/flowentry

ex.  
if RYU OTN Extension ( of OpenFlow Controller ) Server Address:Port is 192.168.1.120:8080 then

     OFCTL_SEND_URL=http://192.168.1.120:8080/stats/flowentry
 

 Starting OCNRM
--------------------------

    $ ./ocnrm_mn -s


 Stopping OCNRM
--------------------------

    $ ./ocnrm_mn -q


