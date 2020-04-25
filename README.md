# Spark-Bench

## Debug Settings
### Program arguments (idea)
    -f mnist -b 1024 --checkpoint model_sgd
    -f mnist -b 1024 --checkpoint model_lars_sgd


## LeNet5 Model on MNIST (http://yann.lecun.com/exdb/lenet/)

Download Mnist dataset: http://yann.lecun.com/exdb/mnist/

Extract the data to mnist folder as:  
train-images-idx3-ubyte  
train-labels-idx1-ubyte  
t10k-images-idx3-ubyte  
t10k-labels-idx1-ubyte  
## Spark, Maven, BigDL Installation Procudures
1. Install Java8 and Spark

        git clone https://github.com/WZStephen/Spark-2.3.4.git
        mv Spark-2.3.4 spark
        sudo apt-get install openjdk-8-jdk
        //If you have more than one Java version installed on your system use following command to switch versions
        sudo update-alternatives --config java 
    Spark version - 2.3.4  
    Scala version - 2.11.8  
    Java8
2. Add Spark to ~/bashrc

        vim ~/bashrc
        SPARK_HOME=~/spark
        export PATH=$SPARK_HOME/bin:$PATH
        export PYSPARK_PYTHON=/usr/bin/python3
        source ~/.bashrc
3. Install SBT

        echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
        curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
        sudo apt-get update
        sudo apt-get install sbt


        
## Steps to run from this repository:
* The command below is the way to run Lenet5 on Mnist dataset (15 epochs for each 60000 training data and 10000 for testing data)
* Refer https://github.com/intel-analytics/BigDL/tree/master/spark/dl/src/main/scala/com/intel/analytics/bigdl/models/lenet for more information
1. Build by SBT

        sbt clean assembly
2. Command for running in local Spark cluster mode (Training with SGD)  
        
        spark-submit --master local[4] \
        --driver-memory 10g \
        --class com.intel.analytics.bigdl.Train_SGD \
        target/scala-2.11/Spark-Bench-assembly-0.1.0.jar \
        -f mnist \
        -b 64 \
        --checkpoint ./model_sgd
3. Command for running in local Spark cluster mode (Training with LARS-SGD)  

        spark-submit --master local[4] \
        --driver-memory 10g \
        --class com.intel.analytics.bigdl.Train_LARS_SGD \
        Spark-Bench-assembly-0.1.0.jar \
        -f mnist \
        -b 64 \
        --checkpoint ./model_lars_sgd
* -f: where you put your Cifar10 data
* --checkpoint: Where you cache the model/train_state snapshot. You should input a folder and make sure the folder is created * when you run this example. The model snapshot will be named as model.#iteration_number, and train state will be named as state.#iteration_number. Note that if there are some files already exist in the folder, the old file will not be overwrite for the safety of your model files.
* -b: The mini-batch size. It is expected that the mini-batch size is a multiple of node_number * core_number.
* --summary: Where you store the training metainfo, which can be visualized in tensorboard
* --checkpoint: Where the model is stored
* --deriver-memoery: should be adjusted depends on your system's memoery
1. Command for running in local Spark cluster mode (Testing)
        
        spark-submit \
        --master local[4] \
        --driver-memory 10g \
        --class com.intel.analytics.bigdl.Test \
        target/scala-2.11/Spark-Bench-assembly-0.1.0.jar \
        -f mnist \
        --model ./model/model.14071 \
        -b 64
* -f: where you put your MNIST data
* --model: the model snapshot file (./model/model.iteration)
* -b: The mini-batch size. It is expected that the mini-batch size is a multiple of node_number * core_number.
    
