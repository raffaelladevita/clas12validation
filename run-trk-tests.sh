#!/bin/sh -f

inFileDir=./infiles
outFileDir=./outfiles
coatDir=./coat

# usage options
usage='run-trk-tests.sh [--build] [-bhost hostname] [-bdir directory] [--wget] [-branch branch/release name] [--clean] [-file input-file-name] [-yaml name.yaml] [-run local/farm/no] [-scratch directory] [--notest]'
if [ $# -eq "0" ]
then
    echo $usage
    echo "--build:        build branch selected with -branch option"
    echo " -bhost:        build host (jlabl5 for CUE building)"
    echo " -bdir:         build directory (/scratch/... for jlabl5)"
    echo "--build:        build branch selected with -branch option"
    echo " -branch:       select branch to be built or release to be downloaded"
    echo "--clean:        remove clara installation and configuration files"
    echo " -file:         specify input file name (default sidis)"
    echo " -yaml:         specify yaml file name (default services.yaml)"
    echo " -scratch:      specify local scratch directory to store input/ouput files during processing"
    echo "--notest:       do not run test"
    echo " -run:          choose reconstruction mode (local or farm or no run)"
    echo "--wget:         get precompiled release as specified with -branch option"
    exit
fi

# check and create relevant directories
if ! [ -d $coatDir ] 
then
    echo  "Creating coatjava tar file directory " $coatDir
    mkdir $coatDir
fi    
if ! [ -d $outFileDir ] 
then
    echo "Creating output file directory " $outFileDir
    mkdir $outFileDir
fi    
if ! [ -d $inFileDir ] 
then
    echo "Input file directory " $inFileDir " does not exist: exiting..."
    exit
fi    


# DEFAULT options
# whether to use CLARA (0=no)
useClara=1
# don't download but use pre-exsting code-coatjava folder as determined by branch name
wget=0
# don't build but use pre-exsting code-coatjava folder as determined by branch name
build=0
# build host
buildhost=""
# build directory
buildir="."
# branch/cotajava-folder name
branch="development"
# if running reconstruction choose between running locally or on the farm
run="local"
# input filename
inFileStub="sidis"
# yaml filename
yamlFile="services.yaml"
# default scratch
scratchDir="null"
# run analysis only
runTest=1
# rm files and folders created at run time
clean=0

ii=1
for xx in $@
do
    ii=`expr "$ii" + 1`
    opt="${@:$ii:1}"
    if [ "$xx" == "--build" ]
    then
        build=1
    elif [ "$xx" == "-bhost" ]
    then
        buildhost=$opt
    elif [ "$xx" == "-bdir" ]
    then
        buildir=$opt
    elif [ "$xx" == "-branch" ]
    then
        branch=$opt
    elif [ "$xx" == "--clean" ]
    then
        clean=1
    elif [ "$xx" == "-file" ]
    then
        inFileStub=$opt
    elif [ "$xx" == "-yaml" ]
    then
        yamlFile=$opt
    elif [ "$xx" == "-scratch" ]
    then
        scratchDir=$opt
    elif [ "$xx" == "-run" ]
    then
        run=$opt
    elif [ "$xx" == "--notest" ]
    then
        runTest=0
    elif [ "$xx" == "--wget" ]
    then
        wget=1    
    fi
done



# sanity check on filestub name,
# just to error with reasonable message before proceeding:
case $inFileStub in
    # sidis:
    sidis)
        ;;
    sidis_extendedtarget)
        ;;
    clas_002391.0)
        ;;
    clas_002391.0.9)
        ;;
    clas_002587.0)
        ;;
    clas_002587.0.9)
        ;;
    clas_003050.0.9)
        ;;
    clas_003718.0.9)
        ;;
    clas_003842.0)
        ;;
    clas_003842.0.9)   
        ;;
    clas_003932.0.9)
        ;;
    clas_004013.0)
        ;;
    clas_004013.0.9)
        ;;
    clas_005038.1231)
        ;;
    clas_004150.0)
        ;;
    clas_006223.1)
        ;;
    *)
      echo Invalid input evio file:  $inFileStub
      exit 1
esac

#clean folder
if [ $clean -eq 1 ] 
then
    rm -rf clara-$branch files.list cook.clara
fi

# set up environment
COAT=$coatDir/coatjava-$branch
export COAT
if ! [ $useClara -eq 0 ]
then
    CLARA_HOME=$PWD/clara-$branch
    CLARA_USER_DATA=$PWD/clara-$branch
    export CLARA_HOME
    export CLARA_USER_DATA
fi

# if build flag is set clone git repo for the chosen branch and compile
if [ $build -eq 1 ]
then
    echo "Downloading and building branch" $branch
    buildcommand="rm -rf clas12-offline-software ; \
                  git clone -b "$branch" https://github.com/JeffersonLab/clas12-offline-software ; \
                  cd clas12-offline-software ; \
                  ./build-coatjava.sh --unittests; \
                  tar -zcvf coatjava.tar.gz coatjava ; \
                  cd .."
    echo "cd $buildir" >  build.sh              
    echo $buildcommand >> build.sh
    rm -rf $coatDir/coatjava-$branch $coatDir/coatjava-$branch.tar.gz;                    
    if ! [ "$buildhost" == "" ]
    then
#        scp -rp build.sh $buildhost:.
        mv build.sh ~/.
        echo "scp -rpC clas12-offline-software/coatjava.tar.gz ftp:$PWD" >> ~/build.sh
        ssh -t $buildhost 'source build.sh'
#        scp -rpC $buildhost:$buildir/clas12-offline-software/coatjava.tar.gz .
        rm ~/build.sh
    else
        source build.sh
        mv clas12-offline-software/coatjava.tar.gz .
        rm build.sh
    fi
    mv coatjava.tar.gz $coatDir/coatjava-$branch.tar.gz
    cd $coatDir
    rm -rf coatjava
    tar xvfz coatjava-$branch.tar.gz
    mv coatjava coatjava-$branch
    cd -
# download and setup dependencies
elif ! [ $wget -eq 0 ]
then
    echo "Downloading tag" $branch
    cd $coatDir
    rm -fr coatjava-$branch.tar.gz coatjava-$branch
    wget --no-check-certificate http://clasweb.jlab.org/clas12offline/distribution/coatjava/coatjava-$branch.tar.gz
    tar xvfz coatjava-$branch.tar.gz
    mv coatjava coatjava-$branch
    cd -
fi

#run reconstruction:
if ! [ "$run" == "no" ]
then

    # check if input file exist
    if ! [ -e $inFileDir/${inFileStub}.hipo ] ; then echo "input file does not exist" ; exit 1 ; fi

    if ! [ -d $outFileDir/$branch ] ; then mkdir $outFileDir/$branch; fi
    rm -f $outFileDir/$branch/out_${inFileStub}.hipo

    # run reconstruction:
    if [ $useClara -eq 0 ]
    then
        $COAT/bin/notsouseful-util -i $inFileDir/${inFileStub}.hipo -o $outFileDir/$branch/out_${inFileStub}.hipo -c 2
    else
        if [ -d $PWD/clara-$branch ] 
        then 
            echo "Using existing clara installation"
        else
            # install clara
            echo "Installing clara..."
            if [ -e install-claracre-clas.sh ] ; then rm install-claracre-clas.sh ; fi
            wget --no-check-certificate https://claraweb.jlab.org/clara/_downloads/install-claracre-clas.sh
            chmod +x install-claracre-clas.sh
            cp -p $coatDir/coatjava-$branch.tar.gz .
            ./install-claracre-clas.sh -l $branch -f 4.3.10 -g 2.1 
            if [ $? != 0 ] ; then echo "clara installation error" ; exit 1 ; fi
            rm install-claracre-clas.sh
        fi
        # create config file and launch
        rm -f $CLARA_HOME/log/*
        if ! [ "$scratchDir" == "null" ]
        then
           echo "Copying input file to local disk in" $scratchDir
           cp -p $inFileDir/${inFileStub}.hipo $scratchDir
           echo "set inputDir $scratchDir"          >  cook.clara
           echo "set outputDir $scratchDir"         >> cook.clara
        else
           echo "set inputDir $inFileDir"           >  cook.clara
           echo "set outputDir $outFileDir/$branch" >> cook.clara
        fi
        echo "set threads 64"                    >> cook.clara
        echo "set farm.exclusive farm16"         >> cook.clara
        echo ${inFileStub}.hipo                  >  files.list
        echo "set fileList $PWD/files.list"      >> cook.clara
        echo "set servicesFile "${CLARA_HOME}"/plugins/clas12/config/"${yamlFile}      >> cook.clara
#        echo "set maxEvents 20000" >> cook.clara
#        echo "set outputFilePrefix dst"          >> cook.clara
        echo "run $run"                          >> cook.clara
        echo "exit"                              >> cook.clara
        $CLARA_HOME/bin/clara-shell cook.clara
        if ! [ "$scratchDir" == "null" ]
        then
           echo "Moving output files to " $outFileDir
           rm $scratchDir/${inFileStub}.hipo
           mv $scratchDir/out_${inFileStub}.hipo $outFileDir/$branch/.
        fi
        if [ -e $outFileDir/$branch/log_${inFileStub} ] ; then rm -r $outFileDir/$branch/log_${inFileStub}; fi
        cp -r $CLARA_HOME/log $outFileDir/$branch/log_${inFileStub}
    fi
fi

# run Event Builder tests:
classPath="$COAT/lib/services/*:$COAT/lib/clas/*:$COAT/lib/utils/*:src/"

# make sure test code compiles before anything else:
if [ $runTest -eq 1 ]
then
    echo "Running test..."
    javac -Xlint:unchecked -cp $classPath src/trk/TrackingTest.java
    if [ $? != 0 ] ; then echo "EBTwoTrackTest compilation failure" ; exit 1 ; fi
    java -DCLAS12DIR="$COAT" -Xmx1536m -Xms1024m -cp $classPath -DINPUTFILE=${inFileStub} -DRESULTS=$outFileDir/$branch trk.TrackingTest 
fi
