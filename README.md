# what's OCNRM
"Opt-transport apps of O3 orchestrator&amp;controller suite"
---
OCNRM(Optical Core Network Resource Manager) is a software which achieves Software Defined Networking in OTN/WDM network.
OCNRM works with ODENOS, PseudoMF(PseudoManagementFunctions) and RYU-OE(RYU OTN Extension), and control OTN/WDM network nodes which support OpenFlow OTN extension.

 Environment
--------------------------
OS：Ubuntu 12.04.3(x64)  
Memory: 1GB (without ODENOS)  
Middleware: Java 7 and later, Maven2  
ODENOS 1.0.0 is required  


 Build
--------------------------

    $ cd ~
    $ git clone https://github.com/o3project/ocnrm.git
    $ bash ./buld.sh
    $ tar xvfz ./target/ocnrm-1.0.0-bin.tar.gz –C ~/

You can choose your install directory instead of ~/.

 Configuration
--------------------------

Edit OCNRM.properties

    $ vi ~/ocnrm-1.0.0-bin/OCNRM.properties

Set redis-server’s host & port. For example, 

     DISPATCHER_HOST=127.0.0.1
     DISPATCHER_PORT=6379

Set pseudoMF’s REST access points. For example,

     REQUEST_ODU_FLOW_URL=http://127.0.0.1:80/DEMO/Generate/L1Path
     REQUEST_OCH_REPLACEMENT_PIECE_URL=http://127.0.0.1:80/DEMO/ID/L0Request
     REQUEST_ODU_REPLACEMENT_PIECE_URL=http://127.0.0.1:80/DEMO/ID/L1Request
     DELETE_ODU_FLOW_URL=http://127.0.0.1:80/DEMO/Delete/L1Path
 
set ryu-oe’s REST access point. For example,

     OFCTL_SEND_URL=http://127.0.0.1:8080/stats/flowentry


 Starting OCNRM
--------------------------

    $ ~/ocnrm-1.0.0-bin/ocnrm_mn -s


 Stopping OCNRM
--------------------------

    ~/ocnrm-1.0.0-bin/ocnrm_mn -q


