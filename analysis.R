rm(list=ls())
options(warn = -1)

library(unbalanced)
library(caret)
library(e1071)
library(ggplot2)
library(pROC)
library(RSNNS)

data <- read.csv("/home/murilo/Documentos/rm/project/data/data.csv")

for (i in 1:ncol(data)){
  nas <- which(is.na(data[[i]]))
  if (length(nas)>0){data <- data[-nas,]}
}

# nas <- which(is.na(data$total_tf))
# if (length(nas)>0){data <- data[-nas,]}

# Data is heavily unbalanced. The great majority of terms is discarded.

discard_rate <- sum(data$get_out == 1)/nrow(data)
cat(sprintf("Discard rate: %.3f\n",discard_rate))

newData <- data[,1:3]
for (col in 4:6){
  factors <- unique(data[,col])
  for (fact in factors){
    newData <- cbind(newData,1*(data[,col] == fact))
    isformer = ""
    if (col==5){isformer="former_"}
    if (col==6){isformer="next_"}
    names(newData)[ncol(newData)] <- strcat(isformer,fact)
  }
}

newData <- cbind(newData,data$get_out)
names(newData)[ncol(newData)] <- "get_out"
data <- newData
data$get_out <- as.factor(data$get_out)

data$idf <- (data$idf - min(data$idf))/(max(data$idf) - min(data$idf))
data$mean_tf<- (data$mean_tf - min(data$mean_tf))/(max(data$mean_tf) - min(data$mean_tf))
data$query_place <- (data$query_place - min(data$query_place))/(max(data$query_place) - min(data$query_place))

# Choosing which variables aren't good enough to stay
# with Fisher's separability measure

seps <- c()
for (i in 1:(ncol(data)-1)){
  dC1 <- data[which(data$get_out == 1),i]
  dCm1 <- data[which(data$get_out == 0),i]
  square_means <- (mean(dC1) - mean(dCm1))^2
  var_sums <- var(dC1) + var(dCm1)
  sep <- square_means/var_sums
  seps <- c(seps,sep)
}

best <- names(data)[order(seps,decreasing = T)[1:10]]
best <- c(best,"get_out")
gtfo <- which(!(names(data) %in% best))
# q <- quantile(seps)
# gtfo <- which(seps<q[[4]])#order(seps,decreasing=T)[-(1:10)]
data <- data[,-gtfo]

Feat <- 1:(ncol(newData)-1)
Separation <- seps[order(seps)]
ggplot(data=data.frame(Feat,Separation),aes(x=Feat,y=Separation)) +
  geom_line() + geom_point(color='blue')
ggplot(data=data.frame(X1=rep(1,length(Separation)),Separation),aes(x=X1,y=Separation)) + 
  geom_boxplot()

###################################
# SMOTE did not solve the balancing problem!

# X <- data[,-ncol(data)]
# Y <- data$get_out

# p <- ubSMOTE(X,Y)
# data <- cbind(p[[1]],p[[2]])
# names(data)[ncol(data)] <- "get_out"

####################################

seqt <- seq(0,1,.05)
log.acc <- 0
log.roc <- matrix(0,nrow=length(seqt),ncol=2)
log.fprate <- 0
log.fnrate <- 0
log.auc <- 0

knn.acc <- 0
knn.fprate <- 0
knn.fnrate <- 0

svm.acc <- 0
svm.fprate <- 0

xgb.acc <- 0
xgb.fprate <- 0

mlp.acc <- 0
mlp.roc <- matrix(0,nrow=length(seqt),ncol=2)
mlp.fprate <- 0
mlp.fnrate <- 0
mlp.auc <- 0

nb.acc <- 0
nb.fprate <- 0
nb.fnrate <- 0

folds <- 10
set.seed(1)
ici <- sample(nrow(data))
smp <- split(ici,1:folds)
for (i in 1:folds){
  dataTest <- data[smp[[i]],]
  labTest <- dataTest$get_out
  dataTest <- dataTest[,-ncol(dataTest)]
  
  dataTrain <- data[-smp[[i]],]
  labTrain <- dataTrain$get_out
  dataTrain_noTarget <- dataTrain[,-ncol(dataTrain)]
  
  ####################################
  # Naive Bayes
  
  nb.model <- naiveBayes(get_out~.,data=dataTrain)
  nb.pred <- predict(nb.model,dataTest)
  fp_rate <- sum((nb.pred == 1) & (labTest == 0))/sum(labTest == 0)
  fn_rate <- sum((nb.pred == 0) & (labTest == 1))/sum(labTest == 1)
  acc <- sum(nb.pred == labTest)/length(nb.pred)
  
  nb.acc <- nb.acc + acc
  nb.fnrate <- nb.fnrate + fn_rate
  nb.fprate <- nb.fprate + fp_rate
  ####################################
  # KNN classification
  
  pred.knn <- knn(dataTrain_noTarget,dataTest,as.factor(labTrain),k=1)
  fp_rate <- sum((pred.knn == 1) & (labTest == 0))/sum(labTest == 0)
  fn_rate <- sum((pred.knn == 0) & (labTest == 1))/sum(labTest == 1)
  acc <- sum(pred.knn == labTest)/length(pred.knn)
  
  knn.acc <- knn.acc + acc
  knn.fnrate <- knn.fnrate + fn_rate
  knn.fprate <- knn.fprate + fp_rate
  
  ######################################################
  # Logistic regression
  dataTrain <- data[ici[-(1:500)],]
  log.model <- glm(get_out~.,data=dataTrain,family=binomial(link='logit'),
                   control=list(maxit=100))
  log.pred <- predict(log.model,dataTest,type='response')
  
  curve <- matrix(nrow=length(seqt),ncol=2)
  ci <- 0
  for (th in seqt){
    ci <- ci+1
    preds <- 1*(log.pred > th)
    tprate <- sum((preds == 1) & (labTest == 1))/sum(labTest==1)
    fprate <- sum((preds == 1) & (labTest == 0))/sum(labTest==0)
    curve[ci,] <- c(fprate,tprate)
  }
  
  auc <- 0
  for (i in 1:(nrow(curve)-1)){
    area <- (curve[i+1,2] * (curve[i,1] - curve[i+1,1])) + 
      ((curve[i,1] - curve[i+1,1]) * (curve[i,2] - curve[i+1,2]))/2
    auc <- auc+area
  }
  
  log.pred <- 1*(log.pred >= .8)
  acc <- sum(log.pred == labTest)/length(log.pred)
  fp_rate <- sum((log.pred == 1) & (labTest == 0))/sum(labTest == 0)
  fn_rate <- sum((log.pred == 0) & (labTest == 1))/sum(labTest == 1)
  
  log.acc <- log.acc + acc
  log.fprate <- log.fprate + fp_rate
  log.fnrate <- log.fnrate + fn_rate
  log.roc <- log.roc + curve
  log.auc <- log.auc + auc
  
  
  #######################################################
  # SVM brought complexity, but not accuracy improvement.
  # Also, it is more sensitive for the unbalanced data.
  
  # metodo <- "svmRadial"
  # 
  # ctrl <- caret::trainControl(method = "repeatedcv",
  #                             repeats = 1)#, classProbs = TRUE)
  # # summaryFunction = twoClassSummary)
  # 
  # tune <- caret::train(get_out ~.,data=dataTrain , method = metodo,
  #                      verbose=FALSE)#,trControl = ctrl, metric="ROC")
  # 
  # pred <- predict(tune, dataTest)
  # 
  # acc <- sum(as.numeric(pred) == as.numeric(labTest))/length(pred)
  # fprate <- sum((as.numeric(pred) == 2) & (as.numeric(labTest) == 1))/
  #   sum(as.numeric(pred) != as.numeric(labTest))
  # 
  # svm.acc <- svm.acc + acc
  # svm.fprate <- svm.fprate + fprate
  
  ###############################################################
  # XGBoost
  
  # metodo <- "xgbLinear"
  # 
  # ctrl <- caret::trainControl(method = "repeatedcv",
  #                             repeats = 1)
  # 
  # tune <- caret::train(get_out ~.,data=dataTrain , method = metodo,
  #                      verbose=FALSE)#,trControl = ctrl, metric="ROC")
  # 
  # pred <- predict(tune, dataTest)
  # 
  # acc <- sum(as.numeric(pred) == as.numeric(labTest))/length(pred)
  # fprate <- sum((as.numeric(pred) == 2) & (as.numeric(labTest) == 1))/
  #   sum(as.numeric(pred) != as.numeric(labTest))
  # 
  # xgb.acc <- xgb.acc + acc
  # xgb.fprate <- xgb.fprate + fprate
  
  ###################################################
  
  # mlp.model <- RSNNS::mlp(x = dataTrain_noTarget,y = as.numeric(labTrain)-1,size=c(40,10),
  #                  learnFunc = "BackpropWeightDecay")
  # mlp.pred <- predict(mlp.model,dataTest)
  # 
  # curve <- matrix(nrow=length(seqt),ncol=2)
  # ci <- 0
  # for (th in seqt){
  #   ci <- ci+1
  #   preds <- 1*(mlp.pred > th)
  #   tprate <- sum((preds == 1) & (labTest == 1))/sum(labTest==1)
  #   fprate <- sum((preds == 1) & (labTest == 0))/sum(labTest==0)
  #   curve[ci,] <- c(fprate,tprate)
  # }
  # 
  # auc <- 0
  # for (i in 1:(nrow(curve)-1)){
  #   area <- (curve[i+1,2] * (curve[i,1] - curve[i+1,1])) +
  #     ((curve[i,1] - curve[i+1,1]) * (curve[i,2] - curve[i+1,2]))/2
  #   auc <- auc+area
  # }
  # 
  # mlp.pred <- 1*(mlp.pred > .8)
  # acc <- sum(mlp.pred == labTest)/length(mlp.pred)
  # fp_rate <- sum((mlp.pred == 1) & (labTest == 0))/sum(labTest == 0)
  # fn_rate <- sum((mlp.pred == 0) & (labTest == 1))/sum(labTest == 1)
  # 
  # mlp.acc <- mlp.acc + acc
  # mlp.fprate <- mlp.fprate + fp_rate
  # mlp.fnrate <- mlp.fnrate + fn_rate
  # mlp.auc <- mlp.auc + auc
  # mlp.roc <- mlp.roc + curve
}

log.acc <- log.acc/folds
log.roc <- log.roc/folds
log.fprate <- log.fprate/folds
log.fnrate <- log.fnrate/folds
log.auc <- log.auc/folds

mlp.acc <- mlp.acc/folds
mlp.roc <- mlp.roc/folds
mlp.fprate <- mlp.fprate/folds
mlp.fnrate <- mlp.fnrate/folds
mlp.auc <- mlp.auc/folds

nb.acc <- nb.acc/folds
nb.fnrate <- nb.fnrate/folds
nb.fprate <- nb.fprate/folds

knn.acc <- knn.acc/folds
knn.fprate <- knn.fprate/folds
knn.fnrate <- knn.fnrate/folds

xgb.acc <- xgb.acc/folds
xgb.fprate <- xgb.fprate/folds

svm.acc <- svm.acc/folds
svm.fprate <- svm.fprate/folds

cat(sprintf("Naive Bayes accuracy: %.2f",nb.acc*100))
cat(sprintf("Naive Bayes false positive rate: %.2f",nb.fprate))
cat(sprintf("Naive Bayes false negative rate: %.2f",nb.fnrate))

cat(sprintf("KNN accuracy: %.2f",knn.acc*100))
cat(sprintf("KNN false positive rate: %.2f",knn.fprate))
cat(sprintf("KNN false negative rate: %.2f",knn.fnrate))

cat(sprintf("Logistic accuracy: %.2f",log.acc*100))
cat(sprintf("Logistic false positive rate: %.2f",log.fprate))
cat(sprintf("Logistic false negative rate: %.2f",log.fnrate))
cat(sprintf("Logistic area under ROC curve: %.3f",log.auc))

# cat(sprintf("MLP accuracy: %.2f",mlp.acc*100))
# cat(sprintf("MLP false positive rate: %.2f",mlp.fprate))
# cat(sprintf("MLP false negative rate: %.2f",mlp.fnrate))
# cat(sprintf("MLP area under ROC curve: %.3f",mlp.auc))

# cat(sprintf("XGBoost accuracy: %.2f\n",xgb.acc*100))
# cat(sprintf("XGBoost false positive rate: %.2f\n",xgb.fprate))

# cat(sprintf("SVM accuracy: %.2f",svm.acc*100))
# cat(sprintf("SVM false positive rate: %.2f",svm.fprate))

ggplot(data=as.data.frame(log.roc),aes(x=V1,y=V2)) + 
  geom_line() + geom_point(color='red') + 
  xlab("FP Rate") + ylab("TP Rate") #+
  # geom_line(data=as.data.frame(mlp.roc),aes(x=V1,y=V2),color="blue") + 
  # geom_point(data=as.data.frame(mlp.roc),aes(x=V1,y=V2),color="green")

# ggplot(data=as.data.frame(mlp.roc),aes(x=V1,y=V2)) +
#   geom_line() + geom_point(color='red') +
#   xlab("FP Rate") + ylab("TP Rate")

options(warn=0)
