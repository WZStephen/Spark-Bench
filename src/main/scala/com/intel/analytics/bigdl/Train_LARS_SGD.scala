/*
 * Copyright 2016 The BigDL Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.analytics.bigdl

import com.intel.analytics.bigdl._
import com.intel.analytics.bigdl.dataset.DataSet
import com.intel.analytics.bigdl.dataset.image.{BytesToGreyImg, GreyImgNormalizer, GreyImgToBatch}
import com.intel.analytics.bigdl.nn.{ClassNLLCriterion, CrossEntropyCriterion, Module}
import com.intel.analytics.bigdl.numeric.NumericFloat
import com.intel.analytics.bigdl.optim._
import com.intel.analytics.bigdl.utils._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext

object Train_LARS_SGD {
  LoggerFilter.redirectSparkInfoLogs()


  import Utils._

  def main(args: Array[String]): Unit = {
    trainParser.parse(args, new TrainParams()).map(param => {
      val conf = Engine.createSparkConf()
        .setAppName("Train Lenet on MNIST")
        .set("spark.task.maxFailures", "1")
        .setMaster("local[4]")
      val sc = new SparkContext(conf)
      Engine.init

      val trainData = param.folder + "/train-images-idx3-ubyte"
      val trainLabel = param.folder + "/train-labels-idx1-ubyte"
      val validationData = param.folder + "/t10k-images-idx3-ubyte"
      val validationLabel = param.folder + "/t10k-labels-idx1-ubyte"

      val model = if (param.modelSnapshot.isDefined) {
        Module.load[Float](param.modelSnapshot.get)
      } else {
        if (param.graphModel) {
          LeNet5.graph(classNum = 10)
        } else {
          Engine.getEngineType() match {
            case MklBlas => LeNet5(10)
            case MklDnn => LeNet5.dnnGraph(param.batchSize / Engine.nodeNumber(), 10)
          }
        }
      }
      val criterion = Engine.getEngineType() match {
        case MklBlas => ClassNLLCriterion()
        case MklDnn => CrossEntropyCriterion()
      }

      val optimMethod = if (param.stateSnapshot.isDefined) {
        OptimMethod.load[Float](param.stateSnapshot.get)
      } else {
         new LarsSGD[Float](
           true,
           1,
           0.001,
           0.01,
           0.0005,
           0.5)
        //new LarsSGD[Float](true, _learningRate = 0.1)
      }

      val trainSet = DataSet.array(load(trainData, trainLabel), sc) ->
        BytesToGreyImg(28, 28) -> GreyImgNormalizer(trainMean, trainStd) -> GreyImgToBatch(
        param.batchSize)

      val optimizer = Optimizer(
        model = model,
        dataset = trainSet,
        criterion = criterion)
      if (param.checkpoint.isDefined) {
        optimizer.setCheckpoint(param.checkpoint.get, Trigger.everyEpoch)
      }
      if(param.overWriteCheckpoint) {
        optimizer.overWriteCheckpoint()
      }

      val validationSet = DataSet.array(load(validationData, validationLabel), sc) ->
        BytesToGreyImg(28, 28) -> GreyImgNormalizer(testMean, testStd) -> GreyImgToBatch(
        param.batchSize)

      optimizer
        .setValidation(
          trigger = Trigger.everyEpoch,
          dataset = validationSet,
          vMethods = Array(new Top1Accuracy, new Top5Accuracy[Float], new Loss[Float]))
        .setOptimMethod(optimMethod)
        .setEndWhen(Trigger.maxEpoch(param.maxEpoch))
        .optimize()

      sc.stop()
    })
  }
}

