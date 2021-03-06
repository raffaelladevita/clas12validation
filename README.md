# clas12validation

Scripts to launch tracking validation for specific branches/releases

The main script, run-trk-tests.sh, is meant to:
1- build a specific coatjava branch from https://github.com/JeffersonLab/clas12-offline-software or get a precompiled release
2- install and run clara with the coatjava version at 1
3- run reconstruction on a mc or data file from a predefined list
4- make a standard set of plots

Usage:
run-trk-tests.sh [--build] [-bhost hostname] [-bdir directory] [--wget] [-branch branch/release name] [--clean] [-file input-file-name] [-run local/farm/no] [--notest]
--build:        build branch selected with -branch option
 -bhost:        build host (jlabl5 for CUE building)
 -bdir:         build directory (/scratch/... for jlabl5)
--build:        build branch selected with -branch option
 -branch name:  select branch to be built or release to be downloaded
--clean:        remove clara installation and configuration files
 -file name:    specify input file name (default sidis)
--notest:       do not run test
 -run mode:     choose reconstruction mode (local or farm or no run)
--wget:         get precompiled release as specified with -branch option


On jlab machines:
- login on ifarm for testing or clara1603 for running reconstruction.
- copy the validation folder in an appropriate location
- edit the main script to set the path for the input and output folder appropriately
- run...
- note that on clara1603 it will not be possible to run the analysis; use clara1603 to run reconstruction with the --notest option and then run the analysis separately

 
Examples:

./run-trk-tests.sh --build -bhost jlabl5 -bdir /scratch/devita -branch testmagfield -run no --notest
will build the testmagfield branch without running reconstruction or analysis

./run-trk-tests.sh -branch testmagfield -file sidis_tm1_sm1
will build the testmagfield branch running reconstruction and analysis on file sidis_tm1_sm1.hipo

./run-trk-tests.sh --wget -branch 5a.3.3 -file sidis_tm1_sm1
will get release 5a.3.3 running reconstruction and analysis on file sidis_tm1_sm1.hipo





